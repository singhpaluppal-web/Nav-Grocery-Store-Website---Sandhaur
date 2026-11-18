import React, { useState } from 'react';
import { X, ShieldCheck, CheckCircle2, Mail, ArrowRight } from 'lucide-react';
import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup } from 'firebase/auth';
import { CustomerSession } from '../types';
import { FIREBASE_RTDB_BASE } from '../utils/firebase';

const firebaseConfig = { apiKey: 'AIzaSyDUaToRs_hhQNvI4-qSj61luN_stjUr1-o', authDomain: 'nav-grocery-store.firebaseapp.com', databaseURL: FIREBASE_RTDB_BASE, projectId: 'nav-grocery-store' };
const app = getApps().length ? getApp() : initializeApp(firebaseConfig);
const auth = getAuth(app);

interface WhatsAppAuthModalProps { isOpen: boolean; existingSession: CustomerSession | null; onClose: () => void; onVerified: (session: CustomerSession) => void; onLogout: () => void; }
const emailToKey = (email: string) => email.trim().toLowerCase().replace(/[^a-z0-9]/g, '_');

export const WhatsAppAuthModal: React.FC<WhatsAppAuthModalProps> = ({ isOpen, existingSession, onClose, onVerified, onLogout }) => {
  const [email, setEmail] = useState('');
  const [otpToken, setOtpToken] = useState('');
  const [otpInput, setOtpInput] = useState('');
  const [phone, setPhone] = useState(existingSession?.phone || '');
  const [name, setName] = useState(existingSession?.name || '');
  const [address, setAddress] = useState(existingSession?.address || '');
  const [step, setStep] = useState<'auth' | 'otp' | 'profile'>('auth');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  if (!isOpen) return null;

  const completeLoginAfterEmail = async (verifiedEmail: string, displayName?: string) => {
    const cleanEmail = verifiedEmail.trim().toLowerCase();
    setEmail(cleanEmail);
    if (displayName && !name) setName(displayName);
    try {
      const res = await fetch(\`${FIREBASE_RTDB_BASE}/users/${emailToKey(cleanEmail)}.json\`);
      const profile = res.ok ? await res.json() : null;
      if (profile && profile.phone && String(profile.phone).length === 10) {
        onVerified({ phone: String(profile.phone), name: profile.name || displayName || 'Sandhaur Customer', address: profile.address || 'Main Bazaar, Sandhaur 148020', verifiedAt: Date.now() });
        onClose(); return;
      }
    } catch {}
    setStep('profile');
  };

  const handleGoogleSignIn = async () => {
    setError(''); setLoading(true);
    try {
      const result = await signInWithPopup(auth, new GoogleAuthProvider());
      if (!result.user.email) throw new Error('Google account email not found.');
      await completeLoginAfterEmail(result.user.email, result.user.displayName || '');
    } catch (err: any) { setError(err?.message || 'Google sign-in failed.'); } finally { setLoading(false); }
  };

  const handleSendEmailOtp = async (e: React.FormEvent) => {
    e.preventDefault(); setError('');
    const cleanEmail = email.trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) { setError('Please enter a valid Gmail address.'); return; }
    setLoading(true);
    try {
      const res = await fetch('/api/auth/send-email-otp', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: cleanEmail }) });
      const data = await res.json();
      if (!res.ok || !data.token) throw new Error(data.error || 'Could not send OTP.');
      setOtpToken(data.token); setOtpInput(''); setStep('otp');
    } catch (err: any) { setError(err?.message || 'Failed to send OTP.'); } finally { setLoading(false); }
  };

  const handleVerifyEmailOtp = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const res = await fetch('/api/auth/verify-email-otp', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: email.trim().toLowerCase(), otp: otpInput.trim(), token: otpToken }) });
      const data = await res.json();
      if (!res.ok || !data.ok) throw new Error(data.error || 'Invalid 6-digit OTP.');
      await completeLoginAfterEmail(email);
    } catch (err: any) { setError(err?.message || 'OTP verification failed.'); } finally { setLoading(false); }
  };

  const handleSaveDeliveryProfile = async (e: React.FormEvent) => {
    e.preventDefault(); setError('');
    const digits = phone.replace(/\D/g, '').slice(-10);
    if (digits.length !== 10) { setError('Please enter a valid 10-digit Indian mobile number.'); return; }
    const finalName = name.trim() || 'Sandhaur Customer';
    const finalAddress = address.trim() || 'Main Bazaar, Sandhaur 148020';
    setLoading(true);
    try {
      if (email) {
        await fetch(\`${FIREBASE_RTDB_BASE}/users/${emailToKey(email)}.json\`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: email.trim().toLowerCase(), phone: digits, name: finalName, address: finalAddress, updatedAt: Date.now() }) });
      }
    } catch {} finally { setLoading(false); }
    onVerified({ phone: digits, name: finalName, address: finalAddress, verifiedAt: Date.now() });
    onClose();
  };
  return (
    <div className="fixed inset-0 z-50 bg-black/55 backdrop-blur-xs flex items-center justify-center p-4" onClick={onClose} role="dialog" aria-modal="true">
      <div className="bg-white rounded-2xl max-w-md w-full overflow-hidden shadow-2xl border border-[#0F3D28]/15" onClick={(e) => e.stopPropagation()}>
        <div className="px-5 py-4 bg-[#0F3D28] text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5"><ShieldCheck className="w-5 h-5 text-[#E6A817]" /><div><h2 className="font-display font-bold text-lg leading-tight">Customer Sign-In</h2><p className="text-[11px] text-white/75">Instant Sandhaur 148020 order autofill</p></div></div>
          <button type="button" onClick={onClose} className="p-1.5 rounded-lg hover:bg-white/15 cursor-pointer"><X className="w-5 h-5" /></button>
        </div>
        <div className="p-6 space-y-4">
          {existingSession ? (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-[#F8F7F2] border border-[#0F3D28]/15 space-y-1.5"><div className="flex items-center gap-2 text-sm font-bold text-[#0F3D28]"><CheckCircle2 className="w-4 h-4" /><span>Signed In</span></div><p className="text-sm font-semibold text-[#14221B]">{existingSession.name}</p><p className="text-xs font-mono-num text-[#526058]">+91 {existingSession.phone}</p><p className="text-xs text-[#526058]">{existingSession.address}</p></div>
              <div className="flex gap-3"><button type="button" onClick={() => { onLogout(); onClose(); }} className="flex-1 py-2.5 px-4 rounded-xl border border-[#0F3D28]/20 text-sm font-semibold text-[#14221B] cursor-pointer">Sign Out</button><button type="button" onClick={onClose} className="flex-1 py-2.5 px-4 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold cursor-pointer">Continue Shopping</button></div>
            </div>
          ) : step === 'auth' ? (
            <div className="space-y-4">
              <button type="button" disabled={loading} onClick={handleGoogleSignIn} className="w-full py-3 px-4 rounded-xl border-2 border-[#0F3D28]/20 bg-white hover:bg-[#F8F7F2] text-[#14221B] font-semibold text-sm flex items-center justify-center gap-2.5 cursor-pointer"><span className="font-bold text-base text-[#0F3D28]">G</span><span>{loading ? 'Connecting...' : 'Continue with Google (1-Click)'}</span></button>
              <div className="relative flex items-center py-1"><div className="flex-grow border-t border-[#0F3D28]/15"></div><span className="shrink mx-3 text-[11px] font-semibold uppercase tracking-wider text-[#526058]">Or Sign In with Gmail OTP</span><div className="flex-grow border-t border-[#0F3D28]/15"></div></div>
              <form onSubmit={handleSendEmailOtp} className="space-y-3.5">
                <div><label className="block text-xs font-semibold text-[#526058] mb-1">Your Gmail Address</label><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="yourname@gmail.com" className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]" /></div>
                {error && <p className="text-xs font-medium text-red-600">{error}</p>}
                <button type="submit" disabled={loading} className="w-full py-3 rounded-xl bg-[#0F3D28] text-white font-semibold text-sm hover:bg-[#0A2A1B] flex items-center justify-center gap-2 cursor-pointer"><Mail className="w-4 h-4 text-[#E6A817]" /><span>{loading ? 'Sending OTP...' : 'Send 6-Digit Gmail OTP'}</span></button>
              </form>
            </div>
          ) : step === 'otp' ? (
            <form onSubmit={handleVerifyEmailOtp} className="space-y-4">
              <div className="p-3.5 rounded-xl bg-[#F8F7F2] border border-[#0F3D28]/15 text-xs space-y-1"><p className="text-[#526058]">6-digit code sent to your Gmail inbox:</p><p className="font-semibold text-[#0F3D28] break-all">{email}</p></div>
              <div><label className="block text-xs font-semibold text-[#526058] mb-1">Enter 6-Digit OTP</label><input type="text" inputMode="numeric" required maxLength={6} value={otpInput} onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ''))} placeholder="Enter 6-digit code" className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num tracking-widest focus:outline-none focus:border-[#0F3D28]" /></div>
              {error && <p className="text-xs font-medium text-red-600">{error}</p>}
              <div className="flex gap-3"><button type="button" onClick={() => { setError(''); setStep('auth'); }} className="px-4 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-semibold text-[#14221B] cursor-pointer">Back</button><button type="submit" disabled={loading} className="flex-1 py-2.5 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold cursor-pointer">{loading ? 'Verifying...' : 'Verify & Sign In'}</button></div>
            </form>
          ) : (
            <form onSubmit={handleSaveDeliveryProfile} className="space-y-3.5">
              <div className="p-3 rounded-xl bg-[#F8F7F2] border border-[#0F3D28]/15 text-xs text-[#0F3D28] font-medium">One-time delivery setup for fast Sandhaur 148020 checkout:</div>
              <div><label className="block text-xs font-semibold text-[#526058] mb-1">Mobile Number (+91)</label><input type="tel" required value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="10-digit mobile number" className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]" /></div>
              <div><label className="block text-xs font-semibold text-[#526058] mb-1">Your Name</label><input type="text" required value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Gurpreet Singh" className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]" /></div>
              <div><label className="block text-xs font-semibold text-[#526058] mb-1">Delivery Address / Village (Sandhaur 148020)</label><input type="text" required value={address} onChange={(e) => setAddress(e.target.value)} placeholder="House / Street / Village" className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]" /></div>
              {error && <p className="text-xs font-medium text-red-600">{error}</p>}
              <button type="submit" disabled={loading} className="w-full py-3 rounded-xl bg-[#0F3D28] text-white font-semibold text-sm flex items-center justify-center gap-2 cursor-pointer"><span>{loading ? 'Saving...' : 'Save & Continue'}</span><ArrowRight className="w-4 h-4" /></button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
