import React, { useEffect, useState } from 'react';
import { CheckCircle2, MapPin, Phone, ShieldCheck, User, X } from 'lucide-react';
import { CustomerSession } from '../types';
import { INDIAN_PHONE_REGEX, saveCustomerProfile } from '../utils/firebase';

export const SANDHAUR_VILLAGE_AREAS = [
  'Main Bazaar, Sandhaur 148020',
  'Bus Stand Road, Sandhaur 148020',
  'Malerkotla Road, Sandhaur 148020',
  'Raikot Road, Sandhaur 148020',
  'Kalyan / Sherwani Kot (Sandhaur 148020)',
  'Dehliz Kalan / Khurd (148020)',
  'Kuthala / Matoi (Sandhaur Zone 148020)',
  'Other Nearby Village (PIN 148020)',
];

interface ProfileCompletionModalProps {
  isOpen: boolean;
  session: CustomerSession | null;
  onClose: () => void;
  onSaved: (updated: CustomerSession) => void;
}

export const ProfileCompletionModal: React.FC<ProfileCompletionModalProps> = ({
  isOpen,
  session,
  onClose,
  onSaved,
}) => {
  const [name, setName] = useState(session?.name || '');
  const [phone, setPhone] = useState(session?.phone || '');
  const [villageArea, setVillageArea] = useState(
    session?.villageArea || SANDHAUR_VILLAGE_AREAS[0]
  );
  const [address, setAddress] = useState(session?.address || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!session) return;
    setName(session.name || '');
    setPhone(session.phone || '');
    setVillageArea(session.villageArea || SANDHAUR_VILLAGE_AREAS[0]);
    setAddress(session.address || '');
    setError('');
  }, [session, isOpen]);

  if (!isOpen || !session) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const cleanName = name.trim();
    const cleanPhone = phone.replace(/\D/g, '').slice(-10);
    const cleanAddress = address.trim();

    if (cleanName.length < 2) {
      setError('Please enter your full name.');
      return;
    }
    if (!INDIAN_PHONE_REGEX.test(cleanPhone)) {
      setError('Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.');
      return;
    }
    if (cleanAddress.length < 5) {
      setError('Please enter your complete street address / house details in Sandhaur 148020.');
      return;
    }

    setSaving(true);
    const updated: CustomerSession = {
      ...session,
      name: cleanName,
      phone: cleanPhone,
      villageArea,
      address: cleanAddress,
      profileComplete: true,
      updatedAt: new Date().toISOString(),
      savedAddresses:
        session.savedAddresses && session.savedAddresses.length > 0
          ? session.savedAddresses
          : [
              {
                id: 'addr-primary',
                label: villageArea,
                recipient: cleanName,
                phone: cleanPhone,
                line1: `${cleanAddress}, ${villageArea}`,
                pincode: '148020',
                isDefault: true,
              },
            ],
    };

    const res = await saveCustomerProfile(session.uid, updated);
    setSaving(false);

    if (!res.ok) {
      setError('Could not save profile to Firebase RTDB right now. Please check your connection and try again.');
      return;
    }

    onSaved(updated);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-label="Complete Your Sandhaur Delivery Profile"
    >
      <div
        className="bg-white rounded-2xl max-w-md w-full overflow-hidden shadow-2xl border border-[#0F3D28]/15"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-4 bg-[#0F3D28] text-white flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            {session.photoURL ? (
              <img
                src={session.photoURL}
                alt={session.name}
                referrerPolicy="no-referrer"
                className="w-10 h-10 rounded-full border-2 border-[#E6A817] object-cover shrink-0"
              />
            ) : (
              <div className="w-10 h-10 rounded-full bg-[#E6A817] text-[#14221B] font-bold flex items-center justify-center shrink-0">
                {(session.name || session.email || 'U').slice(0, 1).toUpperCase()}
              </div>
            )}
            <div className="min-w-0">
              <h2 className="font-display font-bold text-base leading-tight">
                Complete Delivery Profile
              </h2>
              <p className="text-[11px] text-[#E6A817] truncate">{session.email}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close profile setup"
            className="p-1.5 rounded-lg hover:bg-white/15 cursor-pointer shrink-0"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-3.5">
          <p className="text-xs text-[#526058]">
            Save your mobile number and Sandhaur 148020 delivery details once for instant checkout and live order tracking.
          </p>

          <div>
            <label className="block text-xs font-semibold text-[#526058] mb-1">
              <span className="inline-flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-[#0F3D28]" />
                Full Name *
              </span>
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Gurpreet Singh"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#526058] mb-1">
              <span className="inline-flex items-center gap-1">
                <Phone className="w-3.5 h-3.5 text-[#0F3D28]" />
                10-Digit Indian Mobile Number (+91) *
              </span>
            </label>
            <input
              type="tel"
              required
              maxLength={10}
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
              placeholder="e.g. 9876543210"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#526058] mb-1">
              <span className="inline-flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5 text-[#0F3D28]" />
                Village / Area (Sandhaur 148020) *
              </span>
            </label>
            <select
              value={villageArea}
              onChange={(e) => setVillageArea(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm bg-white focus:outline-none focus:border-[#0F3D28]"
            >
              {SANDHAUR_VILLAGE_AREAS.map((area) => (
                <option key={area} value={area}>
                  {area}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#526058] mb-1">
              Full Street Address / Landmark *
            </label>
            <textarea
              rows={2}
              required
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="House No., Street / Mohalla, Near Landmark, Sandhaur 148020"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
            />
          </div>

          {error && (
            <p className="text-xs font-semibold text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={saving}
            className="w-full py-3 rounded-xl bg-[#0F3D28] text-white font-bold text-sm hover:bg-[#0A2A1B] inline-flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60"
          >
            <CheckCircle2 className="w-4 h-4 text-[#E6A817]" />
            <span>{saving ? 'Saving to Firebase...' : 'Save Sandhaur Profile'}</span>
          </button>

          <p className="text-[11px] text-[#526058] flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-[#0F3D28] shrink-0" />
            <span>Synced securely to your Google account profile in Firebase RTDB.</span>
          </p>
        </form>
      </div>
    </div>
  );
};
