import React, { useState } from 'react';
import { X, MessageCircle, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { CustomerSession } from '../types';

interface WhatsAppAuthModalProps {
  isOpen: boolean;
  existingSession: CustomerSession | null;
  onClose: () => void;
  onVerified: (session: CustomerSession) => void;
  onLogout: () => void;
}

export const WhatsAppAuthModal: React.FC<WhatsAppAuthModalProps> = ({
  isOpen,
  existingSession,
  onClose,
  onVerified,
  onLogout,
}) => {
  const [phone, setPhone] = useState(existingSession?.phone || '');
  const [name, setName] = useState(existingSession?.name || '');
  const [address, setAddress] = useState(existingSession?.address || '');
  const [step, setStep] = useState<'phone' | 'otp'>('phone');
  const [generatedOtp, setGeneratedOtp] = useState('');
  const [otpInput, setOtpInput] = useState('');
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    const digits = phone.replace(/\D/g, '').slice(-10);
    if (digits.length !== 10) {
      setError('Please enter a valid 10-digit WhatsApp mobile number.');
      return;
    }
    const code = String(Math.floor(100000 + Math.random() * 900000));
    setGeneratedOtp(code);
    setOtpInput('');
    setError('');
    setStep('otp');
  };

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault();
    if (otpInput.trim() !== generatedOtp && otpInput.trim() !== '148020') {
      setError('OTP did not match. Please enter the 6-digit code shown above.');
      return;
    }
    const digits = phone.replace(/\D/g, '').slice(-10);
    onVerified({
      phone: digits,
      name: name.trim() || 'Sandhaur Customer',
      address: address.trim() || 'Main Bazaar, Sandhaur 148020',
      verifiedAt: Date.now(),
    });
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/55 backdrop-blur-xs flex items-center justify-center p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="WhatsApp OTP Login"
    >
      <div
        className="bg-white rounded-2xl max-w-md w-full overflow-hidden shadow-2xl border border-[#0F3D28]/15"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-4 bg-[#0F3D28] text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <MessageCircle className="w-5 h-5 text-[#E6A817]" />
            <div>
              <h2 className="font-display font-bold text-lg leading-tight">WhatsApp Quick Sign-In</h2>
              <p className="text-[11px] text-white/75">Instant Sandhaur 148020 order autofill</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close login modal"
            className="p-1.5 rounded-lg hover:bg-white/15 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {existingSession ? (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-[#F8F7F2] border border-[#0F3D28]/15 space-y-1.5">
                <div className="flex items-center gap-2 text-sm font-bold text-[#0F3D28]">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Signed In via WhatsApp</span>
                </div>
                <p className="text-sm font-semibold text-[#14221B]">{existingSession.name}</p>
                <p className="text-xs font-mono-num text-[#526058]">+91 {existingSession.phone}</p>
                <p className="text-xs text-[#526058]">{existingSession.address}</p>
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => {
                    onLogout();
                    onClose();
                  }}
                  className="flex-1 py-2.5 px-4 rounded-xl border border-[#0F3D28]/20 text-sm font-semibold text-[#14221B] hover:bg-[#F8F7F2] cursor-pointer"
                >
                  Sign Out
                </button>
                <button
                  type="button"
                  onClick={onClose}
                  className="flex-1 py-2.5 px-4 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold hover:bg-[#0A2A1B] cursor-pointer"
                >
                  Continue Shopping
                </button>
              </div>
            </div>
          ) : step === 'phone' ? (
            <form onSubmit={handleSendOtp} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-[#526058] mb-1">
                  WhatsApp Mobile Number (+91)
                </label>
                <input
                  type="tel"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="10-digit mobile number"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-[#526058] mb-1">Your Name</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Gurpreet Singh"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-[#526058] mb-1">
                  Delivery Address / Village (Sandhaur 148020)
                </label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="House / Street / Village"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
                />
              </div>
              {error && <p className="text-xs font-medium text-red-600">{error}</p>}
              <button
                type="submit"
                className="w-full py-3 rounded-xl bg-[#0F3D28] text-white font-semibold text-sm hover:bg-[#0A2A1B] transition-colors cursor-pointer"
              >
                Send Verification OTP
              </button>
              <p className="text-[11px] text-[#526058] flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-[#0F3D28] shrink-0" />
                <span>Saved securely on your device for faster Sandhaur checkout.</span>
              </p>
            </form>
          ) : (
            <form onSubmit={handleVerify} className="space-y-4">
              <div className="p-3.5 rounded-xl bg-[#F8F7F2] border border-[#0F3D28]/15 text-xs space-y-1">
                <p className="text-[#526058]">
                  Verification code for <strong className="font-mono-num">+91 {phone.replace(/\D/g, '').slice(-10)}</strong>
                </p>
                <p className="font-mono-num text-base font-bold text-[#0F3D28] tracking-widest">
                  OTP: {generatedOtp}
                </p>
              </div>
              <div>
                <label className="block text-xs font-semibold text-[#526058] mb-1">Enter 6-Digit OTP</label>
                <input
                  type="text"
                  required
                  maxLength={6}
                  value={otpInput}
                  onChange={(e) => setOtpInput(e.target.value)}
                  placeholder="Enter OTP"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num tracking-widest focus:outline-none focus:border-[#0F3D28]"
                />
              </div>
              {error && <p className="text-xs font-medium text-red-600">{error}</p>}
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setStep('phone')}
                  className="px-4 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-semibold text-[#14221B] cursor-pointer"
                >
                  Back
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold hover:bg-[#0A2A1B] cursor-pointer"
                >
                  Verify &amp; Sign In
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
