import React, { useState, useEffect } from 'react';
import { subscribeToUserDoc, subscribeToTransactions } from './firebaseAdapter';

/**
 * WeeklySpendDashboard Component
 * Renders an active command center hud of weekly allocations, calculating live spending metrics 
 * directly from your synced Firestore instances. Includes high contrast safety indicators.
 *
 * @param {string} userId - Auth user identifier
 */
export default function WeeklySpendDashboard({ userId = "john_doe" }) {
  const [userData, setUserData] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');

    // Subscribe using the central adapter (detects and gracefully runs online or offline seamlessly)
    const unsubscribeUser = subscribeToUserDoc(
      userId,
      (data) => {
        setUserData(data);
        setLoading(false);
      },
      (err) => {
        console.error("User subscription failed:", err);
        setError("Failed to stream core account metrics.");
        setLoading(false);
      }
    );

    const unsubscribeTransactions = subscribeToTransactions(
      userId,
      4,
      (txs) => {
        setRecentTransactions(txs);
      },
      (err) => {
        console.error("Transactions subscription failed:", err);
      }
    );

    return () => {
      unsubscribeUser();
      unsubscribeTransactions();
    };
  }, [userId]);

  if (loading) {
    return (
      <div className="max-w-md mx-auto my-8 p-8 bg-white border border-purple-100 rounded-3xl shadow-xl flex flex-col items-center justify-center min-h-[300px]">
        <div className="w-8 h-8 border-4 border-purple-950 border-t-transparent rounded-full animate-spin"></div>
        <p className="mt-4 text-xs font-bold text-purple-950 uppercase tracking-widest animate-pulse">Syncing HUD Telemetry...</p>
      </div>
    );
  }

  // Calculate Spend levels safely
  const limitValue = userData?.weeklySpendingLimit || 1.0;
  const remainingValue = Math.max(0, userData?.remainingWeeklyAllowance || 0.0);
  const spentValue = Math.max(0, limitValue - remainingValue);
  const spentPercentage = Math.min(100, (spentValue / limitValue) * 100);
  const remainingPercentage = 100 - spentPercentage;

  /**
   * Evaluates if current outlays have breached the critical 80% budget cap.
   *
   * @param {number} spent - Total accumulated outlays
   * @param {number} limit - Maximum weekly safe allocation
   * @returns {boolean} True if budget utilization exceeds 80%
   */
  const checkBudgetThresholdExceeded = (spent, limit) => {
    if (!limit || limit <= 0) return false;
    return (spent / limit) > 0.80;
  };

  const exceedsEightyPercent = checkBudgetThresholdExceeded(spentValue, limitValue);

  // System status alerts based on boundaries
  const isBudgetCritical = remainingPercentage <= 15;
  const isBudgetWarning = remainingPercentage <= 35 && remainingPercentage > 15;

  return (
    <div className="max-w-md mx-auto my-8 p-6 bg-purple-950 text-white rounded-3xl shadow-2xl border border-purple-900 font-sans">
      
      {/* Header HUD info */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <span className="text-[10px] font-black tracking-widest text-purple-300 uppercase">Weekly Safe Spending</span>
          <h2 className="text-3xl font-black tracking-tight mt-1 flex items-baseline gap-1">
            <span className="text-sm font-semibold text-purple-300">ZMW</span>
            <span className="font-mono">{remainingValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
          </h2>
        </div>
        <div className="text-right">
          <span className="text-[9px] font-extrabold tracking-widest text-purple-400 uppercase">Emergency Fund Reserve</span>
          <p className="text-md font-mono font-bold text-emerald-400 mt-1">
            ZMW {userData?.emergencyFundBalance?.toLocaleString('en-US', { minimumFractionDigits: 2 }) || '0.00'}
          </p>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-900/50 border border-red-700 rounded-xl text-yellow-300 font-bold text-xs uppercase tracking-wide">
          ⚠️ {error}
        </div>
      )}

      {/* Progress Section */}
      <div className="bg-purple-900/40 border border-purple-800/60 rounded-2xl p-4 mb-6">
        <div className="flex justify-between items-baseline mb-2">
          <span className="text-[10px] font-bold uppercase tracking-wider text-purple-300">Spent This Week</span>
          <span className="text-xs font-mono font-black tracking-tight text-white">
            {spentPercentage.toFixed(0)}% Utilized
          </span>
        </div>

        {/* Custom Retro Dash Progress Bar Container */}
        <div className="w-full bg-purple-950/70 border border-purple-700/50 h-5 p-1 rounded-full overflow-hidden flex items-center">
          <div 
            className={`h-full rounded-full transition-all duration-700 ease-out flex items-center justify-end pr-2 min-w-[20px] ${
              isBudgetCritical 
                ? 'bg-red-500 animate-pulse' 
                : isBudgetWarning 
                  ? 'bg-yellow-400' 
                  : 'bg-emerald-400'
            }`}
            style={{ width: `${spentPercentage}%` }}
          >
            {spentPercentage > 15 && (
              <span className="text-[8px] font-black text-purple-950 font-mono">■</span>
            )}
          </div>
        </div>

        {/* Visual Legend Metrics */}
        <div className="flex justify-between mt-3 text-[10px] font-mono font-semibold text-purple-300">
          <div>
            <span>Target Cap: </span>
            <span className="text-white">ZMW {limitValue.toFixed(2)}</span>
          </div>
          <div className="text-right">
            <span>Disbursed: </span>
            <span className="text-white">ZMW {spentValue.toFixed(2)}</span>
          </div>
        </div>
      </div>

      {/* Conditional Warning Pills styled as active system alerts */}
      {exceedsEightyPercent && (
        <div className="mb-6 p-4 bg-amber-500/25 border-2 border-amber-500 rounded-2xl flex flex-col gap-1.5 shadow-md relative overflow-hidden">
          <div className="flex items-center gap-2">
            <span className="text-base">⚠️</span>
            <span className="text-xs font-black uppercase text-amber-300 tracking-wider">
              BUDGET BREACH WARNING (80%+)
            </span>
          </div>
          <p className="text-[11px] font-medium text-amber-100/90 leading-relaxed">
            You have utilized <span className="font-mono font-black text-white bg-amber-600/40 px-1.5 py-0.5 rounded">{spentPercentage.toFixed(1)}%</span> of your total weekly ZMW {limitValue.toFixed(2)} budget cap. Secure your spending and postpone non-essential transactions.
          </p>
        </div>
      )}

      {isBudgetCritical && (
        <div className="mb-6 p-3 bg-red-500/20 border border-red-500 rounded-xl flex items-center gap-2">
          <div className="w-2.5 h-2.5 bg-red-500 rounded-full animate-ping"></div>
          <p className="text-[11px] font-black uppercase text-red-100 tracking-wider">
            CRITICAL OUTLAY LIMIT: SYSTEM WILL SQUEEZE LOCKS
          </p>
        </div>
      )}

      {isBudgetWarning && !isBudgetCritical && (
        <div className="mb-6 p-3 bg-yellow-500/20 border border-yellow-500 rounded-xl flex items-center gap-2">
          <div className="w-2.5 h-2.5 bg-yellow-500 rounded-full animate-pulse"></div>
          <p className="text-[11px] font-black uppercase text-yellow-100 tracking-wider">
            WARNING: Spend approaches allocation limit
          </p>
        </div>
      )}

      {/* History Feed Telemetry */}
      <div>
        <div className="flex justify-between items-center mb-3">
          <span className="text-[10px] font-black tracking-widest text-purple-300 uppercase">Recent System Command Syncs</span>
          <span className="text-[8px] font-bold text-purple-400 uppercase font-mono">Live Firestore Feed</span>
        </div>

        {recentTransactions.length === 0 ? (
          <div className="p-4 bg-purple-900/10 border border-dashed border-purple-800 rounded-xl text-center text-xs text-purple-400/80 font-medium">
            No logged outlays found for this cycles context.
          </div>
        ) : (
          <div className="space-y-2">
            {recentTransactions.map((tx) => (
              <div 
                key={tx.id} 
                className="flex justify-between items-center bg-purple-900/30 hover:bg-purple-900/40 border border-purple-900/60 p-3 rounded-xl transition-all duration-200"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-white uppercase">{tx.category || 'Other'}</span>
                    {tx.isEmergency && (
                      <span className="text-[8px] bg-red-500 text-white font-black px-1.5 py-0.5 rounded uppercase tracking-tighter">
                        Emergency
                      </span>
                    )}
                  </div>
                  <span className="text-[10px] text-purple-300 uppercase font-medium block truncate max-w-[200px] mt-0.5">
                    {tx.description || `Debit from Channel: ${tx.walletId}`}
                  </span>
                </div>
                <div className="text-right">
                  <span className={`text-xs font-black font-mono ${tx.type === 'CREDIT' ? 'text-emerald-400' : 'text-red-400'}`}>
                    {tx.type === 'CREDIT' ? '+' : '-'} ZMW {parseFloat(tx.amount || 0).toFixed(2)}
                  </span>
                  <span className="text-[8px] text-purple-400 font-mono uppercase block mt-0.5">
                    {tx.walletId}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
