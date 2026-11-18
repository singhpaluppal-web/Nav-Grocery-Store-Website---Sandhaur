import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  Edit3,
  Heart,
  LogOut,
  MapPin,
  PackageCheck,
  Phone,
  Plus,
  Radio,
  RotateCcw,
  Save,
  ShieldCheck,
  Trash2,
  User,
  X,
} from 'lucide-react';
import {
  CustomerSession,
  OrderRecord,
  PaymentMethod,
  PaymentSettings,
  Product,
  ReturnRequest,
  SavedAddress,
} from '../types';
import {
  INDIAN_PHONE_REGEX,
  fetchOrderById,
  getLocalReturnRequests,
  saveCustomerProfile,
  submitReturnRequest,
  subscribeToCustomerOrders,
} from '../utils/firebase';
import { resolveProductImage, getNextFallbackImage } from '../utils/brandPhotos';
import { StoreLang } from '../utils/i18n';
import { LanguageSwitcher } from './LanguageSwitcher';
import { StoreLogo } from './StoreLogo';
import { GoogleSignInButton } from './GoogleSignInButton';
import { LiveOrderTrackerCard } from './LiveOrderTrackerCard';
import { SANDHAUR_VILLAGE_AREAS } from './ProfileCompletionModal';

interface AccountPageViewProps {
  logoUrl: string;
  products: Product[];
  favoriteIds: string[];
  customerSession: CustomerSession | null;
  paymentSettings: PaymentSettings;
  lang: StoreLang;
  authLoading: boolean;
  authError: string;
  onChangeLang: (lang: StoreLang) => void;
  onUpdateSession: (session: CustomerSession | null) => void;
  onToggleFavorite: (productId: string) => void;
  onAddToCart: (product: Product, qty?: number) => void;
  onNavigateHome: () => void;
  onSignInWithGoogle: () => Promise<void>;
  onSignOut: () => Promise<void>;
  onClearAuthError: () => void;
}

type AccountSection = 'orders' | 'profile' | 'wishlist' | 'addresses' | 'payments' | 'returns';

const DEFAULT_SAVED_ADDRESS: SavedAddress = {
  id: 'addr-sandhaur',
  label: 'Home · Sandhaur',
  recipient: 'Sandhaur Shopper',
  phone: '8847370905',
  line1: 'Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab',
  pincode: '148020',
  isDefault: true,
};

export const AccountPageView: React.FC<AccountPageViewProps> = ({
  logoUrl,
  products,
  favoriteIds,
  customerSession,
  paymentSettings,
  lang,
  authLoading,
  authError,
  onChangeLang,
  onUpdateSession,
  onToggleFavorite,
  onAddToCart,
  onNavigateHome,
  onSignInWithGoogle,
  onSignOut,
  onClearAuthError,
}) => {
  const [section, setSection] = useState<AccountSection>('orders');

  // Real-time orders filtered strictly by customerSession.uid
  const [orders, setOrders] = useState<OrderRecord[]>([]);
  const [ordersLoading, setOrdersLoading] = useState<boolean>(false);
  const [liveSynced, setLiveSynced] = useState<boolean>(false);
  const [lookupId, setLookupId] = useState('');
  const [lookupBusy, setLookupBusy] = useState(false);
  const [lookupStatus, setLookupStatus] = useState('');
  const [reorderToast, setReorderToast] = useState('');

  // Inline Edit Profile state synced to /customers/{uid}.json
  const [profileName, setProfileName] = useState(customerSession?.name || '');
  const [profilePhone, setProfilePhone] = useState(customerSession?.phone || '');
  const [profileVillage, setProfileVillage] = useState(
    customerSession?.villageArea || SANDHAUR_VILLAGE_AREAS[0]
  );
  const [profileAddress, setProfileAddress] = useState(customerSession?.address || '');
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileMsg, setProfileMsg] = useState<{ type: 'ok' | 'err'; text: string } | null>(null);

  const [addresses, setAddresses] = useState<SavedAddress[]>(() => {
    if (customerSession?.savedAddresses && customerSession.savedAddresses.length > 0) {
      return customerSession.savedAddresses;
    }
    return [DEFAULT_SAVED_ADDRESS];
  });
  const [addrLabel, setAddrLabel] = useState('');
  const [addrLine, setAddrLine] = useState('');
  const [addrPin, setAddrPin] = useState('148020');

  const [preferredPay, setPreferredPay] = useState<PaymentMethod>(
    customerSession?.preferredPayment || 'upi'
  );

  const [returns, setReturns] = useState<ReturnRequest[]>(() => getLocalReturnRequests());
  const [returnOrderId, setReturnOrderId] = useState('');
  const [returnReason, setReturnReason] = useState('Sealed Pack Damaged in Transit');
  const [returnNotes, setReturnNotes] = useState('');
  const [returnMsg, setReturnMsg] = useState('');

  // Keep profile form & addresses synced whenever customerSession updates
  useEffect(() => {
    if (!customerSession) {
      setProfileName('');
      setProfilePhone('');
      setProfileVillage(SANDHAUR_VILLAGE_AREAS[0]);
      setProfileAddress('');
      setAddresses([DEFAULT_SAVED_ADDRESS]);
      return;
    }
    setProfileName(customerSession.name || '');
    setProfilePhone(customerSession.phone || '');
    setProfileVillage(customerSession.villageArea || SANDHAUR_VILLAGE_AREAS[0]);
    setProfileAddress(customerSession.address || '');
    if (customerSession.preferredPayment) {
      setPreferredPay(customerSession.preferredPayment);
    }
    if (customerSession.savedAddresses && customerSession.savedAddresses.length > 0) {
      setAddresses(customerSession.savedAddresses);
    }
  }, [customerSession]);

  // Live Firebase RTDB subscription to /orders filtered by signed-in uid
  useEffect(() => {
    if (!customerSession?.uid) {
      setOrders([]);
      setOrdersLoading(false);
      setLiveSynced(false);
      return;
    }

    setOrdersLoading(true);
    const unsubscribe = subscribeToCustomerOrders(customerSession.uid, (nextOrders, connected) => {
      setOrders(nextOrders);
      setLiveSynced(connected);
      setOrdersLoading(false);
    });
    return unsubscribe;
  }, [customerSession?.uid]);

  const favoriteSet = useMemo(() => new Set(favoriteIds), [favoriteIds]);
  const wishlistProducts = useMemo(
    () => products.filter((p) => favoriteSet.has(p.id)),
    [products, favoriteSet]
  );

  const persistProfileToFirebase = async (nextSession: CustomerSession) => {
    onUpdateSession(nextSession);
    return saveCustomerProfile(nextSession.uid, nextSession);
  };

  const handleSaveInlineProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerSession?.uid) return;
    setProfileMsg(null);

    const cleanName = profileName.trim();
    const cleanPhone = profilePhone.replace(/\D/g, '').slice(-10);
    const cleanAddr = profileAddress.trim();

    if (cleanName.length < 2) {
      setProfileMsg({ type: 'err', text: 'Please enter your full name.' });
      return;
    }
    if (!INDIAN_PHONE_REGEX.test(cleanPhone)) {
      setProfileMsg({
        type: 'err',
        text: 'Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.',
      });
      return;
    }
    if (cleanAddr.length < 5) {
      setProfileMsg({
        type: 'err',
        text: 'Please enter your complete street address in Sandhaur 148020.',
      });
      return;
    }

    setProfileSaving(true);
    const updated: CustomerSession = {
      ...customerSession,
      name: cleanName,
      phone: cleanPhone,
      villageArea: profileVillage,
      address: cleanAddr,
      profileComplete: true,
      updatedAt: new Date().toISOString(),
    };
    const res = await persistProfileToFirebase(updated);
    setProfileSaving(false);

    if (res.ok) {
      setProfileMsg({
        type: 'ok',
        text: 'Profile saved to Firebase Realtime Database (/customers/' + customerSession.uid.slice(0, 8) + '…).',
      });
    } else {
      setProfileMsg({
        type: 'err',
        text: 'Updated locally, but Firebase RTDB sync failed. Please retry.',
      });
    }
  };

  const persistAddresses = (next: SavedAddress[]) => {
    setAddresses(next);
    if (customerSession) {
      const updated: CustomerSession = {
        ...customerSession,
        address: next.find((a) => a.isDefault)?.line1 || customerSession.address,
        savedAddresses: next,
        updatedAt: new Date().toISOString(),
      };
      void persistProfileToFirebase(updated);
    }
  };

  const handleLookupOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    const id = lookupId.trim().toUpperCase();
    if (!id) return;
    setLookupBusy(true);
    setLookupStatus('');
    const found = await fetchOrderById(id);
    setLookupBusy(false);
    if (found) {
      setOrders((prev) => [found, ...prev.filter((o) => o.orderId !== found.orderId)]);
      setLookupStatus(`Synced latest status for ${found.orderId}: ${found.status}`);
    } else {
      setLookupStatus(`Order ID "${id}" not found in Firebase RTDB.`);
    }
  };

  const handleReorder = (order: OrderRecord) => {
    let totalUnits = 0;
    order.items.forEach((it) => {
      const match = products.find((p) => p.id === it.id);
      if (match) {
        const qty = Math.max(1, Math.floor(Number(it.qty) || 1));
        onAddToCart(match, qty);
        totalUnits += qty;
      }
    });
    if (totalUnits > 0) {
      setReorderToast(`Added ${totalUnits} unit${totalUnits === 1 ? '' : 's'} from ${order.orderId} to your cart.`);
      setTimeout(() => {
        onNavigateHome();
      }, 450);
    }
  };

  const handleAddAddress = (e: React.FormEvent) => {
    e.preventDefault();
    if (!addrLabel.trim() || !addrLine.trim()) return;
    const item: SavedAddress = {
      id: `addr-${Date.now()}`,
      label: addrLabel.trim(),
      recipient: customerSession?.name || 'Verified Customer',
      phone: customerSession?.phone || '8847370905',
      line1: addrLine.trim(),
      pincode: addrPin.trim() || '148020',
      isDefault: addresses.length === 0,
    };
    persistAddresses([...addresses, item]);
    setAddrLabel('');
    setAddrLine('');
  };

  const handlePreferredPaymentSave = (method: PaymentMethod) => {
    setPreferredPay(method);
    if (customerSession) {
      const updated: CustomerSession = {
        ...customerSession,
        preferredPayment: method,
        updatedAt: new Date().toISOString(),
      };
      void persistProfileToFirebase(updated);
    }
  };

  const handleSubmitReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanId = returnOrderId.trim().toUpperCase();
    if (!cleanId) return;
    const req: ReturnRequest = {
      requestId: `RET-${Math.floor(10000 + Math.random() * 90000)}`,
      orderId: cleanId,
      customerPhone: customerSession?.phone || '8847370905',
      reason: returnReason,
      notes: returnNotes.trim() || undefined,
      status: 'Submitted',
      createdAt: new Date().toISOString(),
    };
    await submitReturnRequest(req);
    setReturns((prev) => [req, ...prev]);
    setReturnOrderId('');
    setReturnNotes('');
    setReturnMsg(
      `Claim ${req.requestId} submitted for order ${req.orderId}. Our Sandhaur support desk will follow up shortly.`
    );
  };

  const navTabs: Array<{ id: AccountSection; label: string; icon: React.ReactNode }> = [
    {
      id: 'orders',
      label: `${lang === 'pa' ? 'ਆਰਡਰ ਤੇ ਲਾਈਵ ਟ੍ਰੈਕਿੰਗ' : 'Live Orders & Tracking'} (${orders.length})`,
      icon: <PackageCheck className="w-4 h-4" />,
    },
    {
      id: 'profile',
      label: lang === 'pa' ? 'ਪ੍ਰੋਫਾਈਲ ਸੰਪਾਦਿਤ ਕਰੋ' : 'Edit Profile',
      icon: <Edit3 className="w-4 h-4" />,
    },
    {
      id: 'wishlist',
      label: `${lang === 'pa' ? 'ਪਸੰਦੀਦਾ' : 'Wishlist'} (${wishlistProducts.length})`,
      icon: <Heart className="w-4 h-4" />,
    },
    {
      id: 'addresses',
      label: lang === 'pa' ? 'ਸੇਵ ਪਤੇ' : 'Saved Addresses',
      icon: <MapPin className="w-4 h-4" />,
    },
    {
      id: 'payments',
      label: lang === 'pa' ? 'ਭੁਗਤਾਨ ਢੰਗ' : 'Payment Methods',
      icon: <CreditCard className="w-4 h-4" />,
    },
    {
      id: 'returns',
      label: lang === 'pa' ? 'ਵਾਪਸੀ ਤੇ ਵਾਰੰਟੀ' : 'Returns & Warranty',
      icon: <RotateCcw className="w-4 h-4" />,
    },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-[#F8F7F2] text-[#14221B]">
      {/* Account Top Header */}
      <header className="sticky top-0 z-30 bg-[#0F3D28] text-white border-b border-[#E6A817]/25">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onNavigateHome}
              className="px-3 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4 text-[#E6A817]" />
              <span>{lang === 'pa' ? 'ਸਟੋਰ ’ਤੇ ਵਾਪਸ' : 'Back to Store'}</span>
            </button>
            <div className="hidden sm:flex items-center gap-2.5">
              <StoreLogo logoUrl={logoUrl} darkSurface sizeClass="w-9 h-9" />
              <div>
                <p className="font-display font-bold text-sm leading-tight">
                  {lang === 'pa' ? 'ਮੇਰਾ ਖਾਤਾ' : 'Customer Account'}
                </p>
                <p className="text-[11px] text-[#E6A817]">
                  Nav Grocery &amp; Nav Kart · Main Bazaar, Sandhaur 148020
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <LanguageSwitcher lang={lang} onChange={onChangeLang} />
            {customerSession ? (
              <button
                type="button"
                onClick={() => void onSignOut()}
                className="px-3 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-[#E6A817] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>{lang === 'pa' ? 'ਸਾਈਨ ਆਊਟ' : 'Sign Out'}</span>
              </button>
            ) : (
              <GoogleSignInButton
                onClick={() => void onSignInWithGoogle()}
                loading={authLoading}
                label={lang === 'pa' ? 'ਗੂਗਲ ਨਾਲ ਜਾਰੀ ਰੱਖੋ' : 'Continue with Google'}
                variant="gold"
                className="py-2! px-3! text-xs!"
              />
            )}
          </div>
        </div>

        {authError && (
          <div className="bg-amber-50 text-[#78350F] border-t border-amber-200 px-4 py-2 text-xs flex items-center justify-between gap-3">
            <span className="font-semibold">{authError}</span>
            <button
              type="button"
              onClick={onClearAuthError}
              aria-label="Dismiss error"
              className="p-0.5 hover:bg-amber-200/60 rounded cursor-pointer shrink-0"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-6">
        {/* Profile Membership Banner */}
        <section className="bg-[#0F3D28] text-white rounded-2xl p-6 border border-[#E6A817]/30 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start gap-4 min-w-0">
            {customerSession?.photoURL ? (
              <img
                src={customerSession.photoURL}
                alt={customerSession.name}
                referrerPolicy="no-referrer"
                className="w-14 h-14 rounded-full border-2 border-[#E6A817] object-cover shrink-0"
              />
            ) : (
              <div className="w-14 h-14 rounded-full bg-[#E6A817] text-[#14221B] font-display font-bold text-xl flex items-center justify-center shrink-0">
                {customerSession ? customerSession.name.slice(0, 1).toUpperCase() : 'N'}
              </div>
            )}

            <div className="space-y-1 min-w-0">
              <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#E6A817]">
                <ShieldCheck className="w-4 h-4 shrink-0" />
                <span>Nav Pass Wholesale Membership · Flat 12% Off MRP</span>
              </div>
              <h1 className="font-display text-2xl font-bold truncate">
                {customerSession ? customerSession.name : 'Welcome, Sandhaur Shopper'}
              </h1>
              {customerSession ? (
                <div className="text-xs sm:text-sm text-white/85 space-y-0.5">
                  <p className="truncate">{customerSession.email}</p>
                  <p className="text-white/75">
                    {customerSession.phone ? `+91 ${customerSession.phone}` : 'Mobile not set'}
                    {' · '}
                    {customerSession.address
                      ? `${customerSession.address}${
                          customerSession.villageArea &&
                          !customerSession.address.includes(customerSession.villageArea)
                            ? `, ${customerSession.villageArea}`
                            : ''
                        }`
                      : 'Complete your Sandhaur 148020 delivery address in Edit Profile'}
                  </p>
                </div>
              ) : (
                <p className="text-xs sm:text-sm text-white/80">
                  Sign in with Google to save your Sandhaur 148020 delivery profile and track orders live in real time.
                </p>
              )}
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 shrink-0">
            {!customerSession && (
              <GoogleSignInButton
                onClick={() => void onSignInWithGoogle()}
                loading={authLoading}
                label={lang === 'pa' ? 'ਗੂਗਲ ਨਾਲ ਜਾਰੀ ਰੱਖੋ' : 'Continue with Google'}
                variant="gold"
              />
            )}
            <div className="grid grid-cols-3 gap-3 text-center bg-[#0A2A1B] p-3.5 rounded-xl border border-white/10">
              <div>
                <p className="text-[11px] text-white/70">Discount</p>
                <p className="font-bold text-sm text-[#E6A817]">12% Off</p>
              </div>
              <div>
                <p className="text-[11px] text-white/70">Orders</p>
                <p className="font-bold text-sm font-mono-num">{orders.length}</p>
              </div>
              <div>
                <p className="text-[11px] text-white/70">Wishlist</p>
                <p className="font-bold text-sm font-mono-num">{wishlistProducts.length}</p>
              </div>
            </div>
          </div>
        </section>

        {/* Quick Section Navigation Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
          {navTabs.map((tab) => {
            const active = section === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setSection(tab.id)}
                className={`px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold inline-flex items-center gap-2 whitespace-nowrap shrink-0 cursor-pointer transition-colors ${
                  active
                    ? 'bg-[#0F3D28] text-white'
                    : 'bg-white text-[#14221B] border border-[#0F3D28]/12 hover:border-[#0F3D28]/40'
                }`}
              >
                <span className={active ? 'text-[#E6A817]' : 'text-[#0F3D28]'}>{tab.icon}</span>
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {reorderToast && (
          <div className="bg-[#0F3D28] text-white px-4 py-3 rounded-xl text-xs font-bold flex items-center justify-between gap-2">
            <span>{reorderToast}</span>
            <button
              type="button"
              onClick={() => setReorderToast('')}
              className="text-[#E6A817] hover:underline cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* SECTION: REAL-TIME ORDERS & TRACKING */}
        {section === 'orders' && (
          <section className="space-y-4">
            {/* Live Sync Header + Manual Order ID Lookup */}
            <div className="bg-white rounded-xl p-4 border border-[#0F3D28]/12 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Radio
                    className={`w-4 h-4 ${
                      customerSession && liveSynced ? 'text-[#0F3D28] animate-pulse' : 'text-[#526058]'
                    }`}
                  />
                  <h2 className="font-display font-bold text-base text-[#14221B]">
                    Real-Time Sandhaur 148020 Order Tracker
                  </h2>
                </div>
                {customerSession && (
                  <span className="text-[11px] font-semibold text-[#0F3D28] bg-[#F8F7F2] px-2.5 py-1 rounded-lg border border-[#0F3D28]/15">
                    {liveSynced
                      ? 'Auto-syncing live from Firebase RTDB'
                      : 'Syncing orders for your Google account'}
                  </span>
                )}
              </div>

              <form onSubmit={handleLookupOrder} className="flex flex-col sm:flex-row gap-2.5">
                <input
                  type="text"
                  value={lookupId}
                  onChange={(e) => setLookupId(e.target.value)}
                  placeholder="Or look up any Order ID directly (e.g. NGS-261118-4821)"
                  className="flex-1 px-3.5 py-2.5 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
                />
                <button
                  type="submit"
                  disabled={lookupBusy}
                  className="px-5 py-2.5 rounded-lg bg-[#0F3D28] text-white text-xs font-bold whitespace-nowrap cursor-pointer disabled:opacity-50"
                >
                  {lookupBusy ? 'Checking...' : 'Track by Order ID'}
                </button>
              </form>
              {lookupStatus && (
                <p className="text-xs font-semibold text-[#0F3D28]">{lookupStatus}</p>
              )}
            </div>

            {!customerSession ? (
              <div className="bg-white rounded-2xl p-10 text-center border border-[#0F3D28]/12 space-y-4">
                <p className="font-display text-lg font-bold text-[#14221B]">
                  Sign in with Google to View Your Live Orders
                </p>
                <p className="text-xs sm:text-sm text-[#526058] max-w-md mx-auto">
                  Every order placed with your Google account automatically updates here in real time — from Order Placed to Packed &amp; Sealed, Out for Delivery in Sandhaur 148020, and Delivered.
                </p>
                <div className="flex justify-center">
                  <GoogleSignInButton
                    onClick={() => void onSignInWithGoogle()}
                    loading={authLoading}
                    variant="gold"
                  />
                </div>
              </div>
            ) : ordersLoading ? (
              <div className="bg-white rounded-2xl p-8 text-center border border-[#0F3D28]/10 text-xs sm:text-sm text-[#526058]">
                Connecting to Firebase Realtime Database for your orders...
              </div>
            ) : orders.length === 0 ? (
              <div className="bg-white rounded-2xl p-10 text-center border border-[#0F3D28]/10 space-y-3">
                <p className="font-display text-lg font-bold">No orders found for {customerSession.email}</p>
                <p className="text-xs sm:text-sm text-[#526058]">
                  Place an order on Nav Grocery Store or Nav Kart to watch live 4-stage Sandhaur 148020 delivery updates here.
                </p>
                <button
                  type="button"
                  onClick={onNavigateHome}
                  className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-xs font-bold cursor-pointer"
                >
                  Browse 308+ Sealed Items
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {orders.map((order) => (
                  <LiveOrderTrackerCard
                    key={order.orderId}
                    order={order}
                    onReorder={handleReorder}
                  />
                ))}
              </div>
            )}
          </section>
        )}

        {/* SECTION: EDIT CUSTOMER PROFILE (/customers/{uid}.json) */}
        {section === 'profile' && (
          <section className="max-w-2xl">
            {!customerSession ? (
              <div className="bg-white rounded-2xl p-8 text-center border border-[#0F3D28]/12 space-y-4">
                <p className="font-display text-lg font-bold">Sign in with Google to Edit Your Profile</p>
                <p className="text-xs sm:text-sm text-[#526058]">
                  Your Name, 10-digit Indian mobile number, and Sandhaur 148020 delivery address are stored securely under your Google account in Firebase RTDB.
                </p>
                <div className="flex justify-center">
                  <GoogleSignInButton
                    onClick={() => void onSignInWithGoogle()}
                    loading={authLoading}
                    variant="gold"
                  />
                </div>
              </div>
            ) : (
              <form
                onSubmit={handleSaveInlineProfile}
                className="bg-white rounded-2xl p-6 border border-[#0F3D28]/12 space-y-4"
              >
                <div className="flex items-center justify-between gap-3 border-b border-[#0F3D28]/10 pb-3">
                  <div>
                    <h2 className="font-display font-bold text-lg text-[#14221B]">
                      Edit Sandhaur 148020 Delivery Profile
                    </h2>
                    <p className="text-xs text-[#526058]">
                      Google Account: <strong>{customerSession.email}</strong>
                    </p>
                  </div>
                  <span className="text-[11px] font-mono-num text-[#0F3D28] bg-[#F8F7F2] px-2.5 py-1 rounded-lg border border-[#0F3D28]/15">
                    PIN 148020
                  </span>
                </div>

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
                    value={profileName}
                    onChange={(e) => setProfileName(e.target.value)}
                    placeholder="Customer full name"
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
                    value={profilePhone}
                    onChange={(e) => setProfilePhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                    placeholder="e.g. 9876543210"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-[#526058] mb-1">
                    <span className="inline-flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-[#0F3D28]" />
                      Village / Area in Sandhaur 148020 *
                    </span>
                  </label>
                  <select
                    value={profileVillage}
                    onChange={(e) => setProfileVillage(e.target.value)}
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
                    Full Street Address / Landmark (Sandhaur 148020) *
                  </label>
                  <textarea
                    rows={3}
                    required
                    value={profileAddress}
                    onChange={(e) => setProfileAddress(e.target.value)}
                    placeholder="House No., Street / Mohalla, Near Landmark, Sandhaur 148020"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
                  />
                </div>

                {profileMsg && (
                  <p
                    className={`text-xs font-semibold px-3 py-2 rounded-lg border ${
                      profileMsg.type === 'ok'
                        ? 'bg-[#0F3D28]/8 text-[#0F3D28] border-[#0F3D28]/20'
                        : 'bg-red-50 text-red-700 border-red-200'
                    }`}
                  >
                    {profileMsg.text}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={profileSaving}
                  className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-xs sm:text-sm font-bold inline-flex items-center gap-2 hover:bg-[#0A2A1B] cursor-pointer disabled:opacity-60"
                >
                  <Save className="w-4 h-4 text-[#E6A817]" />
                  <span>{profileSaving ? 'Saving to Firebase...' : 'Save Profile Changes'}</span>
                </button>
              </form>
            )}
          </section>
        )}

        {/* SECTION: WISHLIST */}
        {section === 'wishlist' && (
          <section className="space-y-4">
            {wishlistProducts.length === 0 ? (
              <div className="bg-white rounded-2xl p-10 text-center border border-[#0F3D28]/10 space-y-3">
                <p className="font-display text-lg font-bold">No wishlist items yet</p>
                <p className="text-xs sm:text-sm text-[#526058]">
                  Tap the heart icon on any grocery, frozen, electronics, or apparel product to save it here.
                </p>
                <button
                  type="button"
                  onClick={onNavigateHome}
                  className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-xs font-bold cursor-pointer"
                >
                  Explore Storefront
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {wishlistProducts.map((item) => (
                  <div
                    key={item.id}
                    className="bg-white rounded-xl p-4 border border-[#0F3D28]/12 flex items-center gap-3.5"
                  >
                    <img
                      src={resolveProductImage(item)}
                      alt={item.name}
                      className="w-16 h-16 rounded-lg object-contain bg-[#F8F7F2] shrink-0"
                      onError={(e) => {
                        const el = e.currentTarget;
                        if (el.src.startsWith('data:image/svg+xml')) {
                          el.onerror = null;
                          return;
                        }
                        el.src = getNextFallbackImage(item, el.getAttribute('src') || el.src);
                      }}
                    />
                    <div className="min-w-0 flex-1 space-y-1">
                      <p className="text-[11px] font-bold text-[#526058] uppercase">
                        {item.brand} · {item.id}
                      </p>
                      <p className="text-xs font-bold text-[#14221B] line-clamp-2">{item.name}</p>
                      <p className="text-xs font-mono-num font-bold text-[#0F3D28]">
                        ₹{item.sale}{' '}
                        <span className="line-through text-[#526058] font-normal">₹{item.mrp}</span>
                      </p>
                      <div className="flex items-center gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() => onAddToCart(item, 1)}
                          className="px-2.5 py-1 rounded-lg bg-[#0F3D28] text-white text-[11px] font-bold inline-flex items-center gap-1 cursor-pointer"
                        >
                          <Plus className="w-3 h-3 text-[#E6A817]" />
                          <span>Add</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => onToggleFavorite(item.id)}
                          className="px-2.5 py-1 rounded-lg border border-red-200 text-red-700 text-[11px] font-semibold cursor-pointer"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* SECTION: SAVED ADDRESSES */}
        {section === 'addresses' && (
          <section className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <div className="lg:col-span-7 space-y-3">
              {addresses.map((addr) => (
                <div
                  key={addr.id}
                  className="bg-white rounded-xl p-4 border border-[#0F3D28]/15 flex items-start justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-[#0F3D28] uppercase">{addr.label}</span>
                      {addr.isDefault && (
                        <span className="text-[11px] font-bold text-[#0F3D28] inline-flex items-center gap-1">
                          <CheckCircle2 className="w-3.5 h-3.5 text-[#E6A817]" />
                          Default
                        </span>
                      )}
                    </div>
                    <p className="text-sm font-semibold text-[#14221B]">{addr.line1}</p>
                    <p className="text-xs text-[#526058] font-mono-num">
                      PIN: {addr.pincode} · Phone: +91 {addr.phone}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {!addr.isDefault && (
                      <button
                        type="button"
                        onClick={() =>
                          persistAddresses(
                            addresses.map((a) => ({ ...a, isDefault: a.id === addr.id }))
                          )
                        }
                        className="px-2.5 py-1 rounded-lg border border-[#0F3D28]/25 text-xs font-bold text-[#0F3D28] cursor-pointer"
                      >
                        Set Default
                      </button>
                    )}
                    {addresses.length > 1 && (
                      <button
                        type="button"
                        onClick={() =>
                          persistAddresses(addresses.filter((a) => a.id !== addr.id))
                        }
                        aria-label="Delete address"
                        className="p-1.5 rounded-lg text-red-700 hover:bg-red-50 cursor-pointer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <form
              onSubmit={handleAddAddress}
              className="lg:col-span-5 bg-white rounded-xl p-5 border border-[#0F3D28]/12 space-y-3 h-fit"
            >
              <h3 className="font-display font-bold text-base">Add Sandhaur / Punjab Address</h3>
              <input
                type="text"
                required
                value={addrLabel}
                onChange={(e) => setAddrLabel(e.target.value)}
                placeholder="Label (e.g. Home, Shop, Farmhouse)"
                className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm"
              />
              <textarea
                rows={2}
                required
                value={addrLine}
                onChange={(e) => setAddrLine(e.target.value)}
                placeholder="Street, Main Bazaar / Village, Distt. Malerkotla"
                className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm"
              />
              <input
                type="text"
                value={addrPin}
                onChange={(e) => setAddrPin(e.target.value)}
                placeholder="Pincode (e.g. 148020)"
                className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm font-mono-num"
              />
              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#0F3D28] text-white text-xs font-bold cursor-pointer"
              >
                Save Delivery Address
              </button>
            </form>
          </section>
        )}

        {/* SECTION: PAYMENT METHODS */}
        {section === 'payments' && (
          <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[
              {
                id: 'upi' as PaymentMethod,
                title: 'UPI Instant Pay (GPay, PhonePe, Paytm, BHIM)',
                desc: `Merchant VPA: ${paymentSettings.upiId} · Dynamic QR supported`,
              },
              {
                id: 'razorpay' as PaymentMethod,
                title: 'Razorpay Cards / NetBanking / Wallets',
                desc: 'Encrypted online INR checkout with instant confirmation',
              },
              {
                id: 'bank_transfer' as PaymentMethod,
                title: 'Bank Transfer (NEFT / IMPS)',
                desc: `A/C ${paymentSettings.bankAccountNumber || '1480208847370905'} · IFSC ${paymentSettings.bankIfsc || 'PUNB0148020'}`,
              },
              {
                id: 'cod' as PaymentMethod,
                title: 'Cash on Delivery (COD) / Main Bazaar Pickup',
                desc: `Available on orders ₹${paymentSettings.codMinOrder}+ in Sandhaur 148020`,
              },
            ].map((opt) => {
              const active = preferredPay === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => handlePreferredPaymentSave(opt.id)}
                  className={`p-4 rounded-xl border text-left transition-colors cursor-pointer ${
                    active
                      ? 'bg-[#0F3D28] text-white border-[#0F3D28]'
                      : 'bg-white text-[#14221B] border-[#0F3D28]/15 hover:border-[#0F3D28]/40'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-bold">{opt.title}</span>
                    {active && (
                      <span className="px-2 py-0.5 rounded bg-[#E6A817] text-[#14221B] text-[10px] font-bold">
                        PREFERRED
                      </span>
                    )}
                  </div>
                  <p className={`text-xs mt-1 ${active ? 'text-white/80' : 'text-[#526058]'}`}>
                    {opt.desc}
                  </p>
                </button>
              );
            })}
          </section>
        )}

        {/* SECTION: RETURNS & WARRANTY */}
        {section === 'returns' && (
          <section className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <form
              onSubmit={handleSubmitReturn}
              className="lg:col-span-5 bg-white rounded-xl p-5 border border-[#0F3D28]/12 space-y-3 h-fit"
            >
              <h3 className="font-display font-bold text-base">
                7-Day Sealed Return &amp; Warranty Request
              </h3>
              <p className="text-xs text-[#526058]">
                Request instant replacement for damaged packs, electronics warranty claims, or size exchanges on apparel.
              </p>
              <input
                type="text"
                required
                value={returnOrderId}
                onChange={(e) => setReturnOrderId(e.target.value)}
                placeholder="Order ID (e.g. NGS-261118-4821)"
                className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm font-mono-num"
              />
              <select
                value={returnReason}
                onChange={(e) => setReturnReason(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm bg-white"
              >
                <option value="Sealed Pack Damaged in Transit">Sealed Pack Damaged in Transit</option>
                <option value="Wrong SKU or Quantity Received">Wrong SKU or Quantity Received</option>
                <option value="Electronics / Appliance Warranty Claim">Electronics / Appliance Warranty Claim</option>
                <option value="Apparel Size Exchange (Tags Intact)">Apparel Size Exchange (Tags Intact)</option>
              </select>
              <textarea
                rows={2}
                value={returnNotes}
                onChange={(e) => setReturnNotes(e.target.value)}
                placeholder="Optional SKU ID or notes..."
                className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm"
              />
              <button
                type="submit"
                className="w-full py-2.5 rounded-xl bg-[#0F3D28] text-white text-xs font-bold cursor-pointer"
              >
                Submit Return / Warranty Ticket
              </button>
              {returnMsg && (
                <p className="text-xs font-semibold text-[#0F3D28]">{returnMsg}</p>
              )}
            </form>

            <div className="lg:col-span-7 space-y-3">
              {returns.length === 0 ? (
                <div className="bg-white rounded-xl p-8 text-center border border-[#0F3D28]/10 text-xs sm:text-sm text-[#526058]">
                  No active return or warranty claims on this device.
                </div>
              ) : (
                returns.map((r) => (
                  <div
                    key={r.requestId}
                    className="bg-white rounded-xl p-4 border border-[#0F3D28]/12 flex items-center justify-between gap-3"
                  >
                    <div>
                      <p className="font-mono-num text-xs font-bold text-[#0F3D28]">
                        {r.requestId} · Order {r.orderId}
                      </p>
                      <p className="text-sm font-semibold text-[#14221B]">{r.reason}</p>
                      {r.notes && <p className="text-xs text-[#526058] mt-0.5">{r.notes}</p>}
                    </div>
                    <span className="px-2.5 py-1 rounded-md bg-[#E6A817]/20 text-[#14221B] text-xs font-bold shrink-0">
                      {r.status}
                    </span>
                  </div>
                ))
              )}
            </div>
          </section>
        )}
      </main>
    </div>
  );
};
