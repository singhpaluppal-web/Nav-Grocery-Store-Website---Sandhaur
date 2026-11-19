export type FulfillmentMode = 'delivery' | 'pickup';

export type PaymentMethod = 'upi' | 'cod' | 'razorpay' | 'bank_transfer';

export type ServiceVertical = 'store' | 'supermall' | 'express' | 'essentials';

export interface PublishingChannels {
  navGroceryWeb: boolean;
  navKartWeb: boolean;
  androidApps: boolean;
}

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
  rating?: number;
  reviewCount?: number;
  image: string;
  imageUrl?: string;
  customImage?: string;
  description?: string;
  channels?: PublishingChannels;
  updatedAt?: string;
  cost?: number;
  sellerId?: string;
  sellerName?: string;
  approvalStatus?: string;
  isVerifiedSeller?: boolean;
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
  targetVertical?: ServiceVertical;
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
  bankTransferEnabled?: boolean;
  bankName?: string;
  bankAccountName?: string;
  bankAccountNumber?: string;
  bankIfsc?: string;
  bankBranch?: string;
  pickupSlots: string[];
}

export interface StoreStatusSettings {
  storeName: string;
  isOpen: boolean;
  hoursLabel: string;
  address: string;
  phones: string[];
  dispatchNotice?: string;
  updatedAt?: string;
}

export interface StoreConfig {
  storeName: string;
  isOpen: boolean;
  hoursLabel: string;
  address: string;
  phones: string[];
  announcementText: string;
  deliveryFee: number;
  freeDeliveryAbove: number;
  codMinOrder: number;
  expressDeliveryEnabled: boolean;
  pickupEnabled: boolean;
  deliveryZones: string[];
  updatedAt?: string;
}

export interface SavedAddress {
  id: string;
  label: string;
  recipient: string;
  phone: string;
  line1: string;
  pincode: string;
  isDefault?: boolean;
}

export interface CustomerSession {
  uid: string;
  email: string;
  photoURL?: string;
  phone: string;
  name: string;
  villageArea?: string;
  address: string;
  profileComplete?: boolean;
  verifiedAt: number;
  updatedAt?: string;
  membershipTier?: 'Nav Pass Wholesale' | 'Standard Member';
  savedAddresses?: SavedAddress[];
  preferredPayment?: PaymentMethod;
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
  source?: 'web' | 'android' | string;
  uid?: string;
  customerEmail?: string;
  customerPhoto?: string;
  createdAt: string;
  updatedAt?: string;
  customerName: string;
  customerPhone: string;
  address: string;
  fulfillment: FulfillmentMode;
  shippingZone?: string;
  slot: string;
  paymentMethod: PaymentMethod;
  paymentStatus:
    | 'Paid / UTR Submitted'
    | 'UTR Verified'
    | 'Pay on Delivery'
    | 'Pay at Pickup'
    | 'Gateway Paid'
    | 'Bank Transfer / UTR Submitted';
  utrRef?: string;
  razorpayPaymentId?: string;
  subtotalMrp: number;
  subtotalSale: number;
  gstTotal?: number;
  savings: number;
  deliveryFee: number;
  totalPayable: number;
  status: OrderStatus;
  items: OrderItemSummary[];
}

export interface ReturnRequest {
  requestId: string;
  orderId: string;
  customerPhone: string;
  reason: string;
  notes?: string;
  status: 'Submitted' | 'Reviewing' | 'Approved' | 'Replaced';
  createdAt: string;
}
