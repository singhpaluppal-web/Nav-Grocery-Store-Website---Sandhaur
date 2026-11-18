import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  CreditCard,
  IndianRupee,
  RefreshCw,
  Search,
  Save,
  ShieldCheck,
} from 'lucide-react';
import { OrderRecord, OrderStatus, PaymentMethod, PaymentSettings } from '../../types';
import {
  DEFAULT_PAYMENTS,
  fetchAllOrdersFromFirebase,
  fetchPaymentSettings,
  savePaymentSettingsToFirebase,
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

export const OrdersReconciliationPanel: React.FC<OrdersReconciliationPanelProps> = ({
  onNotify,
}) => {
  const [orders, setOrders] = useState<OrderRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [methodFilter, setMethodFilter] = useState<'all' | PaymentMethod>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [savingId, setSavingId] = useState<string | null>(null);

  const [payCfg, setPayCfg] = useState<PaymentSettings>(DEFAULT_PAYMENTS);
  const [savingCfg, setSavingCfg] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    const [ordRes, cfgRes] = await Promise.all([
      fetchAllOrdersFromFirebase(),
      fetchPaymentSettings(),
    ]);
    setOrders(ordRes.orders);
    setPayCfg(cfgRes);
    setLoading(false);
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredOrders = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return orders.filter((o) => {
      if (methodFilter !== 'all' && o.paymentMethod !== methodFilter) return false;
      if (!q) return true;
      const hay = `${o.orderId} ${o.customerName} ${o.customerPhone} ${o.utrRef || ''} ${o.razorpayPaymentId || ''}`.toLowerCase();
      return hay.includes(q);
    });
  }, [orders, methodFilter, searchQuery]);

  const metrics = useMemo(() => {
    let totalRevenue = 0;
    let utrPending = 0;
    let verifiedCount = 0;
    for (const o of orders) {
      if (o.status !== 'Cancelled') {
        totalRevenue += Number(o.totalPayable) || 0;
      }
      if (
        (o.paymentMethod === 'upi' || o.paymentMethod === 'bank_transfer') &&
        o.paymentStatus !== 'UTR Verified'
      ) {
        utrPending++;
      } else {
        verifiedCount++;
      }
    }
    return { totalRevenue, utrPending, verifiedCount };
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

  const handleSavePaymentConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingCfg(true);
    const res = await savePaymentSettingsToFirebase(payCfg);
    setSavingCfg(false);
    if (res.ok) {
      onNotify('ok', 'Saved Indian Payment Gateway settings (UPI / Razorpay / NEFT) live to Firebase RTDB.');
    } else {
      onNotify('err', 'Saved locally, but Firebase RTDB /settings/payments.json write failed.');
    }
  };

  return (
    <div className="space-y-5">
      {/* Reconciliation KPI Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">Recorded Order Volume</div>
          <div className="text-2xl font-bold text-[#0B3B24] font-mono-num mt-1 flex items-center gap-1">
            <IndianRupee className="w-5 h-5" />
            <span>{metrics.totalRevenue.toLocaleString('en-IN')}</span>
          </div>
          <div className="text-[11px] text-[#526058] mt-1">{orders.length} order(s) synced</div>
        </div>

        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">UTR Verification Queue</div>
          <div className="text-2xl font-bold text-amber-700 font-mono-num mt-1">
            {metrics.utrPending}
          </div>
          <div className="text-[11px] text-[#526058] mt-1">
            UPI / NEFT / IMPS UTRs awaiting match
          </div>
        </div>

        <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
          <div className="text-xs font-semibold text-[#526058]">Verified / COD / Gateway</div>
          <div className="text-2xl font-bold text-emerald-800 font-mono-num mt-1">
            {metrics.verifiedCount}
          </div>
          <div className="text-[11px] text-[#526058] mt-1">
            Merchant VPA: {payCfg.upiId}
          </div>
        </div>
      </div>

      {/* Live Orders & UTR Reconciliation Table */}
      <section className="bg-white rounded-xl border border-[#0B3B24]/12 overflow-hidden">
        <div className="p-4 border-b border-[#0B3B24]/10 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div>
            <h2 className="font-display font-bold text-lg text-[#0B3B24]">
              Orders &amp; Indian Payment Reconciliation
            </h2>
            <p className="text-xs text-[#526058]">
              Inspect UPI UTRs, NEFT/IMPS transfers, Razorpay IDs, and Sandhaur 148020 COD dispatch status.
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
              onClick={loadData}
              disabled={loading}
              className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 text-xs font-bold text-[#0B3B24] inline-flex items-center gap-1 cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>Sync Orders</span>
            </button>
          </div>
        </div>

        {loading ? (
          <div className="p-8 text-center text-sm text-[#526058]">
            Fetching orders from Firebase RTDB...
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="p-8 text-center text-sm text-[#526058]">
            No matching orders found. Orders submitted from the storefront checkout appear here in real time.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-[#FAF8F3] border-b border-[#0B3B24]/12 text-[11px] font-bold uppercase tracking-wider text-[#526058]">
                  <th className="py-2.5 px-3.5">Order ID &amp; Customer</th>
                  <th className="py-2.5 px-3.5">Method &amp; UTR / Payment Ref</th>
                  <th className="py-2.5 px-3.5 text-right">Amount</th>
                  <th className="py-2.5 px-3.5">Fulfillment Status</th>
                  <th className="py-2.5 px-3.5 text-right">Reconciliation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#0B3B24]/8">
                {filteredOrders.map((o) => {
                  const needsUtrConfirm =
                    (o.paymentMethod === 'upi' || o.paymentMethod === 'bank_transfer') &&
                    o.paymentStatus !== 'UTR Verified';
                  return (
                    <tr key={o.orderId} className="hover:bg-[#FAF8F3]/60">
                      <td className="py-3 px-3.5">
                        <div className="font-mono-num font-bold text-[#0B3B24]">{o.orderId}</div>
                        <div className="font-semibold text-[#14221B] mt-0.5">
                          {o.customerName} · +91 {o.customerPhone}
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

                      <td className="py-3 px-3.5">
                        <select
                          aria-label={`Order status for ${o.orderId}`}
                          value={o.status}
                          disabled={savingId === o.orderId}
                          onChange={(e) =>
                            handleUpdateOrder(
                              o.orderId,
                              { status: e.target.value },
                              `${o.orderId}: Fulfillment updated to "${e.target.value}".`
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
