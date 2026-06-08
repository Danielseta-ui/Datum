import { initializeApp } from 'firebase/app';
import { 
  getFirestore, 
  collection, 
  doc, 
  runTransaction, 
  onSnapshot,
  query,
  orderBy,
  limit as firestoreLimit
} from 'firebase/firestore';

// Retrieve credentials securely from env
const firebaseConfig = {
  apiKey: process.env.REACT_APP_FIREBASE_API_KEY,
  authDomain: process.env.REACT_APP_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.REACT_APP_FIREBASE_PROJECT_ID,
  storageBucket: process.env.REACT_APP_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.REACT_APP_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.REACT_APP_FIREBASE_APP_ID
};

// Check if credentials are set and are not placeholder values
const isFirebaseConfigured = (() => {
  const keys = Object.values(firebaseConfig);
  if (keys.some(v => !v || v.includes('MY_') || v.includes('PLACEHOLDER') || v === '')) {
    return false;
  }
  return true;
})();

let db = null;
let firebaseActive = false;

if (isFirebaseConfigured) {
  try {
    const app = initializeApp(firebaseConfig);
    db = getFirestore(app);
    firebaseActive = true;
    console.log("⚡ [FirebaseAdapter] Online mode initialized successfully with remote Firestore.");
  } catch (error) {
    console.warn("⚠️ [FirebaseAdapter] Failed to initialize Firebase SDK online client. Cascading to local sandbox:", error);
    firebaseActive = false;
  }
} else {
  console.log("🧱 [FirebaseAdapter] Configuration absent. Activated local-first LocalStorage sandbox client.");
  firebaseActive = false;
}

// ========================================================
// LOCALSTORAGE SANDBOX IMPLEMENTATION
// ========================================================
const listeners = {
  user: {},
  transactions: {}
};

// Seed initial fallback local storage values if empty
const seedLocalSandbox = (userId) => {
  const userKey = `datum_user_${userId}`;
  const walletsKey = `datum_wallets_${userId}`;
  const txKey = `datum_txs_${userId}`;

  if (!localStorage.getItem(userKey)) {
    localStorage.setItem(userKey, JSON.stringify({
      weeklySpendingLimit: 2500.0,
      remainingWeeklyAllowance: 2100.0,
      emergencyFundBalance: 12000.0,
      email: "danielseta37@gmail.com",
      displayName: "Daniel"
    }));
  }

  if (!localStorage.getItem(walletsKey)) {
    localStorage.setItem(walletsKey, JSON.stringify({
      CASH: { balance: 2100.0, displayName: 'Cash Fund' },
      FNB: { balance: 12000.0, displayName: 'FNB Vault' },
      AIRTEL: { balance: 500.0, displayName: 'Airtel Money' },
      GLOBAL_CARD: { balance: 100.0, displayName: 'Global Debit Card' }
    }));
  }

  if (!localStorage.getItem(txKey)) {
    localStorage.setItem(txKey, JSON.stringify([
      {
        transactionId: "seed-tx-1",
        walletId: "CASH",
        amount: 300.0,
        type: "DEBIT",
        category: "Food",
        description: "Initial grocery stock-up",
        timestamp: Date.now() - 3600000,
        isEmergency: false
      },
      {
        transactionId: "seed-tx-2",
        walletId: "GLOBAL_CARD",
        amount: 99.0,
        type: "DEBIT",
        category: "Spotify",
        description: "Monthly streaming subscripton",
        timestamp: Date.now() - 7200000,
        isEmergency: false
      }
    ]));
  }
};

const notifyUserListeners = (userId) => {
  const data = JSON.parse(localStorage.getItem(`datum_user_${userId}`));
  if (listeners.user[userId]) {
    listeners.user[userId].forEach(cb => cb(data));
  }
};

const notifyTxListeners = (userId) => {
  const txs = JSON.parse(localStorage.getItem(`datum_txs_${userId}`)) || [];
  if (listeners.transactions[userId]) {
    listeners.transactions[userId].forEach(({ cb, max }) => {
      // Return sorted descending limit
      const sorted = [...txs].sort((a, b) => b.timestamp - a.timestamp).slice(0, max);
      cb(sorted);
    });
  }
};

// ========================================================
// EXPORTED DATA MANAGEMENT OPERATIONS
// ========================================================

/**
 * Subscribes to the live user state.
 */
export function subscribeToUserDoc(userId, onUpdate, onError) {
  if (firebaseActive && db) {
    const userDocRef = doc(db, 'users', userId);
    return onSnapshot(userDocRef, (snap) => {
      if (snap.exists()) {
        onUpdate(snap.data());
      } else {
        onUpdate({
          weeklySpendingLimit: 2500.0,
          remainingWeeklyAllowance: 2100.0,
          emergencyFundBalance: 12000.0
        });
      }
    }, onError);
  } else {
    // Offline / Local sandbox
    seedLocalSandbox(userId);
    if (!listeners.user[userId]) {
      listeners.user[userId] = [];
    }
    listeners.user[userId].push(onUpdate);
    
    // Immediate callback trigger
    const currentUser = JSON.parse(localStorage.getItem(`datum_user_${userId}`));
    onUpdate(currentUser);

    return () => {
      listeners.user[userId] = listeners.user[userId].filter(cb => cb !== onUpdate);
    };
  }
}

/**
 * Subscribes to the live transactions feed.
 */
export function subscribeToTransactions(userId, limitCount, onUpdate, onError) {
  if (firebaseActive && db) {
    const txColRef = collection(db, 'users', userId, 'transactions');
    const q = query(txColRef, orderBy('timestamp', 'desc'), firestoreLimit(limitCount));
    return onSnapshot(q, (snap) => {
      const list = [];
      snap.forEach((doc) => {
        list.push({ id: doc.id, ...doc.data() });
      });
      onUpdate(list);
    }, onError);
  } else {
    // Offline / Local sandbox
    seedLocalSandbox(userId);
    if (!listeners.transactions[userId]) {
      listeners.transactions[userId] = [];
    }
    listeners.transactions[userId].push({ cb: onUpdate, max: limitCount });

    // Immediate callback trigger
    const allTxs = JSON.parse(localStorage.getItem(`datum_txs_${userId}`)) || [];
    const sorted = [...allTxs].sort((a, b) => b.timestamp - a.timestamp).slice(0, limitCount);
    onUpdate(sorted);

    return () => {
      listeners.transactions[userId] = listeners.transactions[userId].filter(item => item.cb !== onUpdate);
    };
  }
}

/**
 * Commits a transaction containing balance updates and limit alterations atomic scope
 */
export async function commitTransaction(userId, txData) {
  const {
    walletId,
    amount,
    type,
    category,
    description,
    isEmergency,
    emergencyReason
  } = txData;

  if (firebaseActive && db) {
    await runTransaction(db, async (transaction) => {
      const userRef = doc(db, 'users', userId);
      const walletRef = doc(db, 'users', userId, 'wallets', walletId);
      const transactionColRef = collection(db, 'users', userId, 'transactions');
      const newTxRef = doc(transactionColRef);

      const userSnap = await transaction.get(userRef);
      const walletSnap = await transaction.get(walletRef);

      if (!walletSnap.exists()) {
        throw new Error(`Wallet ${walletId} does not exist in online Firestore database connection!`);
      }

      const currentWalletBalance = walletSnap.data().balance || 0.0;
      const currentRemainingAllowance = userSnap.exists() ? (userSnap.data().remainingWeeklyAllowance || 0.0) : 0.0;
      const currentEmergencyBalance = userSnap.exists() ? (userSnap.data().emergencyFundBalance || 0.0) : 0.0;

      let nextWalletBalance = currentWalletBalance;
      if (type === 'DEBIT') {
        nextWalletBalance -= amount;
      } else {
        nextWalletBalance += amount;
      }

      if (nextWalletBalance < 0 && !isEmergency) {
        throw new Error('Overdraft denied. Toggle emergency override to access backup funds.');
      }

      const transactionPayload = {
        transactionId: newTxRef.id,
        walletId,
        amount,
        type,
        category,
        description,
        timestamp: Date.now(),
        isEmergency,
        emergencyReason: isEmergency ? emergencyReason : null
      };

      const rootUpdates = {};
      if (walletId === 'CASH' && type === 'DEBIT') {
        rootUpdates.remainingWeeklyAllowance = Math.max(0, currentRemainingAllowance - amount);
      } else if (walletId === 'FNB') {
        rootUpdates.emergencyFundBalance = type === 'DEBIT' 
          ? currentEmergencyBalance - amount 
          : currentEmergencyBalance + amount;
      }

      transaction.set(newTxRef, transactionPayload);
      transaction.update(walletRef, { 
        balance: nextWalletBalance,
        lastTouchedTimestamp: Date.now()
      });

      if (Object.keys(rootUpdates).length > 0) {
        transaction.update(userRef, rootUpdates);
      }
    });
  } else {
    // LocalStorage atomic simulation
    seedLocalSandbox(userId);
    const userKey = `datum_user_${userId}`;
    const walletsKey = `datum_wallets_${userId}`;
    const txKey = `datum_txs_${userId}`;

    const user = JSON.parse(localStorage.getItem(userKey));
    const wallets = JSON.parse(localStorage.getItem(walletsKey));
    const txs = JSON.parse(localStorage.getItem(txKey)) || [];

    const wallet = wallets[walletId];
    if (!wallet) {
      throw new Error(`Wallet ${walletId} does not exist in local sandbox database!`);
    }

    const currentWalletBalance = wallet.balance || 0.0;
    const currentRemainingAllowance = user.remainingWeeklyAllowance || 0.0;
    const currentEmergencyBalance = user.emergencyFundBalance || 0.0;

    let nextWalletBalance = currentWalletBalance;
    if (type === 'DEBIT') {
      nextWalletBalance -= amount;
    } else {
      nextWalletBalance += amount;
    }

    if (nextWalletBalance < 0 && !isEmergency) {
      throw new Error('Overdraft denied. Toggle emergency override to unlock funds.');
    }

    // Append transaction
    const newTx = {
      id: `tx-local-${Date.now()}`,
      transactionId: `tx-local-${Date.now()}`,
      walletId,
      amount,
      type,
      category,
      description,
      timestamp: Date.now(),
      isEmergency,
      emergencyReason: isEmergency ? emergencyReason : null
    };

    txs.push(newTx);
    localStorage.setItem(txKey, JSON.stringify(txs));

    // Update wallet
    wallets[walletId].balance = nextWalletBalance;
    localStorage.setItem(walletsKey, JSON.stringify(wallets));

    // Handle root allowance dynamics
    if (walletId === 'CASH' && type === 'DEBIT') {
      user.remainingWeeklyAllowance = Math.max(0, currentRemainingAllowance - amount);
    } else if (walletId === 'FNB') {
      user.emergencyFundBalance = type === 'DEBIT' 
        ? currentEmergencyBalance - amount 
        : currentEmergencyBalance + amount;
    }

    localStorage.setItem(userKey, JSON.stringify(user));

    // Post alerts to active listening components
    notifyUserListeners(userId);
    notifyTxListeners(userId);
  }
}
