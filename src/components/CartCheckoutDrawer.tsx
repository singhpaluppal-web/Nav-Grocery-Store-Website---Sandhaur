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
  Landmark,
  MessageCircle,
  CheckCircle2,
  Printer,
  Truck,
  Store,
  PackageCheck,
  Copy,
  Check,
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
  shippingZone?: string;
  paymentSettings: PaymentSettings;
  customerSession: CustomerSession | null;
  onClose: () => void;
  onUpdateQty: (productId: string, qty: number) => void;
  onRemoveItem: (productId: string) => void;
  onClearCart: () => void;
  onChangeFulfillment: (mode: FulfillmentMode) => void;
  onTrackOrder?: (orderId: string) => void;
}

interface RazorpayInstance {
  open: () => void;
}

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => RazorpayInstance;
  }
}

function loadRazorpaySdk(): Promise<boolean> {
  if (typeof window === 'undefined') return Promise.resolve(false);
  if (window.Razorpay) return Promise.resolve(true);
  return new Promise((resolve) => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => resolve(Boolean(window.Razorpay));
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export const CartCheckoutDrawer: React.FC<CartCheckoutDrawerProps> = ({
  isOpen,
  logoUrl,
  items,
  fulfillment,
  shippingZone = 'Sandhaur 148020 Local',
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
    paymentSettings.pickupSlots[0] || '15-Min Express Delivery (Sandhaur 148020)'
  );
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>(
    customerSession?.preferredPayment || 'upi'
  );
  const [utrRef, setUtrRef] = useState('');
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [confirmedOrder, setConfirmedOrder] = useState<OrderRecord | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [copiedBank, setCopiedBank] = useState(false);

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
  const gstIncluded = Math.round((subtotalSale * 5) / 105);
  const deliveryFee =
    fulfillment === 'pickup' || subtotalSale >= paymentSettings.freeDeliveryAbove || subtotalSale === 0
      ? 0
      : paymentSettings.deliveryFee;
  const totalPayable = subtotalSale + deliveryFee;

  const merchantVpa = (paymentSettings.upiId || '8847370905@paytm').trim();
  const merchantName = (paymentSettings.upiMerchantName || 'NavGroceryStore').trim();
  const upiQuery = `pa=${encodeURIComponent(merchantVpa)}&pn=${encodeURIComponent(
    merchantName
  )}&am=${totalPayable.toFixed(2)}&cu=INR&tn=${encodeURIComponent('Nav Grocery & Nav Kart Order')}`;

  const upiUri = `upi://pay?${upiQuery}`;
  const gpayUri = `tez://upi/pay?${upiQuery}`;
  const phonepeUri = `phonepe://pay?${upiQuery}`;
  const paytmUri = `paytmmp://pay?${upiQuery}`;
  const bhimUri = `upi://pay?${upiQuery}`;

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

  const buildOrderRecord = (
    statusLabel: OrderRecord['paymentStatus'],
    overrideMethod?: PaymentMethod,
    razorpayPaymentId?: string
  ): OrderRecord => {
    const orderId = `NGS-${new Date().toISOString().slice(2, 10).replace(/-/g, '')}-${Math.floor(
      1000 + Math.random() * 9000
    )}`;
    const finalMethod = overrideMethod || paymentMethod;
    return {
      orderId,
      createdAt: new Date().toISOString(),
      customerName: customerName.trim(),
      customerPhone: customerPhone.replace(/\D/g, '').slice(-10),
      address:
        fulfillment === 'pickup'
          ? '30-Min Store Pickup — Main Bazaar, Sandhaur 148020'
          : `${address.trim()} (${shippingZone})`,
      fulfillment,
      shippingZone,
      slot,
      paymentMethod: finalMethod,
      paymentStatus: statusLabel,
      utrRef: utrRef.trim() || undefined,
      razorpayPaymentId,
      subtotalMrp,
      subtotalSale,
      gstTotal: gstIncluded,
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

  const finalizeOrder = async (
    statusLabel: OrderRecord['paymentStatus'],
    overrideMethod?: PaymentMethod,
    razorpayPaymentId?: string
  ) => {
    setSubmitting(true);
    const order = buildOrderRecord(statusLabel, overrideMethod, razorpayPaymentId);
    await saveOrder(order);
    setConfirmedOrder(order);
    onClearCart();
    setSubmitting(false);
    setStep('receipt');
  };

  const handleConfirmOrder = async () => {
    setErrorMsg('');
    if ((paymentMethod === 'upi' || paymentMethod === 'bank_transfer') && utrRef.trim().length < 6) {
      setErrorMsg(
        paymentMethod === 'upi'
          ? 'Please enter your 12-digit UPI Transaction / UTR Reference after scanning QR or paying in app.'
          : 'Please enter your NEFT / IMPS UTR Reference number after transferring.'
      );
      return;
    }
    if (paymentMethod === 'cod' && !codAllowed) {
      setErrorMsg(`Cash on Delivery requires minimum order of ₹${paymentSettings.codMinOrder}.`);
      return;
    }

    if (paymentMethod === 'razorpay') {
      await handleRazorpayTrigger();
      return;
    }

    const paymentStatus: OrderRecord['paymentStatus'] =
      paymentMethod === 'upi'
        ? 'Paid / UTR Submitted'
        : paymentMethod === 'bank_transfer'
        ? 'Bank Transfer / UTR Submitted'
        : fulfillment === 'pickup'
        ? 'Pay at Pickup'
        : 'Pay on Delivery';

    await finalizeOrder(paymentStatus);
  };

  const handleRazorpayTrigger = async () => {
    setErrorMsg('');
    const keyId = (paymentSettings.razorpayKeyId || '').trim();
    if (!keyId) {
      setErrorMsg(
        'Razorpay Key ID is not yet set in Admin Settings. Choose UPI Direct Pay, NEFT/IMPS Bank Transfer, or Cash on Delivery.'
      );
      return;
    }

    setSubmitting(true);
    const loaded = await loadRazorpaySdk();
    setSubmitting(false);

    if (!loaded || !window.Razorpay) {
      setErrorMsg('Could not load Razorpay Checkout SDK. Please use UPI QR or NEFT/IMPS.');
      return;
    }

    const rzp = new window.Razorpay({
      key: keyId,
      amount: Math.round(totalPayable * 100),
      currency: 'INR',
      name: 'Nav Grocery Store & Nav Kart',
      description: `Main Bazaar, Sandhaur 148020 · ${items.length} SKU(s)`,
      prefill: {
        name: customerName.trim(),
        contact: `+91${customerPhone.replace(/\D/g, '').slice(-10)}`,
      },
      theme: { color: '#0F3D28' },
      handler: (response: { razorpay_payment_id?: string }) => {
        void finalizeOrder('Gateway Paid', 'razorpay', response.razorpay_payment_id);
      },
    });
    rzp.open();
  };

  const handleCopyBankDetails = () => {
    const details = [
      `Account Name: ${paymentSettings.bankAccountName || 'Nav Grocery Store Sandhaur'}`,
      `Account No: ${paymentSettings.bankAccountNumber || '1480208847370905'}`,
      `IFSC: ${paymentSettings.bankIfsc || 'PUNB0148020'}`,
      `Bank: ${paymentSettings.bankName || 'Punjab National Bank (Main Bazaar, Sandhaur)'}`,
      `Amount: ₹${totalPayable}`,
    ].join('\n');
    navigator.clipboard?.writeText(details);
    setCopiedBank(true);
    setTimeout(() => setCopiedBank(false), 2000);
  };

  const getWhatsAppReceiptUrl = (order: OrderRecord) => {
    const lines = [
      `*Nav Grocery Store & Nav Kart — Order Receipt*`,
      `Main Bazaar, Sandhaur, Punjab 148020`,
      `Order ID: *${order.orderId}*`,
      `Customer: ${order.customerName} (+91 ${order.customerPhone})`,
      `Mode: ${order.fulfillment === 'pickup' ? '30-Min Store Pickup' : 'Delivery'} (${order.slot})`,
      `Address: ${order.address}`,
      `Payment: ${order.paymentMethod.toUpperCase()} — ${order.paymentStatus}${
        order.utrRef ? ` (UTR: ${order.utrRef})` : ''
      }${order.razorpayPaymentId ? ` (RZP: ${order.razorpayPaymentId})` : ''}`,
      ``,
      `*Items:*`,
      ...order.items.map(
        (it, idx) => `${idx + 1}. ${it.name} x${it.qty} = ₹${it.sale * it.qty}`
      ),
      ``,
      `Total Savings (12% Off): ₹${order.savings}`,
      `Included GST (5%): ₹${order.gstTotal ?? 0}`,
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
            <div className="flex items-center gap-2">
              <h2 className="font-display font-bold text-lg">
                {step === 'cart' && `Your Basket (${items.reduce((a, b) => a + b.quantity, 0)})`}
                {step === 'details' && 'Delivery & Pickup Details'}
                {step === 'payment' && 'Indian Checkout & Payment'}
                {step === 'receipt' && 'Order Confirmed — GST Bill'}
              </h2>
              {step === 'cart' && items.length > 0 && (
                <span className="px-2 py-0.5 rounded bg-[#E6A817] text-[#14221B] text-[10px] font-bold uppercase tracking-wide">
                  12% Off Applied
                </span>
              )}
            </div>
            <p className="text-xs text-white/75">
              Nav Grocery Store &amp; Nav Kart · {shippingZone}
            </p>
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
                    Add sealed packaged groceries, -18°C frozen essentials, electronics, or apparel at flat 12% off MRP.
                  </p>
                  <button
                    type="button"
                    onClick={onClose}
                    className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold cursor-pointer"
                  >
                    Browse 320+ Verified SKUs
                  </button>
                </div>
              ) : (
                <>
                  <div className="bg-white rounded-xl p-3 border border-[#0F3D28]/10 flex items-center justify-between text-xs">
                    <span className="font-semibold text-[#0F3D28]">
                      {subtotalSale >= paymentSettings.freeDeliveryAbove
                        ? 'Unlocked: Free Local Delivery in Sandhaur 148020'
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
                    <p className="text-xs font-bold">Express / Local Delivery</p>
                    <p className="text-[11px] opacity-80">{shippingZone}</p>
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
                    Preferred Delivery / Pickup Window
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
              {/* 1. UPI Dynamic QR + GPay / PhonePe / Paytm / BHIM Intent */}
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
                        <p className="text-sm font-bold text-[#14221B]">UPI Instant Pay &amp; Dynamic QR</p>
                        <p className="text-xs text-[#526058]">
                          GPay, PhonePe, Paytm, BHIM · {merchantVpa}
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
                            className="w-36 h-36 rounded-xl border border-[#0F3D28]/15 p-1 bg-white shrink-0"
                          />
                        )}
                        <div className="flex-1 w-full space-y-2.5 text-xs">
                          <p className="font-semibold text-[#14221B]">
                            Scan QR or tap an app to pay ₹{totalPayable}
                          </p>
                          <div className="grid grid-cols-2 gap-1.5">
                            <a
                              href={gpayUri}
                              className="py-1.5 px-2.5 rounded-lg bg-[#0F3D28] text-white font-bold text-center hover:bg-[#0A2A1B]"
                            >
                              Google Pay
                            </a>
                            <a
                              href={phonepeUri}
                              className="py-1.5 px-2.5 rounded-lg bg-[#0F3D28] text-white font-bold text-center hover:bg-[#0A2A1B]"
                            >
                              PhonePe
                            </a>
                            <a
                              href={paytmUri}
                              className="py-1.5 px-2.5 rounded-lg bg-[#E6A817] text-[#14221B] font-bold text-center hover:bg-[#f0b72b]"
                            >
                              Paytm
                            </a>
                            <a
                              href={bhimUri}
                              className="py-1.5 px-2.5 rounded-lg bg-[#F8F7F2] border border-[#0F3D28]/25 text-[#0F3D28] font-bold text-center"
                            >
                              BHIM UPI
                            </a>
                          </div>
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

              {/* 2. Razorpay Checkout SDK (Cards, NetBanking, UPI Intent, Wallets) */}
              {paymentSettings.razorpayEnabled && (
                <div
                  className={`bg-white rounded-xl p-4 border ${
                    paymentMethod === 'razorpay' ? 'border-[#0F3D28] ring-1 ring-[#0F3D28]' : 'border-[#0F3D28]/15'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentMethod('razorpay');
                      setErrorMsg('');
                    }}
                    className="w-full flex items-center justify-between text-left cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5">
                      <CreditCard className="w-5 h-5 text-[#0F3D28]" />
                      <div>
                        <p className="text-sm font-bold text-[#14221B]">Razorpay Cards, NetBanking &amp; Wallets</p>
                        <p className="text-xs text-[#526058]">
                          Visa, Mastercard, RuPay, HDFC/SBI/PNB NetBanking
                        </p>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[#0F3D28] tabular-nums">₹{totalPayable}</span>
                  </button>
                  {paymentMethod === 'razorpay' && (
                    <div className="mt-3 pt-3 border-t border-[#0F3D28]/10 flex items-center justify-between gap-2 text-xs">
                      <span className="text-[#526058]">
                        {paymentSettings.razorpayKeyId.trim()
                          ? 'Encrypted Razorpay gateway ready in INR.'
                          : 'Configure Razorpay Key ID in Admin Payments tab to enable live card popup.'}
                      </span>
                      <button
                        type="button"
                        onClick={handleRazorpayTrigger}
                        className="px-3 py-1.5 rounded-lg bg-[#0F3D28] text-white font-bold shrink-0 cursor-pointer"
                      >
                        Launch Razorpay
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* 3. Direct Bank Transfer (NEFT / IMPS) */}
              {paymentSettings.bankTransferEnabled !== false && (
                <div
                  className={`bg-white rounded-xl p-4 border ${
                    paymentMethod === 'bank_transfer'
                      ? 'border-[#0F3D28] ring-1 ring-[#0F3D28]'
                      : 'border-[#0F3D28]/15'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentMethod('bank_transfer');
                      setErrorMsg('');
                    }}
                    className="w-full flex items-center justify-between text-left cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5">
                      <Landmark className="w-5 h-5 text-[#0F3D28]" />
                      <div>
                        <p className="text-sm font-bold text-[#14221B]">Bank Transfer (NEFT / IMPS)</p>
                        <p className="text-xs text-[#526058]">
                          Wholesale direct bank transfer + UTR verification
                        </p>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[#0F3D28] tabular-nums">₹{totalPayable}</span>
                  </button>

                  {paymentMethod === 'bank_transfer' && (
                    <div className="mt-3 pt-3 border-t border-[#0F3D28]/10 space-y-2.5 text-xs">
                      <div className="p-3 rounded-lg bg-[#F8F7F2] border border-[#0F3D28]/12 space-y-1 font-mono-num">
                        <div className="flex items-center justify-between">
                          <span className="font-sans font-bold text-[#14221B]">
                            {paymentSettings.bankName || 'Punjab National Bank (Main Bazaar, Sandhaur)'}
                          </span>
                          <button
                            type="button"
                            onClick={handleCopyBankDetails}
                            className="inline-flex items-center gap-1 text-[11px] font-sans font-bold text-[#0F3D28] cursor-pointer"
                          >
                            {copiedBank ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                            <span>{copiedBank ? 'Copied' : 'Copy'}</span>
                          </button>
                        </div>
                        <p>A/C Name: {paymentSettings.bankAccountName || 'Nav Grocery Store Sandhaur'}</p>
                        <p>A/C No: {paymentSettings.bankAccountNumber || '1480208847370905'}</p>
                        <p>IFSC: {paymentSettings.bankIfsc || 'PUNB0148020'}</p>
                      </div>
                      <div>
                        <label className="block text-[11px] font-semibold text-[#526058] mb-1">
                          Enter NEFT / IMPS UTR Reference *
                        </label>
                        <input
                          type="text"
                          value={utrRef}
                          onChange={(e) => setUtrRef(e.target.value)}
                          placeholder="Enter bank UTR / IMPS reference number"
                          className="w-full px-3 py-2 rounded-lg border border-[#0F3D28]/20 font-mono-num text-xs"
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* 4. Cash on Delivery / Pay at Store Pickup */}
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
                            ? 'Verified Sandhaur 148020 delivery + instant WhatsApp confirmation'
                            : `Minimum order ₹${paymentSettings.codMinOrder} required for COD`}
                        </p>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[#0F3D28] tabular-nums">₹{totalPayable}</span>
                  </button>
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
                    <h3 className="font-display font-bold text-base text-[#14221B]">
                      Nav Grocery Store &amp; Nav Kart
                    </h3>
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
                  <span>Included GST (5%)</span>
                  <span>₹{confirmedOrder.gstTotal ?? gstIncluded}</span>
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
                  <span>Confirm Order on WhatsApp</span>
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
                <span>Included GST (5%)</span>
                <span>₹{gstIncluded}</span>
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
                  <span>Continue to Indian Checkout</span>
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
