import React from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  MapPin,
  MessageCircle,
  Package,
  ShoppingBasket,
  Store,
  Truck,
} from 'lucide-react';
import { OrderRecord } from '../types';

interface LiveOrderTrackerCardProps {
  order: OrderRecord;
  onReorder: (order: OrderRecord) => void;
}

interface StageItem {
  key: string;
  label: string;
  sublabel: string;
}

function getStages(fulfillment: OrderRecord['fulfillment']): StageItem[] {
  if (fulfillment === 'pickup') {
    return [
      { key: 'placed', label: 'Order Placed', sublabel: 'Confirmed at Main Bazaar' },
      { key: 'packed', label: 'Packed & Sealed', sublabel: 'Cold-chain -18°C held' },
      { key: 'ready', label: 'Ready for Pickup', sublabel: 'Sandhaur Pickup Counter' },
      { key: 'delivered', label: 'Delivered', sublabel: 'Picked up by customer' },
    ];
  }
  return [
    { key: 'placed', label: 'Order Placed', sublabel: 'Logged at Sandhaur 148020' },
    { key: 'packed', label: 'Packed & Sealed', sublabel: 'Sealed & QC verified' },
    { key: 'transit', label: 'Out for Delivery', sublabel: 'Sandhaur rider en route' },
    { key: 'delivered', label: 'Delivered', sublabel: 'Handed over safely' },
  ];
}

function resolveStage(statusRaw: string): {
  stepIndex: number;
  cancelled: boolean;
  label: string;
} {
  const s = (statusRaw || 'Pending').trim().toLowerCase();
  if (s.includes('cancel')) {
    return { stepIndex: -1, cancelled: true, label: 'Cancelled' };
  }
  if ((s.includes('deliver') && !s.includes('out')) || s.includes('picked') || s.includes('complete')) {
    return { stepIndex: 3, cancelled: false, label: statusRaw || 'Delivered' };
  }
  if (s.includes('out') || s.includes('dispatch') || s.includes('ready') || s.includes('transit')) {
    return { stepIndex: 2, cancelled: false, label: statusRaw };
  }
  if (s.includes('pack') || s.includes('prepar') || s.includes('process')) {
    return { stepIndex: 1, cancelled: false, label: statusRaw };
  }
  return { stepIndex: 0, cancelled: false, label: statusRaw || 'Pending' };
}

function formatDate(iso: string): string {
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

export const LiveOrderTrackerCard: React.FC<LiveOrderTrackerCardProps> = ({
  order,
  onReorder,
}) => {
  const stages = getStages(order.fulfillment);
  const stage = resolveStage(order.status);
  const totalQty = order.items.reduce((acc, it) => acc + Math.max(1, Number(it.qty) || 1), 0);

  return (
    <article className="bg-white rounded-2xl p-4 sm:p-5 border border-[#0F3D28]/12 space-y-4 shadow-2xs">
      {/* Top Order Metadata */}
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-[#0F3D28]/10 pb-3">
        <div className="space-y-0.5">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono-num font-bold text-sm text-[#0F3D28]">
              {order.orderId}
            </span>
            <span className="text-xs text-[#526058]">· {formatDate(order.createdAt)}</span>
          </div>
          <div className="flex items-center gap-1.5 text-xs font-semibold text-[#14221B]">
            {order.fulfillment === 'pickup' ? (
              <Store className="w-3.5 h-3.5 text-[#0F3D28]" />
            ) : (
              <Truck className="w-3.5 h-3.5 text-[#0F3D28]" />
            )}
            <span>{order.slot}</span>
          </div>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <span
            className={`px-2.5 py-1 rounded-lg text-xs font-bold inline-flex items-center gap-1.5 ${
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
            <span>{stage.label}</span>
          </span>
          <span className="font-mono-num font-bold text-base text-[#0F3D28]">
            ₹{order.totalPayable}
          </span>
        </div>
      </div>

      {/* 4-Stage Visual Delivery Progress Stepper */}
      {stage.cancelled ? (
        <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-xs text-red-800">
          This order was marked as <strong>Cancelled</strong>. For refund or reorder help in Sandhaur 148020, message +91 8847370905.
        </div>
      ) : (
        <div className="bg-[#F8F7F2] rounded-xl p-3.5 sm:p-4 border border-[#0F3D28]/10">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 relative">
            {stages.map((st, idx) => {
              const completed = idx <= stage.stepIndex;
              const current = idx === stage.stepIndex;
              return (
                <div key={st.key} className="relative flex sm:flex-col items-start sm:items-center gap-2.5 sm:gap-1.5 sm:text-center">
                  {/* Connector line on sm+ screens */}
                  {idx < stages.length - 1 && (
                    <div
                      aria-hidden="true"
                      className={`hidden sm:block absolute top-3.5 left-[56%] right-[-44%] h-0.5 ${
                        idx < stage.stepIndex ? 'bg-[#0F3D28]' : 'bg-[#0F3D28]/15'
                      }`}
                    />
                  )}
                  <div
                    className={`relative z-10 w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 border transition-colors ${
                      current
                        ? 'bg-[#E6A817] text-[#14221B] border-[#0F3D28] ring-2 ring-[#E6A817]/40'
                        : completed
                        ? 'bg-[#0F3D28] text-[#E6A817] border-[#0F3D28]'
                        : 'bg-white text-[#526058] border-[#0F3D28]/20'
                    }`}
                  >
                    {completed ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                  </div>
                  <div className="min-w-0">
                    <p
                      className={`text-xs font-bold leading-snug ${
                        current
                          ? 'text-[#0F3D28]'
                          : completed
                          ? 'text-[#14221B]'
                          : 'text-[#526058]'
                      }`}
                    >
                      {st.label}
                    </p>
                    <p className="text-[10px] text-[#526058] leading-tight mt-0.5">{st.sublabel}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Delivery Address & Payment Info */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-[#526058]">
        <div className="flex items-start gap-1.5 min-w-0">
          <MapPin className="w-3.5 h-3.5 text-[#0F3D28] shrink-0 mt-0.5" />
          <span className="text-[#14221B] font-medium truncate">{order.address}</span>
        </div>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 shrink-0">
          <span>
            Payment: <strong className="text-[#14221B]">{order.paymentMethod.toUpperCase()}</strong> ({order.paymentStatus})
            {order.utrRef ? ` · UTR ${order.utrRef}` : ''}
          </span>
          <span className="text-[#0F3D28] font-bold">
            12% Off Saved ₹{order.savings}
          </span>
        </div>
      </div>

      {/* Items List + Reorder CTA */}
      <div className="pt-2.5 border-t border-[#0F3D28]/10 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-start gap-2 min-w-0">
          <Package className="w-4 h-4 text-[#0F3D28] shrink-0 mt-0.5" />
          <p className="text-xs text-[#14221B] leading-relaxed">
            <strong className="text-[#0F3D28]">{totalQty} unit{totalQty === 1 ? '' : 's'}:</strong>{' '}
            {order.items.map((it) => `${it.name} ×${it.qty}`).join(', ')}
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <a
            href={`https://wa.me/918847370905?text=${encodeURIComponent(
              `Hello Nav Grocery Store, checking live status for Order ${order.orderId} (${order.status}).`
            )}`}
            target="_blank"
            rel="noreferrer"
            className="px-3 py-2 rounded-lg border border-[#0F3D28]/20 text-[#0F3D28] hover:bg-[#F8F7F2] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap"
          >
            <MessageCircle className="w-3.5 h-3.5" />
            <span>Help</span>
          </a>
          <button
            type="button"
            onClick={() => onReorder(order)}
            className="px-3.5 py-2 rounded-lg bg-[#0F3D28] text-white hover:bg-[#0A2A1B] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
          >
            <ShoppingBasket className="w-3.5 h-3.5 text-[#E6A817]" />
            <span>Reorder ({totalQty})</span>
          </button>
        </div>
      </div>
    </article>
  );
};
