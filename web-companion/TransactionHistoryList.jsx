import React, { useState, useEffect } from 'react';
import { subscribeToTransactions } from './firebaseAdapter';

/**
 * TransactionHistoryList Component
 * Connects directly to the user's Firestore transactions sub-collection, streaming the 10 most recent 
 * system spend records in real-time. Displays precise date, time, category, and amount indicators.
 *
 * @param {string} userId - Auth user identifier
 */
export default function TransactionHistoryList({ userId = "john_doe" }) {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');

    // Stream live transactions via adapter (online Firestore or sandbox local fallback)
    const unsubscribe = subscribeToTransactions(
      userId,
      10,
      (list) => {
        setTransactions(list);
        setLoading(false);
      },
      (err) => {
        console.error("Historical feed streaming failed:", err);
        setError("Unable to sync telemetry logs from Firestore.");
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [userId]);

  // Helper to format timestamps gracefully
  const formatTxDate = (timestampValue) => {
    if (!timestampValue) return 'Unknown Date';
    // Handle both Long millisecond epoch values OR absolute Firestore Timestamps
    const date = typeof timestampValue === 'number' 
      ? new Date(timestampValue) 
      : timestampValue.toDate ? timestampValue.toDate() : new Date(timestampValue);
    
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }) + ' @ ' + date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  const getCategoryTheme = (category) => {
    switch (category?.toLowerCase()) {
      case 'food':
        return 'bg-amber-100 text-amber-950 border-amber-300';
      case 'transport':
        return 'bg-blue-100 text-blue-950 border-blue-300';
      case 'spotify':
        return 'bg-emerald-100 text-emerald-950 border-emerald-300';
      case 'savings':
      case 'emergency':
        return 'bg-red-100 text-red-950 border-red-300';
      default:
        return 'bg-purple-100 text-purple-950 border-purple-300';
    }
  };

  if (loading) {
    return (
      <div className="max-w-md mx-auto my-8 p-8 bg-white border border-purple-100 rounded-3xl shadow-xl flex flex-col items-center justify-center min-h-[300px]">
        <div className="w-8 h-8 border-4 border-purple-950 border-t-transparent rounded-full animate-spin"></div>
        <p className="mt-4 text-xs font-bold text-purple-950 uppercase tracking-widest animate-pulse">Streaming Outlay History...</p>
      </div>
    );
  }

  return (
    <div className="max-w-md mx-auto my-8 p-6 bg-white rounded-3xl border border-purple-200 shadow-xl font-sans text-gray-900">
      
      {/* Title Header */}
      <div className="flex justify-between items-center mb-6 border-b border-purple-100 pb-4">
        <div>
          <h2 className="text-2xl font-black tracking-tight text-purple-900 uppercase">
            Ledger Audit
          </h2>
          <p className="text-[10px] text-gray-500 font-bold uppercase tracking-wider mt-0.5">
            Real-time Telemetry (Last 10 Synced)
          </p>
        </div>
        <div className="px-3 py-1 bg-purple-50 rounded-full border border-purple-100 flex items-center gap-1.5 animate-pulse">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
          <span className="text-[9px] font-black text-purple-950 uppercase tracking-widest">Live</span>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 rounded-xl bg-red-50 border border-red-200 text-red-950 font-bold text-xs uppercase tracking-wider">
          ⚠️ ERROR: {error}
        </div>
      )}

      {/* Transaction List */}
      {transactions.length === 0 ? (
        <div className="py-12 px-4 bg-purple-50 rounded-2xl text-center border-2 border-dashed border-purple-100">
          <p className="text-sm font-bold text-purple-950 uppercase tracking-tight">Zero logged outlaws found</p>
          <p className="text-xs text-slate-500 mt-1">Begin adding spends to activate your ledger sync feed.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {transactions.map((tx) => {
            const isDebit = tx.type === 'DEBIT';
            return (
              <div 
                key={tx.id}
                className="flex items-center justify-between p-4 bg-purple-50/50 hover:bg-purple-50 rounded-2xl border border-purple-100/80 transition-all duration-150"
              >
                {/* Left section: Category Indicator & Description */}
                <div className="flex items-center gap-3 min-w-0">
                  <div className={`px-2 py-1 border text-[9px] font-black uppercase rounded-lg tracking-wide ${getCategoryTheme(tx.category)}`}>
                    {tx.category || 'Other'}
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-black text-purple-950 uppercase tracking-tight truncate max-w-[180px]">
                      {tx.description || `Spend on Channel: ${tx.walletId}`}
                    </p>
                    <span className="text-[10px] text-slate-500 block font-semibold mt-0.5 font-mono">
                      {formatTxDate(tx.timestamp)}
                    </span>
                  </div>
                </div>

                {/* Right section: Numeric Values & Channel Indicator */}
                <div className="text-right flex-shrink-0 pl-2">
                  <p className={`text-sm font-black font-mono tracking-tight ${isDebit ? 'text-red-600' : 'text-emerald-700'}`}>
                    {isDebit ? '-' : '+'} ZMW {parseFloat(tx.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </p>
                  
                  <div className="flex items-center justify-end gap-1 mt-0.5">
                    {tx.isEmergency && (
                      <span className="text-[7px] bg-red-600 text-white font-black px-1.5 py-0.5 rounded uppercase tracking-tighter shadow-sm animate-pulse">
                        EMERGNCY
                      </span>
                    )}
                    <span className="text-[9px] text-purple-950 font-black tracking-wider uppercase font-mono px-1.5 py-0.5 bg-purple-100/50 rounded border border-purple-200">
                      {tx.walletId}
                    </span>
                  </div>
                </div>

              </div>
            );
          })}
        </div>
      )}

      {/* Decorative summary metadata footer */}
      <div className="mt-6 pt-4 border-t border-purple-100 text-center">
        <p className="text-[8px] font-bold text-gray-400 uppercase tracking-widest leading-none">
          Strictly Authenticated under Central Ledger Protocol
        </p>
      </div>

    </div>
  );
}
