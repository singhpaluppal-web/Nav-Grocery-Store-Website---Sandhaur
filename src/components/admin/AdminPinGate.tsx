import React, { useEffect, useState } from 'react';
import type { User as FirebaseUser } from 'firebase/auth';
import { Lock, ArrowLeft, Eye, EyeOff, ShieldCheck, LogOut, CheckCircle2 } from 'lucide-react';
import {
  isFirebaseAuthConfigured,
  signInWithGoogle,
  signOutFromGoogle,
  subscribeToAuthState,
} from '../../firebase';
import { GoogleSignInButton } from '../GoogleSignInButton';

interface AdminPinGateProps {
  expectedPin: string;
  onUnlock: (operatorEmail?: string) => void;
}

export const AdminPinGate: React.FC<AdminPinGateProps> = ({ expectedPin, onUnlock }) => {
  const [pin, setPin] = useState('');
  const [showPin, setShowPin] = useState(false);
  const [error, setError] = useState('');
  const [authUser, setAuthUser] = useState<FirebaseUser | null>(null);
  const [authLoading, setAuthLoading] = useState(false);

  useEffect(() => {
    const unsubscribe = subscribeToAuthState((user) => {
      setAuthUser(user);
    });
    return unsubscribe;
  }, []);

  const handleGoogleAdminAuth = async () => {
    setError('');
    setAuthLoading(true);
    const res = await signInWithGoogle();
    setAuthLoading(false);
    if (res.error) {
      setError(res.error);
    } else if (res.user) {
      setAuthUser(res.user);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isFirebaseAuthConfigured && !authUser) {
      setError('Please sign in with your authorized Google Admin account first.');
      return;
    }
    if (pin.trim() === expectedPin) {
      setError('');
      onUnlock(authUser?.email || undefined);
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
                Authenticated Admin Login
              </h1>
              <p className="text-xs text-white/80 mt-0.5">
                Nav Grocery Store · Nav AI Media Studio · Navkart Android Sync
              </p>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {isFirebaseAuthConfigured && (
            <div className="space-y-2">
              <span className="block text-xs font-bold uppercase tracking-wider text-[#526058]">
                Step 1 · Verify Admin Account
              </span>
              {authUser ? (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5 min-w-0">
                    {authUser.photoURL ? (
                      <img
                        src={authUser.photoURL}
                        alt={authUser.displayName || 'Admin'}
                        referrerPolicy="no-referrer"
                        className="w-8 h-8 rounded-full border border-[#0B3B24]/30 object-cover shrink-0"
                      />
                    ) : (
                      <CheckCircle2 className="w-6 h-6 text-emerald-700 shrink-0" />
                    )}
                    <div className="min-w-0">
                      <p className="text-xs font-bold text-[#0B3B24] truncate">
                        {authUser.displayName || 'Authenticated Admin'}
                      </p>
                      <p className="text-[11px] text-[#526058] truncate">{authUser.email}</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => void signOutFromGoogle()}
                    className="px-2.5 py-1 rounded-lg bg-white border border-[#0B3B24]/20 text-[11px] font-semibold text-[#0B3B24] inline-flex items-center gap-1 hover:bg-[#FAF8F3] cursor-pointer shrink-0"
                  >
                    <LogOut className="w-3 h-3" />
                    <span>Switch</span>
                  </button>
                </div>
              ) : (
                <GoogleSignInButton
                  onClick={() => void handleGoogleAdminAuth()}
                  loading={authLoading}
                  label="Sign in with Google Admin Account"
                  variant="gold"
                  className="w-full"
                />
              )}
            </div>
          )}

          <div className="space-y-1.5">
            <label
              htmlFor="admin-pin-input"
              className="block text-xs font-bold uppercase tracking-wider text-[#526058]"
            >
              {isFirebaseAuthConfigured ? 'Step 2 · Store Admin PIN' : 'Enter 6-Digit Admin PIN'}
            </label>
            <div className="relative">
              <input
                id="admin-pin-input"
                type={showPin ? 'text' : 'password'}
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={12}
                autoFocus={!isFirebaseAuthConfigured || Boolean(authUser)}
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
            disabled={isFirebaseAuthConfigured && !authUser}
            className="w-full py-3 px-4 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold text-sm transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Unlock Admin Control Center
          </button>

          <div className="pt-3 border-t border-[#0B3B24]/10 flex items-center gap-2 text-xs text-[#526058]">
            <ShieldCheck className="w-4 h-4 text-[#0B3B24] shrink-0" />
            <span>
              Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020 · 8:00 AM – 9:30 PM
            </span>
          </div>
        </form>
      </main>

      <footer className="text-center text-xs text-[#526058]">
        Nav Grocery Store · Protected Admin Session
      </footer>
    </div>
  );
};
