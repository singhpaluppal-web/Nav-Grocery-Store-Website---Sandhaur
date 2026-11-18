import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  Heart,
  MapPin,
  PackageCheck,
  Plus,
  RotateCcw,
  ShieldCheck,
  ShoppingBasket,
  Trash2,
  UserCheck,
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
  fetchOrderById,
  getLocalOrders,
  getLocalReturnRequests,
  submitReturnRequest,
} from '../utils/firebase';
import { resolveProductImage, getNextFallbackImage } from '../utils/brandPhotos';
import { StoreLang } from '../utils/i18n';
import { LanguageSwitcher } from './LanguageSwitcher';
import { StoreLogo } from './StoreLogo';

interface AccountPageViewProps {
  logoUrl: string;
  products: Product[];
  favoriteIds: string[];
  customerSession: CustomerSession | null;
  paymentSettings: PaymentSettings;
  lang: StoreLang;
  onChangeLang: (lang: StoreLang) => void;
  onUpdateSession: (session: CustomerSession | null) => void;
  onToggleFavorite: (productId: string) => void;
  onAddToCart: (product: Product) => void;
  onNavigateHome: () => void;
  onOpenWhatsAppAuth: () => void;
}

type AccountSection = 'orders' | 'wishlist' | 'addresses' | 'payments' | 'returns';

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
  onChangeLang,
  onUpdateSession,
  onToggleFavorite,
  onAddToCart,
  onNavigateHome,
  onOpenWhatsAppAuth,
}) => {
  const [section, setSection] = useState<AccountSection>('orders');
  const [orders, setOrders] = useState<OrderRecord[]>(() => getLocalOrders());
  const [lookupId, setLookupId] = useState('');
  const [lookupBusy, setLookupBusy] = useState(false);
  const [lookupStatus, setLookupStatus] = useState('');

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

  useEffect(() => {
    setOrders(getLocalOrders());
  }, []);

  const favoriteSet = useMemo(() => new Set(favoriteIds), [favoriteIds]);
  const wishlistProducts = useMemo(
    () => products.filter((p) => favoriteSet.has(p.id)),
    [products, favoriteSet]
  );

  const persistAddresses = (next: SavedAddress[]) => {
    setAddresses(next);
    if (customerSession) {
      const updated: CustomerSession = {
        ...customerSession,
        address: next.find((a) => a.isDefault)?.line1 || customerSession.address,
        savedAddresses: next,
      };
      localStorage.setItem('nav_customer_session_v2', JSON.stringify(updated));
      onUpdateSession(updated);
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
      };
      localStorage.setItem('nav_customer_session_v2', JSON.stringify(updated));
      onUpdateSession(updated);
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
    setReturnMsg(`Claim ${req.requestId} submitted for order ${req.orderId}. Our Sandhaur support desk will follow up on WhatsApp.`);
  };

  const navTabs: Array<{ id: AccountSection; label: string; icon: React.ReactNode }> = [
    { id: 'orders', label: lang === 'pa' ? 'ਆਰਡਰ ਤੇ ਟ੍ਰੈਕਿੰਗ' : 'Orders & Tracking', icon: <PackageCheck className="w-4 h-4" /> },
    { id: 'wishlist', label: `${lang === 'pa' ? 'ਪਸੰਦੀਦਾ' : 'Wishlist'} (${wishlistProducts.length})`, icon: <Heart className="w-4 h-4" /> },
    { id: 'addresses', label: lang === 'pa' ? 'ਸੇਵ ਪਤੇ' : 'Saved Addresses', icon: <MapPin className="w-4 h-4" /> },
    { id: 'payments', label: lang === 'pa' ? 'ਭੁਗਤਾਨ ਢੰਗ' : 'Payment Methods', icon: <CreditCard className="w-4 h-4" /> },
    { id: 'returns', label: lang === 'pa' ? 'ਵਾਪਸੀ ਤੇ ਵਾਰੰਟੀ' : 'Returns & Warranty', icon: <RotateCcw className="w-4 h-4" /> },
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
                <p className="text-[11px] text-[#E6A817]">Nav Grocery &amp; Nav Kart · 148020</p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <LanguageSwitcher lang={lang} onChange={onChangeLang} />
            <button
              type="button"
              onClick={onOpenWhatsAppAuth}
              className="px-3 py-2 rounded-lg bg-[#E6A817] text-[#14221B] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
            >
              <UserCheck className="w-3.5 h-3.5" />
              <span>{customerSession ? 'Switch Profile' : 'WhatsApp Sign In'}</span>
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-6">
        {/* Profile Membership Banner */}
        <section className="bg-[#0F3D28] text-white rounded-2xl p-6 border border-[#E6A817]/30 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#E6A817]">
              <ShieldCheck className="w-4 h-4" />
              <span>Nav Pass Wholesale Membership · Active</span>
            </div>
            <h1 className="font-display text-2xl font-bold">
              {customerSession ? customerSession.name : 'Welcome, Sandhaur Shopper'}
            </h1>
            <p className="text-xs sm:text-sm text-white/80">
              {customerSession
                ? `WhatsApp Verified: +91 ${customerSession.phone} · ${customerSession.address}`
                : 'Sign in with WhatsApp OTP to sync your saved Sandhaur 148020 addresses & wholesale invoices.'}
            </p>
          </div>

          <div className="grid grid-cols-3 gap-3 shrink-0 text-center bg-[#0A2A1B] p-3.5 rounded-xl border border-white/10">
            <div>
              <p className="text-[11px] text-white/70">Wholesale Tier</p>
              <p className="font-bold text-sm text-[#E6A817]">Flat 12% Off</p>
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

        {/* SECTION: ORDERS & TRACKING */}
        {section === 'orders' && (
          <section className="space-y-4">
            <form
              onSubmit={handleLookupOrder}
              className="bg-white rounded-xl p-4 border border-[#0F3D28]/12 flex flex-col sm:flex-row gap-2.5"
            >
              <input
                type="text"
                value={lookupId}
                onChange={(e) => setLookupId(e.target.value)}
                placeholder="Track any Order ID from Firebase RTDB (e.g. NGS-261116-4821)"
                className="flex-1 px-3.5 py-2.5 rounded-lg border border-[#0F3D28]/20 text-xs sm:text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
              />
              <button
                type="submit"
                disabled={lookupBusy}
                className="px-5 py-2.5 rounded-lg bg-[#0F3D28] text-white text-xs font-bold whitespace-nowrap cursor-pointer disabled:opacity-50"
              >
                {lookupBusy ? 'Syncing...' : 'Refresh Order Status'}
              </button>
            </form>
            {lookupStatus && (
              <p className="text-xs font-semibold text-[#0F3D28] px-1">{lookupStatus}</p>
            )}

            {orders.length === 0 ? (
              <div className="bg-white rounded-2xl p-10 text-center border border-[#0F3D28]/10 space-y-3">
                <p className="font-display text-lg font-bold">No orders placed on this device yet</p>
                <p className="text-xs sm:text-sm text-[#526058]">
                  Every basket checkout generates a live GST bill synced to Firebase RTDB with Sandhaur 148020 delivery or 30-min store pickup.
                </p>
                <button
                  type="button"
                  onClick={onNavigateHome}
                  className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-xs font-bold cursor-pointer"
                >
                  Browse 320+ Sealed Items
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {orders.map((order) => (
                  <div
                    key={order.orderId}
                    className="bg-white rounded-xl p-4 sm:p-5 border border-[#0F3D28]/12 space-y-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#0F3D28]/10 pb-3">
                      <div>
                        <p className="font-mono-num font-bold text-sm text-[#0F3D28]">
                          {order.orderId}
                        </p>
                        <p className="text-xs text-[#526058]">
                          {new Date(order.createdAt).toLocaleString()} · {order.slot}
                        </p>
                      </div>
                      <div className="flex items-center gap-2 text-xs">
                        <span className="px-2.5 py-1 rounded-md bg-[#E6A817]/20 text-[#14221B] font-bold">
                          {order.status}
                        </span>
                        <span className="font-mono-num font-bold text-sm text-[#0F3D28]">
                          ₹{order.totalPayable}
                        </span>
                      </div>
                    </div>

                    <div className="text-xs text-[#526058] flex flex-wrap justify-between gap-2">
                      <span>
                        Payment: <strong className="text-[#14221B]">{order.paymentMethod.toUpperCase()}</strong> ({order.paymentStatus})
                        {order.utrRef ? ` · UTR: ${order.utrRef}` : ''}
                      </span>
                      <span className="text-[#0F3D28] font-semibold">
                        12% Savings: ₹{order.savings}
                      </span>
                    </div>

                    <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                      <p className="text-xs text-[#14221B]">
                        {order.items.map((it) => `${it.name} ×${it.qty}`).join(', ')}
                      </p>
                      <button
                        type="button"
                        onClick={() => {
                          order.items.forEach((it) => {
                            const match = products.find((p) => p.id === it.id);
                            if (match) onAddToCart(match);
                          });
                          onNavigateHome();
                        }}
                        className="px-3 py-1.5 rounded-lg bg-[#0F3D28] text-white text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer shrink-0"
                      >
                        <ShoppingBasket className="w-3.5 h-3.5 text-[#E6A817]" />
                        <span>Reorder Items</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
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
                          onClick={() => onAddToCart(item)}
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
                placeholder="Order ID (e.g. NGS-261116-4821)"
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
