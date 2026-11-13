import React, { useCallback, useEffect, useState } from 'react';
import {
  X,
  Search,
  Truck,
  Store,
  PackageCheck,
  CheckCircle2,
  Clock,
  RefreshCw,
  MessageCircle,
  AlertCircle,
  MapPin,
} from 'lucide-react';
import { OrderRecord } from '../types';
import { fetchOrderById, getLocalOrders } from '../utils/firebase';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface OrderTrackerModalProps {
  isOpen: boolean;
  initialOrderId?: string;
  lang?: StoreLang;
  onClose: () => void;
}

interface TrackingStep {
  key: string;
  label: string;
  description: string;
}

function getTrackingSteps(fulfillment: OrderRecord['fulfillment']): TrackingStep[] {
  if (fulfillment === 'pickup') {
    return [
      {
        key: 'placed',
        label: 'Order Placed',
        description: 'Order logged & verified at Nav Grocery Store, Main Bazaar.',
      },
      {
        key: 'packed',
        label: 'Packed & Sealed',
        description: 'Sealed groceries packed; -18°C frozen packs held in cold storage.',
      },
      {
        key: 'ready',
        label: 'Ready for Pickup',
        description: 'Ready at Main Bazaar Sandhaur Express Pickup Counter.',
      },
      {
        key: 'completed',
        label: 'Picked Up',
        description: 'Order collected by customer.',
      },
    ];
  }

  return [
    {
      key: 'placed',
      label: 'Order Placed',
      description: 'Order logged & verified at Nav Grocery Store, Main Bazaar.',
    },
    {
      key: 'packed',
      label: 'Packed & Sealed',
      description: 'Sealed groceries & -18°C cold-chain packs prepared for dispatch.',
    },
    {
      key: 'transit',
      label: 'Out for Delivery',
      description: 'Local Sandhaur delivery rider is on the way to your address.',
    },
    {
      key: 'completed',
      label: 'Delivered',
      description: 'Order handed over safely at your doorstep.',
    },
  ];
}

function resolveStatusStage(statusRaw: string): {
  stepIndex: number;
  cancelled: boolean;
  displayLabel: string;
} {
  const s = (statusRaw || 'Pending').trim().toLowerCase();
  if (s.includes('cancel')) {
    return { stepIndex: -1, cancelled: true, displayLabel: 'Cancelled' };
  }
  if (s.includes('deliver') && !s.includes('out')) {
    return { stepIndex: 3, cancelled: false, displayLabel: statusRaw || 'Delivered' };
  }
  if (s.includes('picked') || s.includes('complete') || s.includes('collect')) {
    return { stepIndex: 3, cancelled: false, displayLabel: statusRaw || 'Completed' };
  }
  if (s.includes('out') || s.includes('dispatch') || s.includes('ready') || s.includes('transit')) {
    return { stepIndex: 2, cancelled: false, displayLabel: statusRaw };
  }
  if (s.includes('pack') || s.includes('prepar') || s.includes('process')) {
    return { stepIndex: 1, cancelled: false, displayLabel: statusRaw };
  }
  return { stepIndex: 0, cancelled: false, displayLabel: statusRaw || 'Pending' };
}

function formatOrderDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

export const OrderTrackerModal: React.FC<OrderTrackerModalProps> = ({
  isOpen,
  initialOrderId = '',
  lang = 'en',
  onClose,
}) => {
  const t = getStoreTranslations(lang);
  const [queryId, setQueryId] = useState<string>(initialOrderId);
  const [recentOrders, setRecentOrders] = useState<OrderRecord[]>([]);
  const [activeOrder, setActiveOrder] = useState<OrderRecord | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string>('');
  const [searchedOnce, setSearchedOnce] = useState<boolean>(false);

  const performLookup = useCallback(async (orderIdInput: string) => {
    const clean = orderIdInput.trim().toUpperCase();
    if (!clean) {
      setErrorMsg('Please enter your Order ID (for example: NGS-261113-4821).');
      return;
    }

    setQueryId(clean);
    setLoading(true);
    setErrorMsg('');
    setSearchedOnce(true);

    const found = await fetchOrderById(clean);
    setLoading(false);
    setRecentOrders(getLocalOrders().slice(0, 4));

    if (!found) {
      setActiveOrder(null);
      setErrorMsg(
        `No order found matching "${clean}". Check the Order ID on your digital bill or WhatsApp message, or call +91 8847370905.`
      );
      return;
    }

    setActiveOrder(found);
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    const localList = getLocalOrders().slice(0, 4);
    setRecentOrders(localList);

    if (initialOrderId && initialOrderId.trim()) {
      setQueryId(initialOrderId.trim().toUpperCase());
      performLookup(initialOrderId);
    } else {
      setErrorMsg('');
    }
  }, [isOpen, initialOrderId, performLookup]);

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const steps = activeOrder ? getTrackingSteps(activeOrder.fulfillment) : [];
  const stage = activeOrder ? resolveStatusStage(activeOrder.status) : null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    performLookup(queryId);
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Order Status Tracker"
    >
      <div
        className="bg-[#F8F7F2] rounded-2xl max-w-2xl w-full max-h-[92vh] flex flex-col overflow-hidden shadow-2xl border border-[#0F3D28]/15"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Modal Header */}
        <div className="px-5 py-4 bg-[#0F3D28] text-white flex items-center justify-between gap-3 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#E6A817] text-[#14221B] flex items-center justify-center shrink-0">
              <PackageCheck className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-display font-bold text-lg leading-tight">{t.trackerTitle}</h2>
              <p className="text-xs text-white/75">
                {t.trackerSubtitle}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close Order Status Tracker"
            className="p-1.5 rounded-lg hover:bg-white/15 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5">
          {/* Search Form */}
          <form
            onSubmit={handleSubmit}
            className="bg-white rounded-xl p-4 border border-[#0F3D28]/12 space-y-3"
          >
            <label
              htmlFor="order-tracker-input"
              className="block text-xs font-bold uppercase tracking-wider text-[#526058]"
            >
              {t.trackerInputLabel}
            </label>
            <div className="flex flex-col sm:flex-row gap-2.5">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-[#526058] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  id="order-tracker-input"
                  type="text"
                  value={queryId}
                  onChange={(e) => setQueryId(e.target.value)}
                  placeholder="e.g. NGS-261113-4821"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 bg-[#F8F7F2] font-mono-num text-sm text-[#14221B] placeholder:text-[#526058]/70 focus:outline-none focus:border-[#0F3D28] focus:bg-white uppercase"
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white font-bold text-sm hover:bg-[#0A2A1B] transition-colors cursor-pointer whitespace-nowrap disabled:opacity-60"
              >
                {loading ? t.trackerChecking : t.trackerSubmit}
              </button>
            </div>

            {recentOrders.length > 0 && (
              <div className="pt-2 border-t border-[#0F3D28]/10 space-y-2">
                <p className="text-[11px] font-semibold text-[#526058]">
                  {t.trackerRecent}
                </p>
                <div className="flex flex-wrap gap-2">
                  {recentOrders.map((ord) => {
                    const selected = activeOrder?.orderId === ord.orderId;
                    return (
                      <button
                        key={ord.orderId}
                        type="button"
                        onClick={() => performLookup(ord.orderId)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold inline-flex items-center gap-2 border transition-colors cursor-pointer whitespace-nowrap ${
                          selected
                            ? 'bg-[#0F3D28] text-white border-[#0F3D28]'
                            : 'bg-[#F8F7F2] text-[#14221B] border-[#0F3D28]/15 hover:border-[#0F3D28]/40'
                        }`}
                      >
                        <span className="font-mono-num">{ord.orderId}</span>
                        <span aria-hidden="true">·</span>
                        <span className="tabular-nums">₹{ord.totalPayable}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </form>

          {/* Error / Not Found Banner */}
          {errorMsg && (
            <div className="bg-white rounded-xl p-4 border border-amber-300 flex items-start gap-3 text-xs sm:text-sm text-[#78350F]">
              <AlertCircle className="w-5 h-5 text-[#B45309] shrink-0 mt-0.5" />
              <div className="space-y-1.5">
                <p className="font-semibold text-[#14221B]">Order Lookup Notice</p>
                <p>{errorMsg}</p>
                <a
                  href={`https://wa.me/918847370905?text=${encodeURIComponent(
                    `Hello Nav Grocery Store, please help me check the status of Order ${queryId.trim().toUpperCase()}.`
                  )}`}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 font-bold text-[#0F3D28] underline pt-0.5"
                >
                  <MessageCircle className="w-4 h-4" />
                  <span>Verify via WhatsApp Helpline (+91 8847370905)</span>
                </a>
              </div>
            </div>
          )}

          {/* Initial Empty Helper Prompt */}
          {!activeOrder && !errorMsg && !loading && !searchedOnce && (
            <div className="bg-white rounded-xl p-6 border border-[#0F3D28]/10 text-center space-y-2">
              <p className="font-display text-lg font-bold text-[#14221B]">
                {t.trackerEmptyTitle}
              </p>
              <p className="text-xs sm:text-sm text-[#526058] max-w-md mx-auto">
                {t.trackerEmptyDesc}
              </p>
            </div>
          )}

          {/* Active Tracked Order Result */}
          {activeOrder && stage && (
            <div className="space-y-4">
              {/* Overview Card */}
              <div className="bg-white rounded-xl p-5 border border-[#0F3D28]/12 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#0F3D28]/10">
                  <div>
                    <div className="flex flex-wrap items-center gap-2 text-xs text-[#526058]">
                      <span className="font-mono-num font-bold text-sm text-[#0F3D28]">
                        {activeOrder.orderId}
                      </span>
                      <span aria-hidden="true">·</span>
                      <span>Placed {formatOrderDate(activeOrder.createdAt)}</span>
                    </div>
                    <div className="flex items-center gap-2 mt-1">
                      {activeOrder.fulfillment === 'pickup' ? (
                        <Store className="w-4 h-4 text-[#0F3D28]" />
                      ) : (
                        <Truck className="w-4 h-4 text-[#0F3D28]" />
                      )}
                      <span className="text-sm font-bold text-[#14221B]">
                        {activeOrder.fulfillment === 'pickup'
                          ? '30-Min Express Store Pickup'
                          : '2-Hour Local Delivery'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <span
                      className={`px-3 py-1 rounded-lg text-xs font-bold inline-flex items-center gap-1.5 ${
                        stage.cancelled
                          ? 'bg-red-50 text-red-700 border border-red-200'
                          : stage.stepIndex === 3
                          ? 'bg-[#0F3D28] text-white'
                          : 'bg-[#E6A817]/25 text-[#14221B] border border-[#E6A817]/60'
                      }`}
                    >
                      {stage.cancelled ? (
                        <AlertCircle className="w-3.5 h-3.5" />
                      ) : stage.stepIndex === 3 ? (
                        <CheckCircle2 className="w-3.5 h-3.5 text-[#E6A817]" />
                      ) : (
                        <Clock className="w-3.5 h-3.5 text-[#0F3D28]" />
                      )}
                      <span>{stage.displayLabel}</span>
                    </span>
                    <button
                      type="button"
                      onClick={() => performLookup(activeOrder.orderId)}
                      disabled={loading}
                      aria-label="Refresh order status"
                      className="p-2 rounded-lg border border-[#0F3D28]/20 text-[#0F3D28] hover:bg-[#F8F7F2] transition-colors cursor-pointer disabled:opacity-50"
                    >
                      <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                    </button>
                  </div>
                </div>

                {/* Progress Stepper */}
                {stage.cancelled ? (
                  <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-xs sm:text-sm text-red-800 space-y-1">
                    <p className="font-bold">This order has been marked as Cancelled.</p>
                    <p>
                      For refund or re-order assistance, please reach out on WhatsApp +91 8847370905.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {steps.map((step, idx) => {
                      const completed = idx <= stage.stepIndex;
                      const current = idx === stage.stepIndex;
                      return (
                        <div key={step.key} className="flex items-start gap-3">
                          <div className="flex flex-col items-center">
                            <div
                              className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 border ${
                                completed
                                  ? 'bg-[#0F3D28] text-[#E6A817] border-[#0F3D28]'
                                  : 'bg-[#F8F7F2] text-[#526058] border-[#0F3D28]/20'
                              }`}
                            >
                              {completed ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                            </div>
                            {idx < steps.length - 1 && (
                              <div
                                className={`w-0.5 h-6 mt-1 ${
                                  idx < stage.stepIndex ? 'bg-[#0F3D28]' : 'bg-[#0F3D28]/15'
                                }`}
                              />
                            )}
                          </div>
                          <div className="pt-0.5 pb-1">
                            <p
                              className={`text-sm font-bold leading-snug ${
                                current
                                  ? 'text-[#0F3D28]'
                                  : completed
                                  ? 'text-[#14221B]'
                                  : 'text-[#526058]'
                              }`}
                            >
                              {step.label}
                              {current && (
                                <span className="ml-2 text-[11px] font-semibold text-[#B45309]">
                                  (Current Stage)
                                </span>
                              )}
                            </p>
                            <p className="text-xs text-[#526058] mt-0.5">{step.description}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Slot & Address Metadata */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-3 border-t border-[#0F3D28]/10 text-xs bg-[#F8F7F2] p-3.5 rounded-xl">
                  <div>
                    <span className="text-[#526058]">Customer &amp; Phone</span>
                    <p className="font-semibold text-[#14221B] mt-0.5">
                      {activeOrder.customerName} · +91 {activeOrder.customerPhone}
                    </p>
                  </div>
                  <div>
                    <span className="text-[#526058]">Scheduled Slot</span>
                    <p className="font-semibold text-[#14221B] mt-0.5">{activeOrder.slot}</p>
                  </div>
                  <div className="sm:col-span-2 flex items-start gap-1.5">
                    <MapPin className="w-3.5 h-3.5 text-[#0F3D28] shrink-0 mt-0.5" />
                    <div>
                      <span className="text-[#526058]">
                        {activeOrder.fulfillment === 'pickup' ? 'Pickup Point: ' : 'Delivery Address: '}
                      </span>
                      <span className="font-semibold text-[#14221B]">{activeOrder.address}</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Items & Payment Summary */}
              <div className="bg-white rounded-xl p-5 border border-[#0F3D28]/12 space-y-3 text-xs">
                <div className="flex items-center justify-between border-b border-[#0F3D28]/10 pb-2.5">
                  <span className="font-bold text-sm text-[#14221B]">
                    Order Items ({activeOrder.items.reduce((acc, i) => acc + i.qty, 0)})
                  </span>
                  <span className="font-semibold text-[#0F3D28]">
                    {activeOrder.paymentMethod.toUpperCase()} · {activeOrder.paymentStatus}
                  </span>
                </div>

                <div className="divide-y divide-[#0F3D28]/10">
                  {activeOrder.items.map((item) => (
                    <div key={item.id} className="py-2 flex justify-between gap-2 tabular-nums">
                      <span className="text-[#14221B]">
                        {item.name} × {item.qty}
                      </span>
                      <span className="font-semibold text-[#14221B]">₹{item.sale * item.qty}</span>
                    </div>
                  ))}
                </div>

                <div className="pt-2.5 border-t border-[#0F3D28]/15 space-y-1 tabular-nums">
                  <div className="flex justify-between text-[#0F3D28] font-semibold">
                    <span>12% Wholesale Savings</span>
                    <span>-₹{activeOrder.savings}</span>
                  </div>
                  <div className="flex justify-between text-[#526058]">
                    <span>Delivery / Packing</span>
                    <span>
                      {activeOrder.deliveryFee === 0 ? 'FREE' : `₹${activeOrder.deliveryFee}`}
                    </span>
                  </div>
                  <div className="flex justify-between text-base font-bold text-[#14221B] pt-1">
                    <span>Total Payable</span>
                    <span>₹{activeOrder.totalPayable}</span>
                  </div>
                </div>

                <div className="pt-2 flex flex-col sm:flex-row gap-2.5">
                  <a
                    href={`https://wa.me/918847370905?text=${encodeURIComponent(
                      `Hello Nav Grocery Store, I am checking the status of my Order ${activeOrder.orderId} (Current Status: ${activeOrder.status}).`
                    )}`}
                    target="_blank"
                    rel="noreferrer"
                    className="flex-1 py-2.5 px-4 rounded-xl bg-[#0F3D28] text-white font-bold text-xs flex items-center justify-center gap-2 hover:bg-[#0A2A1B] transition-colors"
                  >
                    <MessageCircle className="w-4 h-4 text-[#E6A817]" />
                    <span>Need Help? Message Store on WhatsApp</span>
                  </a>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
