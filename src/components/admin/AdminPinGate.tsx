import React, { useState } from 'react';
import { Lock, ArrowLeft, Eye, EyeOff, ShieldCheck } from 'lucide-react';

interface AdminPinGateProps {
  expectedPin: string;
  onUnlock: () => void;
}

export const AdminPinGate: React.FC<AdminPinGateProps> = ({ expectedPin, onUnlock }) => {
  const [pin, setPin] = useState('');
  const [showPin, setShowPin] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (pin.trim() === expectedPin) {
      setError('');
      onUnlock();
    } else {
      setError('Invalid Admin PIN. Enter the 6-digit Sandhaur store PIN.');
    }
  };

  return (
    <div className="min-h-screen bg-[#FAF8F3] text-[#14221B] flex flex-col justify-between p-4 sm:p-6">
      <div className="max-w-6xl w-full mx-auto flex items-center justify-between">
        <a
          href="/"
          className="inline-flex items-center gap-2 text-sm font-semibold text-[#0B3B24] hover:underline"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Nav Grocery Store</span>
        </a>
        <span className="text-xs font-mono-num text-[#526058]">
          www.navgrocerystore.com/admin
        </span>
      </div>

      <main className="w-full max-w-md mx-auto my-10 bg-white rounded-2xl border border-[#0B3B24]/15 shadow-sm overflow-hidden">
        <div className="bg-[#0B3B24] px-6 py-6 text-white">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-[#D4AF37] text-[#0B3B24] flex items-center justify-center shrink-0">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h1 className="font-display text-xl font-bold text-white leading-tight">
                Unified Admin Control Center
              </h1>
              <p className="text-xs text-white/80 mt-0.5">
                Nav Grocery Store · NavKart · Android Apps Sync
              </p>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <div className="space-y-1.5">
            <label
              htmlFor="admin-pin-input"
              className="block text-xs font-bold uppercase tracking-wider text-[#526058]"
            >
              Enter 6-Digit Admin PIN
            </label>
            <div className="relative">
              <input
                id="admin-pin-input"
                type={showPin ? 'text' : 'password'}
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={12}
                autoFocus
                value={pin}
                onChange={(e) => {
                  setPin(e.target.value);
                  if (error) setError('');
                }}
                placeholder="••••••"
                className="w-full px-4 py-3 pr-11 rounded-xl border border-[#0B3B24]/25 bg-[#FAF8F3] text-lg font-mono-num tracking-[0.25em] text-[#14221B] focus:outline-none focus:border-[#0B3B24]"
              />
              <button
                type="button"
                onClick={() => setShowPin((v) => !v)}
                aria-label={showPin ? 'Hide PIN' : 'Show PIN'}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 text-[#526058] hover:text-[#0B3B24] cursor-pointer"
              >
                {showPin ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {error && (
              <p className="text-xs font-semibold text-red-700 pt-1" role="alert">
                {error}
              </p>
            )}
          </div>

          <button
            type="submit"
            className="w-full py-3 px-4 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold text-sm transition-colors cursor-pointer"
          >
            Unlock Admin Control Center
          </button>

          <div className="pt-3 border-t border-[#0B3B24]/10 flex items-center gap-2 text-xs text-[#526058]">
            <ShieldCheck className="w-4 h-4 text-[#0B3B24] shrink-0" />
            <span>
              Direct Firebase RTDB Sync · Main Bazaar, Sandhaur 148020
            </span>
          </div>
        </form>
      </main>

      <footer className="text-center text-xs text-[#526058]">
        Nav Grocery Store &amp; NavKart Unified Inventory Engine
      </footer>
    </div>
  );
};
