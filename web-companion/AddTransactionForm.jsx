import React, { useState } from 'react';
import { commitTransaction } from './firebaseAdapter';

/**
 * AddTransactionForm Component
 * Fully integrated custom React Form for saving transactions to the 4-wallet Firestore schema,
 * performing atomic transaction writes to coordinate wallet balances and dynamic weekly HUD limits.
 *
 * @param {string} userId - Auth user identifier
 */
export default function AddTransactionForm({ userId = "john_doe" }) {
  const [walletId, setWalletId] = useState('CASH'); // DEFAULT: Daily Spend cash wallet
  const [amount, setAmount] = useState('');
  const [type, setType] = useState('DEBIT'); // DEBIT or CREDIT
  const [category, setCategory] = useState('Food');
  const [description, setDescription] = useState('');
  
  // Emergency specific variables
  const [isEmergency, setIsEmergency] = useState(false);
  const [emergencyReason, setEmergencyReason] = useState('');

  // Status & Error indicators
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const walletOptions = [
    { id: 'CASH', name: 'Cash Fund (Daily Spend / ZMW 70 Daily)' },
    { id: 'FNB', name: 'FNB Account (Secured Savings Vault)' },
    { id: 'AIRTEL', name: 'Airtel Money (Emergency Buffer)' },
    { id: 'GLOBAL_CARD', name: 'Global Debit Card (Spotify / Subs)' }
  ];

  const categories = ['Food', 'Transport', 'Spotify', 'Savings', 'Emergency', 'Other'];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!amount || isNaN(amount) || parseFloat(amount) <= 0) {
      setError('Please input a valid positive amount.');
      return;
    }

    if (isEmergency && !emergencyReason.trim()) {
      setError('An emergency rationale is required to authorize emergency over-limits.');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess(false);

    try {
      const parsedAmount = parseFloat(amount);

      // Perform transaction via Centralized Adapter (Atomic Firestore online sync or LocalStorage fallback)
      await commitTransaction(userId, {
        walletId,
        amount: parsedAmount,
        type,
        category,
        description,
        isEmergency,
        emergencyReason
      });

      setSuccess(true);
      setAmount('');
      setDescription('');
      setEmergencyReason('');
      setIsEmergency(false);
    } catch (err) {
      console.error(err);
      setError(err.message || 'Transaction execution failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto my-8 p-6 bg-white rounded-3xl border border-purple-200 shadow-xl font-sans text-gray-900">
      <div className="mb-6">
        <h2 className="text-2xl font-black tracking-tight text-purple-900 uppercase">
          New System Spend
        </h2>
        <p className="text-xs text-gray-500 font-medium tracking-wide uppercase mt-1">
          Synchronizing Real-time with central Firestore
        </p>
      </div>

      {success && (
        <div className="mb-4 p-4 rounded-xl bg-purple-50 border border-purple-200 text-purple-950 font-bold text-xs uppercase tracking-wider flex items-center">
          ✨ Transaction atomically committed and synchronized!
        </div>
      )}

      {error && (
        <div className="mb-4 p-4 rounded-xl bg-red-50 border border-red-200 text-red-950 font-bold text-xs uppercase tracking-wider">
          ⚠️ ERROR: {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Wallet Selection */}
        <div>
          <label className="block text-xs font-bold uppercase text-purple-950 mb-1">
            Debit Wallet Channel
          </label>
          <select 
            value={walletId}
            onChange={(e) => setWalletId(e.target.value)}
            disabled={loading}
            className="w-full p-3 bg-purple-50 border border-purple-200 rounded-xl font-bold text-sm text-purple-950 focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            {walletOptions.map(opt => (
              <option key={opt.id} value={opt.id}>{opt.name}</option>
            ))}
          </select>
        </div>

        {/* Amount and Type Row */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-bold uppercase text-purple-950 mb-1">
              Amount (ZMW)
            </label>
            <input 
              type="number"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              disabled={loading}
              placeholder="0.00"
              className="w-full p-3 bg-purple-100 border-none rounded-xl text-lg font-black text-purple-950 focus:outline-none focus:ring-2 focus:ring-purple-500 placeholder-purple-300"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-purple-950 mb-1">
              Command Type
            </label>
            <div className="flex bg-purple-50 p-1 rounded-xl border border-purple-100">
              <button
                type="button"
                onClick={() => setType('DEBIT')}
                className={`flex-1 py-2 rounded-lg text-xs font-bold uppercase transition ${type === 'DEBIT' ? 'bg-purple-950 text-white shadow' : 'text-purple-900'}`}
              >
                Debit
              </button>
              <button
                type="button"
                onClick={() => setType('CREDIT')}
                className={`flex-1 py-2 rounded-lg text-xs font-bold uppercase transition ${type === 'CREDIT' ? 'bg-purple-900 text-white shadow' : 'text-purple-900'}`}
              >
                Credit
              </button>
            </div>
          </div>
        </div>

        {/* Category selector */}
        <div>
          <label className="block text-xs font-bold uppercase text-purple-950 mb-1">
            Aesthetic Category
          </label>
          <select 
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            disabled={loading}
            className="w-full p-3 bg-purple-50 border border-purple-200 rounded-xl font-medium text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            {categories.map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>

        {/* Narrative Description */}
        <div>
          <label className="block text-xs font-bold uppercase text-purple-950 mb-1">
            Transaction Narrative
          </label>
          <input 
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={loading}
            placeholder="e.g. Weekly transport allowance setup"
            className="w-full p-3 bg-purple-50 border border-purple-200 rounded-xl text-sm focus:outline-none"
          />
        </div>

        {/* Emergency Overrides (only available for cash debits) */}
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl">
          <label className="flex items-center cursor-pointer">
            <input 
              type="checkbox"
              checked={isEmergency}
              onChange={(e) => setIsEmergency(e.target.checked)}
              disabled={loading}
              className="mr-3 accent-red-600 w-4 h-4"
            />
            <span className="text-xs font-black text-red-950 uppercase tracking-tight">
              Emergency Override Permission
            </span>
          </label>

          {isEmergency && (
            <div className="mt-3">
              <label className="block text-[10px] font-bold uppercase text-red-900 mb-1">
                Emergency Justification *
              </label>
              <textarea 
                rows="2"
                value={emergencyReason}
                onChange={(e) => setEmergencyReason(e.target.value)}
                disabled={loading}
                placeholder="Declare why system budget limits must be bypassed..."
                className="w-full p-2 bg-white border border-red-200 rounded-xl text-xs text-red-950 focus:outline-none focus:ring-1 focus:ring-red-500"
              />
            </div>
          )}
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={loading}
          className="w-full mt-4 py-4 px-6 bg-purple-950 hover:bg-purple-900 focus:ring-4 focus:ring-purple-300 rounded-2xl text-white font-bold text-xs uppercase tracking-widest transition flex justify-center items-center gap-2 shadow-lg disabled:opacity-50"
        >
          {loading ? (
            <span className="animate-pulse">Authorizing Atomic Sync...</span>
          ) : (
            'Commit Transaction'
          )}
        </button>
      </form>
    </div>
  );
}
