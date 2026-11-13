import React, { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import {
  X,
  Plus,
  Minus,
  Trash2,
  ArrowRight,
  ArrowLeft,
  QrCode,
  Banknote,
  CreditCard,
  MessageCircle,
  CheckCircle2,
  Printer,
  Truck,
  Store,
  PackageCheck,
} from 'lucide-react';
import {
  CartItem,
  CustomerSession,
  FulfillmentMode,
  OrderRecord,
  PaymentMethod,
  PaymentSettings,
} from '../types';
import { StoreLogo } from './StoreLogo';
import { saveOrder } from '../utils/firebase';

interface CartCheckoutDrawerProps {
  isOpen: boolean;
  logoUrl: string;
  items: CartItem[];
  fulfillment: FulfillmentMode;
  paymentSettings: PaymentSettings;
  customerSession: CustomerSession | null;
  onClose: () => void;
  onUpdateQty: (productId: string, qty: number) => void;
  onRemoveItem: (productId: string) => void;
  onClearCart: () => void;
  onChangeFulfillment: (mode: FulfillmentMode) => void;
  onTrackOrder?: (orderId: string) => void;
}

export const CartCheckoutDrawer: React.FC<CartCheckoutDrawerProps> = ({
  isOpen,
  logoUrl,
  items,
  fulfillment,
  paymentSettings,
  customerSession,
  onClose,
  onUpdateQty,
  onRemoveItem,
  onClearCart,
  onChangeFulfillment,
  onTrackOrder,
}) => {
  const [step, setStep] = useState<'cart' | 'details' | 'payment' | 'receipt'>('cart');
  const [customerName, setCustomerName] = useState(customerSession?.name || '');
  const [customerPhone, setCustomerPhone] = useState(customerSession?.phone || '');
  const [address, setAddress] = useState(customerSession?.address || '');
  const [slot, setSlot] = useState(
    paymentSettings.pickupSlots[0] || '30-Min Store Pickup (Main Bazaar, Sandhaur)'
  );
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('upi');
  const [utrRef, setUtrRef] = useState('');
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [confirmedOrder, setConfirmedOrder] = useState<OrderRecord | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (customerSession) {
      if (!customerName) setCustomerName(customerSession.name);
      if (!customerPhone) setCustomerPhone(customerSession.phone);
      if (!address) setAddress(customerSession.address);
    }
  }, [customerSession, customerName, customerPhone, address]);

  const subtotalMrp = items.reduce((acc, i) => acc + i.product.mrp * i.quantity, 0);
  const subtotalSale = items.reduce((acc, i) => acc + i.product.sale * i.quantity, 0);
  const totalSavings = Math.max(0, subtotalMrp - subtotalSale);
  const deliveryFee =
    fulfillment === 'pickup' || subtotalSale >= paymentSettings.freeDeliveryAbove || subtotalSale === 0
      ? 0
      : paymentSettings.deliveryFee;
  const totalPayable = subtotalSale + deliveryFee;

  const upiUri = `upi://pay?pa=${encodeURIComponent(paymentSettings.upiId)}&pn=${encodeURIComponent(
    paymentSettings.upiMerchantName || 'Nav Grocery Store'
  )}&am=${totalPayable.toFixed(2)}&cu=INR&tn=${encodeURIComponent('Nav Grocery Store Order')}`;

  useEffect(() => {
    if (!isOpen || step !== 'payment' || !paymentSettings.upiEnabled || totalPayable <= 0) return;
    QRCode.toDataURL(upiUri, {
      width: 220,
      margin: 2,
      color: { dark: '#0F3D28', light: '#FFFFFF' },
    })
      .then((url) => setQrDataUrl(url))
      .catch(() => setQrDataUrl(''));
  }, [isOpen, step, upiUri, paymentSettings.upiEnabled, totalPayable]);

  if (!isOpen) return null;

  const codAllowed = paymentSettings.codEnabled && subtotalSale >= paymentSettings.codMinOrder;

  const buildOrderRecord = (statusLabel: OrderRecord['paymentStatus']): OrderRecord => {
    const orderId = `NGS-${new Date().toISOString().slice(2, 10).replace(/-/g, '')}-${Math.floor(
      1000 + Math.random() * 9000
    )}`;
    return {
      orderId,
      createdAt: new Date().toISOString(),
      customerName: customerName.trim(),
      customerPhone: customerPhone.replace(/\D/g, '').slice(-10),
      address:
        fulfillment === 'pickup'
          ? '30-Min Store Pickup — Main Bazaar, Sandhaur 148020'
          : address.trim(),
      fulfillment,
      slot,
      paymentMethod,
      paymentStatus: statusLabel,
      utrRef: utrRef.trim() || undefined,
      subtotalMrp,
      subtotalSale,
      savings: totalSavings,
      deliveryFee,
      totalPayable,
      status: 'Pending',
      items: items.map((i) => ({
        id: i.product.id,
        name: i.product.name,
        brand: i.product.brand,
        qty: i.quantity,
        mrp: i.product.mrp,
        sale: i.product.sale,
        gst: i.product.gst || '5%',
      })),
    };
  };

  const handleConfirmOrder = async () => {
    setErrorMsg('');
    if (paymentMethod === 'upi' && utrRef.trim().length < 6) {
      setErrorMsg('Please enter your 12-digit UPI Transaction / UTR Reference after scanning QR.');
      return;
    }
    if (paymentMethod === 'cod' && !codAllowed) {
      setErrorMsg(`Cash on Delivery requires minimum order of ₹${paymentSettings.codMinOrder}.`);
      return;
    }

    setSubmitting(true);
    const paymentStatus: OrderRecord['paymentStatus'] =
      paymentMethod === 'upi'
        ? 'Paid / UTR Submitted'
        : paymentMethod === 'razorpay'
        ? 'Gateway Paid'
        : fulfillment === 'pickup'
        ? 'Pay at Pickup'
        : 'Pay on Delivery';

    const order = buildOrderRecord(paymentStatus);
    await saveOrder(order);
    setConfirmedOrder(order);
    onClearCart();
    setSubmitting(false);
    setStep('receipt');
  };

  const handleRazorpayTrigger = () => {
    if (!paymentSettings.razorpayKeyId.trim()) {
      setErrorMsg('Online card gateway is currently unavailable. Please choose UPI QR or Cash on Delivery.');
      return;
    }
    setPaymentMethod('razorpay');
    setErrorMsg('');
    handleConfirmOrder();
  };

  const getWhatsAppReceiptUrl = (order: OrderRecord) => {
    const lines = [
      `*Nav Grocery Store — Order Receipt*`,
      `Main Bazaar, Sandhaur, Punjab 148020`,
      `Order ID: *${order.orderId}*`,
      `Customer: ${order.customerName} (+91 ${order.customerPhone})`,
      `Mode: ${order.fulfillment === 'pickup' ? '30-Min Store Pickup' : '2-Hr Local Delivery'} (${order.slot})`,
      `Address: ${order.address}`,
      `Payment: ${order.paymentMethod.toUpperCase()} — ${order.paymentStatus}${
        order.utrRef ? ` (UTR: ${order.utrRef})` : ''
      }`,
      ``,
      `*Items:*`,
      ...order.items.map(
        (it, idx) => `${idx + 1}. ${it.name} x${it.qty} = ₹${it.sale * it.qty}`
      ),
      ``,
      `Total Savings (12% Off): ₹${order.savings}`,
      `Delivery Fee: ₹${order.deliveryFee}`,
      `*Total Payable: ₹${order.totalPayable}*`,
    ];
    return `https://wa.me/918847370905?text=${encodeURIComponent(lines.join('\n'))}`;
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/55 backdrop-blur-xs flex justify-end"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Basket and Checkout Drawer"
    >
      <div
        className="w-full max-w-lg bg-[#F8F7F2] h-full flex flex-col shadow-2xl border-l border-[#0F3D28]/15"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-4 bg-[#0F3D28] text-white flex items-center justify-between no-print">
          <div>
            <h2 className="font-display font-bold text-lg">
              {step === 'cart' && `Your Basket (${items.reduce((a, b) => a + b.quantity, 0)})`}
              {step === 'details' && 'Delivery & Pickup Details'}
              {step === 'payment' && 'Select Payment Method'}
              {step === 'receipt' && 'Order Confirmed — Digital Bill'}
            </h2>
            <p className="text-xs text-white/75">Nav Grocery Store · Sandhaur 148020</p>
          </div>
          <button
            type="button"
            onClick={() => {
              if (step === 'receipt') setStep('cart');
              onClose();
            }}
            aria-label="Close basket"
            className="p-1.5 rounded-lg hover:bg-white/15 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {step === 'cart' && (
            <>
              {items.length === 0 ? (
                <div className="bg-white rounded-2xl p-8 text-center border border-[#0F3D28]/10 space-y-3">
                  <p className="font-display text-lg font-bold text-[#14221B]">Your basket is empty</p>
                  <p className="text-sm text-[#526058]">
                    Add sealed packaged groceries or -18°C frozen essentials at flat 12% off MRP.
                  </p>
                  <button
                    type="button"
                    onClick={onClose}
                    className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold cursor-pointer"
                  >
                    Browse 308 Groceries
                  </button>
                </div>
              ) : (
                <>
                  <div className="bg-white rounded-xl p-3 border border-[#0F3D28]/10 flex items-center justify-between text-xs">
                    <span className="font-semibold text-[#0F3D28]">
                      {subtotalSale >= paymentSettings.freeDeliveryAbove
                        ? 'Unlocked: Free 2-Hour Delivery in Sandhaur 148020'
                        : `Add ₹${paymentSettings.freeDeliveryAbove - subtotalSale} more for Free Delivery`}
                    </span>
                    <span className="font-bold text-[#0F3D28] tabular-nums">
                      Savings ₹{totalSavings}
                    </span>
                  </div>

                  <div className="space-y-2.5">
                    {items.map(({ product, quantity }) => (
                      <div
                        key={product.id}
                        className="bg-white rounded-xl p-3.5 border border-[#0F3D28]/10 flex items-center justify-between gap-3"
                      >
                        <div className="min-w-0 flex-1">
                          <p className="text-[11px] font-semibold uppercase text-[#526058]">
                            {product.brand} · {product.id}
                          </p>
                          <p className="text-sm font-semibold text-[#14221B] truncate">{product.name}</p>
                          <p className="text-xs text-[#526058] tabular-nums">
                            ₹{product.sale} × {quantity} ={' '}
                            <strong className="text-[#0F3D28]">₹{product.sale * quantity}</strong>{' '}
                            <span className="line-through text-[11px]">₹{product.mrp * quantity}</span>
                          </p>
                        </div>

                        <div className="flex items-center gap-2 shrink-0">
                          <div className="inline-flex items-center rounded-lg bg-[#0F3D28] text-white overflow-hidden">
                            <button
                              type="button"
                              onClick={() => onUpdateQty(product.id, quantity - 1)}
                              className="w-7 h-7 flex items-center justify-center hover:bg-[#0A2A1B] cursor-pointer"
                            >
                              <Minus className="w-3 h-3" />
                            </button>
                            <span className="px-2 text-xs font-bold tabular-nums">{quantity}</span>
                            <button
                              type="button"
                              onClick={() => onUpdateQty(product.id, quantity + 1)}
                              className="w-7 h-7 flex items-center justify-center hover:bg-[#0A2A1B] cursor-pointer"
                            >
                              <Plus className="w-3 h-3" />
                            </button>
                          </div>
                          <button
                            type="button"
                            onClick={() => onRemoveItem(product.id)}
                            aria-label={`Remove ${product.name}`}
                            className="p-1.5 text-neutral-400 hover:text-red-600 cursor-pointer"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </>
          )}

          {step === 'details' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-2.5">
                <button
                  type="button"
                  onClick={() => onChangeFulfillment('delivery')}
                  className={`p-3 rounded-xl border text-left flex items-center gap-2.5 cursor-pointer ${
                    fulfillment === 'delivery'
                      ? 'bg-[#0F3D28] text-white border-[#0F3D28]'
                      : 'bg-white text-[#14221B] border-[#0F3D28]/15'
                  }`}
                >
                  <Truck className="w-4 h-4 text-[#E6A817] shrink-0" />
                  <div>
                    <p className="text-xs font-bold">2-Hr Local Delivery</p>
                    <p className="text-[11px] opacity-80">Sandhaur 148020</p>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => onChangeFulfillment('pickup')}
                  className={`p-3 rounded-xl border text-left flex items-center gap-2.5 cursor-pointer ${
                    fulfillment === 'pickup'
                      ? 'bg-[#0F3D28] text-white border-[#0F3D28]'
                      : 'bg-white text-[#14221B] border-[#0F3D28]/15'
                  }`}
                >
                  <Store className="w-4 h-4 text-[#E6A817] shrink-0" />
                  <div>
                    <p className="text-xs font-bold">30-Min Store Pickup</p>
                    <p className="text-[11px] opacity-80">Main Bazaar · Free</p>
                  </div>
                </button>
              </div>

              <div className="bg-white rounded-xl p-4 border border-[#0F3D28]/10 space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-[#526058] mb-1">Full Name *</label>
                  <input
                    type="text"
                    value={customerName}
                    onChange={(e) => setCustomerName(e.target.value)}
                    placeholder="Customer full name"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-[#526058] mb-1">
                    WhatsApp Mobile (+91) *
                  </label>
                  <input
                    type="tel"
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                    placeholder="10-digit mobile number"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm font-mono-num focus:outline-none focus:border-[#0F3D28]"
                  />
                </div>
                {fulfillment === 'delivery' && (
                  <div>
                    <label className="block text-xs font-semibold text-[#526058] mb-1">
                      Delivery Address / Village *
                    </label>
                    <textarea
                      rows={2}
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      placeholder="House No, Street, Main Bazaar / Village, Sandhaur 148020"
                      className="w-full px-3.5 py-2 rounded-xl border border-[#0F3D28]/20 text-sm focus:outline-none focus:border-[#0F3D28]"
                    />
                  </div>
                )}
                <div>
                  <label className="block text-xs font-semibold text-[#526058] mb-1">
                    Preferred Time Slot
                  </label>
                  <select
                    value={slot}
                    onChange={(e) => setSlot(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/20 text-sm bg-white focus:outline-none focus:border-[#0F3D28]"
                  >
                    {paymentSettings.pickupSlots.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              {errorMsg && <p className="text-xs font-semibold text-red-600">{errorMsg}</p>}
            </div>
          )}

          {step === 'payment' && (
            <div className="space-y-3.5">
              {paymentSettings.upiEnabled && (
                <div
                  className={`bg-white rounded-xl p-4 border transition-colors ${
                    paymentMethod === 'upi' ? 'border-[#0F3D28] ring-1 ring-[#0F3D28]' : 'border-[#0F3D28]/15'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentMethod('upi');
                      setErrorMsg('');
                    }}
                    className="w-full flex items-center justify-between text-left cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5">
                      <QrCode className="w-5 h-5 text-[#0F3D28]" />
                      <div>
                        <p className="text-sm font-bold text-[#14221B]">UPI Direct Pay + Dynamic QR</p>
                        <p className="text-xs text-[#526058]">
                          GPay, PhonePe, Paytm, BHIM · Instant Verification
                        </p>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[#0F3D28] tabular-nums">₹{totalPayable}</span>
                  </button>

                  {paymentMethod === 'upi' && (
                    <div className="mt-4 pt-4 border-t border-[#0F3D28]/10 space-y-3">
                      <div className="flex flex-col sm:flex-row items-center gap-4">
                        {qrDataUrl && (
                          <img
                            src={qrDataUrl}
                            alt={`UPI QR Code for ₹${totalPayable}`}
                            className="w-40 h-40 rounded-xl border border-[#0F3D28]/15 p-1 bg-white"
                          />
                        )}
                        <div className="flex-1 space-y-2 text-xs">
                          <p className="font-semibold text-[#14221B]">
                            Scan with any UPI app to pay exact amount ₹{totalPayable}
                          </p>
                          <a
                            href={upiUri}
                            className="inline-flex items-center justify-center w-full py-2 px-3 rounded-lg bg-[#E6A817] text-[#14221B] font-bold hover:bg-[#f0b72b]"
                          >
                            Open UPI App to Pay ₹{totalPayable}
                          </a>
                          <div>
                            <label className="block text-[11px] font-semibold text-[#526058] mb-1">
                              Enter 12-Digit UTR / Transaction Ref *
                            </label>
                            <input
                              type="text"
                              value={utrRef}
                              onChange={(e) => setUtrRef(e.target.value)}
                              placeholder="e.g. 431298561023"
                              className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 font-mono-num text-xs"
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {paymentSettings.codEnabled && (
                <div
                  className={`bg-white rounded-xl p-4 border ${
                    paymentMethod === 'cod' ? 'border-[#0F3D28] ring-1 ring-[#0F3D28]' : 'border-[#0F3D28]/15'
                  }`}
                >
                  <button
                    type="button"
                    disabled={!codAllowed}
                    onClick={() => {
                      setPaymentMethod('cod');
                      setErrorMsg('');
                    }}
                    className={`w-full flex items-center justify-between text-left ${
                      codAllowed ? 'cursor-pointer' : 'opacity-50 cursor-not-allowed'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <Banknote className="w-5 h-5 text-[#0F3D28]" />
                      <div>
                        <p className="text-sm font-bold text-[#14221B]">
                          {fulfillment === 'pickup' ? 'Pay Cash / UPI at Pickup Counter' : 'Cash on Delivery (COD)'}
                        </p>
                        <p className="text-xs text-[#526058]">
                          {codAllowed
                            ? 'Pay when your sealed grocery order arrives'
                            : `Minimum order ₹${paymentSettings.codMinOrder} required for COD`}
                        </p>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[#0F3D28] tabular-nums">₹{totalPayable}</span>
                  </button>
                </div>
              )}

              {paymentSettings.razorpayEnabled && paymentSettings.razorpayKeyId.trim() && (
                <div
                  className={`bg-white rounded-xl p-4 border ${
                    paymentMethod === 'razorpay' ? 'border-[#0F3D28] ring-1 ring-[#0F3D28]' : 'border-[#0F3D28]/15'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <CreditCard className="w-5 h-5 text-[#0F3D28]" />
                      <div>
                        <p className="text-sm font-bold text-[#14221B]">Cards / NetBanking / Wallets</p>
                        <p className="text-xs text-[#526058]">Instant encrypted online payment</p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={handleRazorpayTrigger}
                      className="px-3 py-1.5 rounded-lg bg-[#0F3D28] text-white text-xs font-semibold hover:bg-[#0A2A1B] cursor-pointer"
                    >
                      Pay Now
                    </button>
                  </div>
                </div>
              )}

              {errorMsg && <p className="text-xs font-semibold text-red-600">{errorMsg}</p>}
            </div>
          )}

          {step === 'receipt' && confirmedOrder && (
            <div className="bg-white rounded-2xl p-5 border border-[#0F3D28]/15 space-y-4">
              <div className="flex items-center justify-between border-b border-[#0F3D28]/10 pb-3">
                <div className="flex items-center gap-3">
                  <StoreLogo logoUrl={logoUrl} sizeClass="w-10 h-10" />
                  <div>
                    <h3 className="font-display font-bold text-base text-[#14221B]">Nav Grocery Store</h3>
                    <p className="text-[11px] text-[#526058]">
                      Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab 148020
                    </p>
                    <p className="text-[11px] font-mono-num text-[#526058]">
                      GSTIN: 03AHPPU6185N1ZF · orders@navgrocerystore.com
                    </p>
                  </div>
                </div>
                <CheckCircle2 className="w-6 h-6 text-[#0F3D28] shrink-0" />
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs bg-[#F8F7F2] p-3 rounded-xl">
                <div>
                  <span className="text-[#526058]">Order ID:</span>
                  <p className="font-mono-num font-bold text-[#0F3D28]">{confirmedOrder.orderId}</p>
                </div>
                <div>
                  <span className="text-[#526058]">Payment:</span>
                  <p className="font-semibold text-[#14221B]">{confirmedOrder.paymentStatus}</p>
                </div>
                <div>
                  <span className="text-[#526058]">Customer:</span>
                  <p className="font-semibold text-[#14221B]">{confirmedOrder.customerName}</p>
                </div>
                <div>
                  <span className="text-[#526058]">Slot:</span>
                  <p className="font-semibold text-[#14221B]">{confirmedOrder.slot}</p>
                </div>
              </div>

              <div className="divide-y divide-[#0F3D28]/10 text-xs">
                {confirmedOrder.items.map((it) => (
                  <div key={it.id} className="py-2 flex justify-between gap-2 tabular-nums">
                    <span>
                      {it.name} × {it.qty}
                    </span>
                    <span className="font-semibold">₹{it.sale * it.qty}</span>
                  </div>
                ))}
              </div>

              <div className="border-t border-[#0F3D28]/15 pt-3 space-y-1 text-xs tabular-nums">
                <div className="flex justify-between text-[#0F3D28] font-semibold">
                  <span>12% Wholesale Savings</span>
                  <span>-₹{confirmedOrder.savings}</span>
                </div>
                <div className="flex justify-between text-[#526058]">
                  <span>Delivery / Packing</span>
                  <span>{confirmedOrder.deliveryFee === 0 ? 'FREE' : `₹${confirmedOrder.deliveryFee}`}</span>
                </div>
                <div className="flex justify-between text-base font-bold text-[#14221B] pt-1">
                  <span>Total Payable</span>
                  <span>₹{confirmedOrder.totalPayable}</span>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row flex-wrap gap-2.5 pt-2 no-print">
                {onTrackOrder && (
                  <button
                    type="button"
                    onClick={() => {
                      setStep('cart');
                      onTrackOrder(confirmedOrder.orderId);
                    }}
                    className="w-full py-2.5 px-4 rounded-xl bg-[#E6A817] text-[#14221B] text-xs font-bold inline-flex items-center justify-center gap-2 hover:bg-[#f0b72b] cursor-pointer"
                  >
                    <PackageCheck className="w-4 h-4" />
                    <span>Track Order Status ({confirmedOrder.orderId})</span>
                  </button>
                )}
                <a
                  href={getWhatsAppReceiptUrl(confirmedOrder)}
                  target="_blank"
                  rel="noreferrer"
                  className="flex-1 py-2.5 px-4 rounded-xl bg-[#0F3D28] text-white text-xs font-bold flex items-center justify-center gap-2 hover:bg-[#0A2A1B]"
                >
                  <MessageCircle className="w-4 h-4 text-[#E6A817]" />
                  <span>Send Order to Store WhatsApp</span>
                </a>
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="py-2.5 px-4 rounded-xl border border-[#0F3D28]/20 text-xs font-bold text-[#14221B] inline-flex items-center justify-center gap-1.5 hover:bg-[#F8F7F2] cursor-pointer"
                >
                  <Printer className="w-4 h-4" />
                  <span>Print Bill</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {items.length > 0 && step !== 'receipt' && (
          <div className="p-5 bg-white border-t border-[#0F3D28]/12 space-y-3 no-print">
            <div className="space-y-1 text-xs tabular-nums">
              <div className="flex justify-between text-[#526058]">
                <span>MRP Total</span>
                <span className="line-through">₹{subtotalMrp}</span>
              </div>
              <div className="flex justify-between text-[#0F3D28] font-semibold">
                <span>Wholesale Discount (12% Off)</span>
                <span>-₹{totalSavings}</span>
              </div>
              <div className="flex justify-between text-[#526058]">
                <span>Delivery Fee</span>
                <span>{deliveryFee === 0 ? 'FREE' : `₹${deliveryFee}`}</span>
              </div>
              <div className="flex justify-between text-base font-bold text-[#14221B] pt-1 border-t border-[#0F3D28]/10">
                <span>Total Payable</span>
                <span>₹{totalPayable}</span>
              </div>
            </div>

            <div className="flex items-center gap-2.5">
              {step !== 'cart' && (
                <button
                  type="button"
                  onClick={() => setStep(step === 'payment' ? 'details' : 'cart')}
                  className="px-4 py-3 rounded-xl border border-[#0F3D28]/20 text-sm font-semibold text-[#14221B] inline-flex items-center gap-1 cursor-pointer"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Back</span>
                </button>
              )}

              {step === 'cart' && (
                <button
                  type="button"
                  onClick={() => {
                    setErrorMsg('');
                    setStep('details');
                  }}
                  className="flex-1 py-3 px-5 rounded-xl bg-[#0F3D28] text-white font-semibold text-sm hover:bg-[#0A2A1B] inline-flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span>Proceed to Delivery &amp; Slot</span>
                  <ArrowRight className="w-4 h-4 text-[#E6A817]" />
                </button>
              )}

              {step === 'details' && (
                <button
                  type="button"
                  onClick={() => {
                    if (!customerName.trim() || customerPhone.replace(/\D/g, '').length < 10) {
                      setErrorMsg('Please enter your name and valid 10-digit WhatsApp number.');
                      return;
                    }
                    if (fulfillment === 'delivery' && !address.trim()) {
                      setErrorMsg('Please enter your delivery address in Sandhaur / nearby village.');
                      return;
                    }
                    setErrorMsg('');
                    setStep('payment');
                  }}
                  className="flex-1 py-3 px-5 rounded-xl bg-[#0F3D28] text-white font-semibold text-sm hover:bg-[#0A2A1B] inline-flex items-center justify-center gap-2 cursor-pointer"
                >
                  <span>Continue to Payment</span>
                  <ArrowRight className="w-4 h-4 text-[#E6A817]" />
                </button>
              )}

              {step === 'payment' && (
                <button
                  type="button"
                  disabled={submitting}
                  onClick={handleConfirmOrder}
                  className="flex-1 py-3 px-5 rounded-xl bg-[#E6A817] text-[#14221B] font-bold text-sm hover:bg-[#f0b72b] inline-flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60"
                >
                  <span>{submitting ? 'Placing Order...' : `Confirm Order · ₹${totalPayable}`}</span>
                  <CheckCircle2 className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
