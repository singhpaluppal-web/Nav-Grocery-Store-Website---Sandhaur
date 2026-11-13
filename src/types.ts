export type FulfillmentMode = 'delivery' | 'pickup';

export type PaymentMethod = 'upi' | 'cod' | 'razorpay';

export interface Product {
  id: string;
  name: string;
  brand: string;
  category: string;
  department: string;
  mrp: number;
  sale: number;
  price?: number;
  stock: number;
  gst?: string;
  barcode?: string;
  image: string;
  customImage?: string;
  description?: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface HeroBanner {
  id: string;
  title: string;
  subtitle: string;
  badge: string;
  ctaLabel: string;
  targetAisle: string;
  imageUrl: string;
  active: boolean;
}

export interface BrandingSettings {
  logoUrl: string;
  storeTagline: string;
  announcementText: string;
  banners: HeroBanner[];
}

export interface PaymentSettings {
  upiEnabled: boolean;
  upiId: string;
  upiMerchantName: string;
  codEnabled: boolean;
  codMinOrder: number;
  deliveryFee: number;
  freeDeliveryAbove: number;
  razorpayEnabled: boolean;
  razorpayKeyId: string;
  pickupSlots: string[];
}

export interface CustomerSession {
  phone: string;
  name: string;
  address: string;
  verifiedAt: number;
}

export interface OrderItemSummary {
  id: string;
  name: string;
  brand: string;
  qty: number;
  mrp: number;
  sale: number;
  gst: string;
}

export type OrderStatus =
  | 'Pending'
  | 'Confirmed'
  | 'Packed'
  | 'Out for Delivery'
  | 'Ready for Pickup'
  | 'Delivered'
  | 'Picked Up'
  | 'Cancelled'
  | string;

export interface OrderRecord {
  orderId: string;
  createdAt: string;
  updatedAt?: string;
  customerName: string;
  customerPhone: string;
  address: string;
  fulfillment: FulfillmentMode;
  slot: string;
  paymentMethod: PaymentMethod;
  paymentStatus: 'Paid / UTR Submitted' | 'Pay on Delivery' | 'Pay at Pickup' | 'Gateway Paid';
  utrRef?: string;
  subtotalMrp: number;
  subtotalSale: number;
  savings: number;
  deliveryFee: number;
  totalPayable: number;
  status: OrderStatus;
  items: OrderItemSummary[];
}
