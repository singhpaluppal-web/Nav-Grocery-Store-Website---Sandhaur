import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  CreditCard,
  Globe,
  IndianRupee,
  RefreshCw,
  Save,
  Search,
  ShieldCheck,
  Smartphone,
  Store,
  Truck,
} from 'lucide-react';
import {
  OrderRecord,
  OrderStatus,
  PaymentMethod,
  PaymentSettings,
  StoreConfig,
} from '../../types';
import {
  DEFAULT_PAYMENTS,
  DEFAULT_STORE_CONFIG,
  fetchAllOrdersFromFirebase,
  fetchPaymentSettings,
  savePaymentSettingsToFirebase,
  saveStoreConfigToFirebase,
  subscribeToAllOrders,
  subscribeToStoreConfig,
  updateOrderInFirebase,
} from '../../utils/firebase';

interface OrdersReconciliationPanelProps {
  onNotify: (type: 'ok' | 'err', msg: string) => void;
}

const ORDER_STATUSES: OrderStatus[] = [
  'Pending',
  'Confirmed',
  'Packed',
  'Out for Delivery',
  'Ready for Pickup',
  'Delivered',
  'Picked Up',
  'Cancelled',
];

const QUICK_STAGE_FLOW: OrderStatus[] = [
  'Confirmed',
  'Packed',
  'Out for Delivery',
  'Delivered',
];

export const OrdersReconciliationPanel: React.FC<OrdersReconciliationPanelProps> = ({
  onNotify,
}) => {
  const [orders, setOrders] = useState<OrderRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [liveConnected, setLiveConnected] = useState(true);
  const [sourceFilter, setSourceFilter] = useState<'all' | 'web' | 'android'>('all');
  const [methodFilter, setMethodFilter] = useState<'all' | PaymentMethod>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [savingId, setSavingId] = useState<string | null>(null);

  const [storeConfig, setStoreConfig] = useState<StoreConfig>(DEFAULT_STORE_CONFIG);
  const [savingStoreCfg, setSavingStoreCfg] = useState(false);

  const [payCfg, setPayCfg] = useState<PaymentSettings>(DEFAULT_PAYMENTS);
  const [savingCfg, setSavingCfg] = useState(false);

  // Live RTDB subscriptions to /orders and /storeConfig (syncs Web + Navkart Android in real time)
  useEffect(() => {
    let active = true;
    setLoading(true);

    const unsubOrders = subscribeToAllOrders((liveOrders, connected) => {
      if (!active) return;
      setOrders(liveOrders);
      setLiveConnected(connected);
      setLoading(false);
    });

    const unsubStoreCfg = subscribeToStoreConfig((liveCfg) => {
      if (!active) return;
      setStoreConfig(liveCfg);
    });

    void fetchPaymentSettings().then((cfgRes) => {
      if (active) setPayCfg(cfgRes);
    });

    return () => {
      active = false;
      unsubOrders();
      unsubStoreCfg();
    };
  }, []);

  const handleManualSync = async () => {
    setLoading(true);
    const [ordRes, cfgRes] = await Promise.all([
      fetchAllOrdersFromFirebase(),
      fetchPaymentSettings(),
    ]);
    setOrders(ordRes.orders);
    setLiveConnected(ordRes.connected);
    setPayCfg(cfgRes);
    setLoading(false);
  };

  const filteredOrders = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return orders.filter((o) => {
      const normalizedSource = o.source === 'android' ? 'android' : 'web';
      if (sourceFilter !== 'all' && normalizedSource !== sourceFilter) return false;
      if (methodFilter !== 'all' && o.paymentMethod !== methodFilter) return false;
      if (!q) return true;
      const hay = `${o.orderId} ${o.customerName} ${o.customerPhone} ${o.utrRef || ''} ${o.razorpayPaymentId || ''} ${normalizedSource}`.toLowerCase();
      return hay.includes(q);
    });
  }, [orders, sourceFilter, methodFilter, searchQuery]);

  const metrics = useMemo(() => {
    let totalRevenue = 0;
    let utrPending = 0;
    let webOrders = 0;
    let androidOrders = 0;
    for (const o of orders) {
      if (o.status !== 'Cancelled') {
        totalRevenue += Number(o.totalPayable) || 0;
      }
      if (
        (o.paymentMethod === 'upi' || o.paymentMethod === 'bank_transfer') &&
        o.paymentStatus !== 'UTR Verified'
      ) {
        utrPending++;
      }
      if (o.source === 'android') androidOrders++;
      else webOrders++;
    }
    return { totalRevenue, utrPending, webOrders, androidOrders };
  }, [orders]);

  const handleUpdateOrder = async (
    orderId: string,
    patch: Partial<OrderRecord>,
    successMsg: string
  ) => {
    setSavingId(orderId);
    const res = await updateOrderInFirebase(orderId, patch);
    if (res.ok) {
      setOrders((prev) =>
        prev.map((o) => (o.orderId === orderId ? { ...o, ...patch } : o))
      );
      onNotify('ok', successMsg);
    } else {
      onNotify('err', `Could not sync order ${orderId} to Firebase RTDB.`);
    }
    setSavingId(null);
  };

  const handleSaveStoreConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingStoreCfg(true);
    const res = await saveStoreConfigToFirebase(storeConfig);
    setSavingStoreCfg(false);
    if (res.ok) {
      onNotify(
        'ok',
        'Published /storeConfig live to Firebase RTDB — synced with www.navgrocerystore.com and Navkart Android.'
      );
    } else {
      onNotify('err', 'Saved locally, but Firebase RTDB /storeConfig write failed.');
    }
  };

  const handleSavePaymentConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingCfg(true);
    const res = await savePaymentSettingsToFirebase({
      ...payCfg,
      deliveryFee: storeConfig.deliveryFee,
      freeDeliveryAbove: storeConfig.freeDeliveryAbove,
      codMinOrder: storeConfig.codMinOrder,
    });
    setSavingCfg(false);
    if (res.ok) {
      onNotify(
        'ok',
        'Saved Indian Payment Gateway settings (UPI / Razorpay / NEFT) live to Firebase RTDB.'
      );
    } else {
      onNotify('err', 'Saved locally, but Firebase RTDB /settings/payments.json write failed.');
    }
  };

  return (
    <div className="space-y-5">
      {/* Unified Queue KPI Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-3.5">
        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">Unified Order Volume</div>
          <div className="text-2xl font-bold text-[#0B3B24] font-mono-num mt-1 flex items-center gap-1">
            <IndianRupee className="w-5 h-5" />
            <span>{metrics.totalRevenue.toLocaleString('en-IN')}</span>
          </div>
          <div className="text-[11px] text-[#526058] mt-1">
            {orders.length} total order(s) {liveConnected ? '· Live RTDB' : '· Cached'}
          </div>
        </div>

        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">Website Orders</div>
          <div className="text-2xl font-bold text-[#0B3B24] font-mono-num mt-1">
            {metrics.webOrders}
          </div>
          <div className="text-[11px] text-[#526058] mt-1">www.navgrocerystore.com</div>
        </div>

        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">Navkart Android Orders</div>
          <div className="text-2xl font-bold text-emerald-800 font-mono-num mt-1">
            {metrics.androidOrders}
          </div>
          <div className="text-[11px] text-[#526058] mt-1">Real-time /orders sync</div>
        </div>

        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">UTR Verification Queue</div>
          <div className="text-2xl font-bold text-amber-700 font-mono-num mt-1">
            {metrics.utrPending}
          </div>
          <div className="text-[11px] text-[#526058] mt-1">
            Merchant VPA: {payCfg.upiId}
          </div>
        </div>
      </div>

      {/* Live Unified Orders Queue (Web + Navkart Android App) */}
      <section className="bg-white rounded-xl border border-[#0B3B24]/12 overflow-hidden">
        <div className="p-4 border-b border-[#0B3B24]/10 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div>
            <h2 className="font-display font-bold text-lg text-[#0B3B24]">
              Live Unified Orders Queue (Website + Navkart Android)
            </h2>
            <p className="text-xs text-[#526058]">
              Listening live to /orders in Firebase RTDB. Status updates sync instantly to customers on web and Android.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Order ID, Phone, or UTR..."
                className="pl-8 pr-3 py-1.5 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] text-xs font-mono-num focus:outline-none"
              />
            </div>

            <select
              aria-label="Filter by order source"
              value={sourceFilter}
              onChange={(e) => setSourceFilter(e.target.value as 'all' | 'web' | 'android')}
              className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 bg-white text-xs font-semibold"
            >
              <option value="all">All Channels (Web + Android)</option>
              <option value="web">Website Only</option>
              <option value="android">Navkart Android Only</option>
            </select>

            <select
              aria-label="Filter by payment method"
              value={methodFilter}
              onChange={(e) => setMethodFilter(e.target.value as 'all' | PaymentMethod)}
              className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 bg-white text-xs font-semibold"
            >
              <option value="all">All Payment Modes</option>
              <option value="upi">UPI QR / App Intent</option>
              <option value="razorpay">Razorpay Gateway</option>
              <option value="bank_transfer">NEFT / IMPS Bank Transfer</option>
              <option value="cod">Cash on Delivery (COD)</option>
            </select>

            <button
              type="button"
              onClick={handleManualSync}
              disabled={loading}
              className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 text-xs font-bold text-[#0B3B24] inline-flex items-center gap-1 cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>
        </div>

        {loading ? (
          <div className="p-8 text-center text-sm text-[#526058]">
            Connecting live listener to Firebase RTDB /orders...
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="p-8 text-center text-sm text-[#526058]">
            No matching orders yet. Orders placed on www.navgrocerystore.com or the Navkart Android app appear here automatically in real time.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-[#FAF8F3] border-b border-[#0B3B24]/12 text-[11px] font-bold uppercase tracking-wider text-[#526058]">
                  <th className="py-2.5 px-3.5">Order ID, Channel &amp; Customer</th>
                  <th className="py-2.5 px-3.5">Method &amp; UTR / Payment Ref</th>
                  <th className="py-2.5 px-3.5 text-right">Amount</th>
                  <th className="py-2.5 px-3.5">Live Fulfillment Status</th>
                  <th className="py-2.5 px-3.5 text-right">Reconciliation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#0B3B24]/8">
                {filteredOrders.map((o) => {
                  const needsUtrConfirm =
                    (o.paymentMethod === 'upi' || o.paymentMethod === 'bank_transfer') &&
                    o.paymentStatus !== 'UTR Verified';
                  const isAndroid = o.source === 'android';
                  return (
                    <tr key={o.orderId} className="hover:bg-[#FAF8F3]/60">
                      <td className="py-3 px-3.5">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <span className="font-mono-num font-bold text-[#0B3B24]">{o.orderId}</span>
                          <span
                            className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold ${
                              isAndroid
                                ? 'bg-emerald-100 text-emerald-900'
                                : 'bg-[#0B3B24]/10 text-[#0B3B24]'
                            }`}
                          >
                            {isAndroid ? (
                              <Smartphone className="w-3 h-3" />
                            ) : (
                              <Globe className="w-3 h-3" />
                            )}
                            <span>{isAndroid ? 'Navkart Android' : 'Web'}</span>
                          </span>
                        </div>
                        <div className="font-semibold text-[#14221B] mt-0.5">
                          {o.customerName}
                          {o.customerPhone ? ` · +91 ${o.customerPhone}` : ''}
                        </div>
                        <div className="text-[11px] text-[#526058] truncate max-w-xs">
                          {o.address}
                        </div>
                      </td>

                      <td className="py-3 px-3.5">
                        <div className="font-bold uppercase text-[#14221B]">
                          {o.paymentMethod} ·{' '}
                          <span
                            className={
                              o.paymentStatus === 'UTR Verified' || o.paymentStatus === 'Gateway Paid'
                                ? 'text-emerald-800'
                                : 'text-amber-800'
                            }
                          >
                            {o.paymentStatus}
                          </span>
                        </div>
                        {o.utrRef && (
                          <div className="font-mono-num text-[11px] text-[#526058] mt-0.5">
                            UTR: {o.utrRef}
                          </div>
                        )}
                        {o.razorpayPaymentId && (
                          <div className="font-mono-num text-[11px] text-[#526058] mt-0.5">
                            RZP: {o.razorpayPaymentId}
                          </div>
                        )}
                      </td>

                      <td className="py-3 px-3.5 text-right font-mono-num whitespace-nowrap">
                        <div className="font-bold text-sm text-[#0B3B24]">₹{o.totalPayable}</div>
                        <div className="text-[11px] text-[#526058]">{o.items.length} SKU(s)</div>
                      </td>

                      <td className="py-3 px-3.5 space-y-1.5">
                        <select
                          aria-label={`Order status for ${o.orderId}`}
                          value={o.status}
                          disabled={savingId === o.orderId}
                          onChange={(e) =>
                            handleUpdateOrder(
                              o.orderId,
                              { status: e.target.value },
                              `${o.orderId}: Status synced live to "${e.target.value}".`
                            )
                          }
                          className="px-2.5 py-1.5 rounded-lg border border-[#0B3B24]/25 bg-white font-semibold text-xs"
                        >
                          {ORDER_STATUSES.map((st) => (
                            <option key={st} value={st}>
                              {st}
                            </option>
                          ))}
                        </select>
                        <div className="flex flex-wrap gap-1">
                          {QUICK_STAGE_FLOW.map((stage) => {
                            const active = o.status === stage;
                            return (
                              <button
                                key={stage}
                                type="button"
                                disabled={savingId === o.orderId || active}
                                onClick={() =>
                                  handleUpdateOrder(
                                    o.orderId,
                                    { status: stage },
                                    `${o.orderId}: Updated live to "${stage}".`
                                  )
                                }
                                className={`px-2 py-0.5 rounded text-[10px] font-bold transition-colors cursor-pointer ${
                                  active
                                    ? 'bg-[#0B3B24] text-[#D4AF37]'
                                    : 'bg-[#FAF8F3] text-[#0B3B24] border border-[#0B3B24]/15 hover:bg-[#0B3B24] hover:text-white'
                                }`}
                              >
                                {stage}
                              </button>
                            );
                          })}
                        </div>
                      </td>

                      <td className="py-3 px-3.5 text-right whitespace-nowrap">
                        {needsUtrConfirm ? (
                          <button
                            type="button"
                            disabled={savingId === o.orderId}
                            onClick={() =>
                              handleUpdateOrder(
                                o.orderId,
                                {
                                  paymentStatus: 'UTR Verified',
                                  status: o.status === 'Pending' ? 'Confirmed' : o.status,
                                },
                                `${o.orderId}: Verified UTR ${o.utrRef || ''} and confirmed order.`
                              )
                            }
                            className="px-3 py-1.5 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold inline-flex items-center gap-1 cursor-pointer disabled:opacity-50"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5 text-[#D4AF37]" />
                            <span>Verify UTR</span>
                          </button>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-emerald-800 font-bold">
                            <ShieldCheck className="w-3.5 h-3.5" />
                            <span>Reconciled</span>
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Live /storeConfig Editor (Synced across Website + Navkart Android) */}
      <form
        onSubmit={handleSaveStoreConfig}
        className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 space-y-4"
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#0B3B24]/10 pb-3">
          <div className="flex items-center gap-2">
            <Store className="w-5 h-5 text-[#0B3B24]" />
            <div>
              <h3 className="font-display font-bold text-base text-[#0B3B24]">
                Store Status, Delivery Rules &amp; Announcements (/storeConfig)
              </h3>
              <p className="text-xs text-[#526058]">
                Syncs open/close state, delivery thresholds, and live banners instantaneously to www.navgrocerystore.com and Navkart Android.
              </p>
            </div>
          </div>
          <button
            type="submit"
            disabled={savingStoreCfg}
            className="px-4 py-2 rounded-lg bg-[#D4AF37] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1.5 hover:bg-[#e0bc48] cursor-pointer disabled:opacity-50 shrink-0"
          >
            <Save className="w-3.5 h-3.5" />
            <span>{savingStoreCfg ? 'Publishing...' : 'Publish /storeConfig Live'}</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5 text-xs">
          <label className="flex items-center justify-between gap-3 p-3 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/15 cursor-pointer">
            <div>
              <span className="block font-bold text-[#0B3B24]">Store Open Status</span>
              <span className="text-[11px] text-[#526058]">
                {storeConfig.isOpen
                  ? 'Accepting immediate Sandhaur dispatch'
                  : 'Marked closed — shows next-hours notice'}
              </span>
            </div>
            <input
              type="checkbox"
              checked={storeConfig.isOpen}
              onChange={(e) => setStoreConfig({ ...storeConfig, isOpen: e.target.checked })}
              className="w-4 h-4 accent-[#0B3B24] cursor-pointer"
            />
          </label>

          <div>
            <label className="block font-bold text-[#526058] mb-1">Store Hours</label>
            <input
              type="text"
              value={storeConfig.hoursLabel}
              onChange={(e) => setStoreConfig({ ...storeConfig, hoursLabel: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20"
            />
          </div>

          <div className="flex items-end gap-2">
            <label className="flex-1 flex items-center justify-between gap-2 p-2.5 rounded-lg bg-[#FAF8F3] border border-[#0B3B24]/15 cursor-pointer">
              <span className="font-semibold text-[#14221B] inline-flex items-center gap-1">
                <Truck className="w-3.5 h-3.5 text-[#0B3B24]" />
                <span>15-Min Express</span>
              </span>
              <input
                type="checkbox"
                checked={storeConfig.expressDeliveryEnabled}
                onChange={(e) =>
                  setStoreConfig({ ...storeConfig, expressDeliveryEnabled: e.target.checked })
                }
                className="accent-[#0B3B24]"
              />
            </label>
            <label className="flex-1 flex items-center justify-between gap-2 p-2.5 rounded-lg bg-[#FAF8F3] border border-[#0B3B24]/15 cursor-pointer">
              <span className="font-semibold text-[#14221B]">Store Pickup</span>
              <input
                type="checkbox"
                checked={storeConfig.pickupEnabled}
                onChange={(e) =>
                  setStoreConfig({ ...storeConfig, pickupEnabled: e.target.checked })
                }
                className="accent-[#0B3B24]"
              />
            </label>
          </div>

          <div className="md:col-span-3">
            <label className="block font-bold text-[#526058] mb-1">
              Live Top Announcement Banner (Strictly Sealed Groceries &amp; -18°C Frozen)
            </label>
            <input
              type="text"
              value={storeConfig.announcementText}
              onChange={(e) =>
                setStoreConfig({ ...storeConfig, announcementText: e.target.value })
              }
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20"
            />
          </div>

          <div>
            <label className="block font-bold text-[#526058] mb-1">Local Delivery Fee (₹)</label>
            <input
              type="number"
              min={0}
              value={storeConfig.deliveryFee}
              onChange={(e) =>
                setStoreConfig({ ...storeConfig, deliveryFee: Math.max(0, Number(e.target.value) || 0) })
              }
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>

          <div>
            <label className="block font-bold text-[#526058] mb-1">Free Delivery Above (₹)</label>
            <input
              type="number"
              min={0}
              value={storeConfig.freeDeliveryAbove}
              onChange={(e) =>
                setStoreConfig({
                  ...storeConfig,
                  freeDeliveryAbove: Math.max(0, Number(e.target.value) || 0),
                })
              }
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>

          <div>
            <label className="block font-bold text-[#526058] mb-1">COD Minimum Order (₹)</label>
            <input
              type="number"
              min={0}
              value={storeConfig.codMinOrder}
              onChange={(e) =>
                setStoreConfig({
                  ...storeConfig,
                  codMinOrder: Math.max(0, Number(e.target.value) || 0),
                })
              }
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>
        </div>
      </form>

      {/* Payment Gateway Configuration Form */}
      <form
        onSubmit={handleSavePaymentConfig}
        className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 space-y-4"
      >
        <div className="flex items-center justify-between gap-3 border-b border-[#0B3B24]/10 pb-3">
          <div className="flex items-center gap-2">
            <CreditCard className="w-5 h-5 text-[#0B3B24]" />
            <div>
              <h3 className="font-display font-bold text-base text-[#0B3B24]">
                Indian Payment Gateway &amp; Bank Config (Synced to Storefront)
              </h3>
              <p className="text-xs text-[#526058]">
                Manage Paytm UPI VPA, Razorpay Key ID, NEFT/IMPS bank details &amp; COD rules.
              </p>
            </div>
          </div>
          <button
            type="submit"
            disabled={savingCfg}
            className="px-4 py-2 rounded-lg bg-[#0B3B24] text-white text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <Save className="w-3.5 h-3.5 text-[#D4AF37]" />
            <span>{savingCfg ? 'Saving...' : 'Save Payment Settings'}</span>
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
          <div>
            <label className="block font-bold text-[#526058] mb-1">Merchant UPI ID (VPA)</label>
            <input
              type="text"
              value={payCfg.upiId}
              onChange={(e) => setPayCfg({ ...payCfg, upiId: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>
          <div>
            <label className="block font-bold text-[#526058] mb-1">UPI Payee Name</label>
            <input
              type="text"
              value={payCfg.upiMerchantName}
              onChange={(e) => setPayCfg({ ...payCfg, upiMerchantName: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20"
            />
          </div>
          <div>
            <label className="block font-bold text-[#526058] mb-1">Razorpay Live Key ID (Optional)</label>
            <input
              type="text"
              value={payCfg.razorpayKeyId}
              onChange={(e) => setPayCfg({ ...payCfg, razorpayKeyId: e.target.value })}
              placeholder="rzp_live_..."
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>
          <div>
            <label className="block font-bold text-[#526058] mb-1">Bank Name &amp; Branch</label>
            <input
              type="text"
              value={payCfg.bankName || ''}
              onChange={(e) => setPayCfg({ ...payCfg, bankName: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20"
            />
          </div>
          <div>
            <label className="block font-bold text-[#526058] mb-1">NEFT/IMPS Account Number</label>
            <input
              type="text"
              value={payCfg.bankAccountNumber || ''}
              onChange={(e) => setPayCfg({ ...payCfg, bankAccountNumber: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>
          <div>
            <label className="block font-bold text-[#526058] mb-1">IFSC Code</label>
            <input
              type="text"
              value={payCfg.bankIfsc || ''}
              onChange={(e) => setPayCfg({ ...payCfg, bankIfsc: e.target.value })}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 font-mono-num"
            />
          </div>
        </div>
      </form>
    </div>
  );
};
