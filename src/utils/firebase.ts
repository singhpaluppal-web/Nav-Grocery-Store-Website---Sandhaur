import {
  BrandingSettings,
  OrderRecord,
  PaymentSettings,
  Product,
} from '../types';
import { isStrictlyGrocery308, normalizeWholesalePricing } from './aisles';
import { resolveProductImage } from './brandPhotos';

export const FIREBASE_RTDB_BASE = 'https://nav-grocery-store-default-rtdb.firebaseio.com';

const LOCAL_KEYS = {
  GROCERY: 'nav_public_grocery_308_v3',
  BRANDING: 'nav_public_branding_v3',
  PAYMENTS: 'nav_public_payments_v3',
  ORDERS: 'nav_public_orders_v3',
};

export const DEFAULT_BRANDING: BrandingSettings = {
  logoUrl: '/images/logo.png',
  storeTagline: 'Wholesale Kirana & Cold-Chain Frozen Store · Main Bazaar, Sandhaur 148020',
  announcementText: 'Flat 12% Off MRP on all 308 Grocery Items · 2-Hr Delivery in Sandhaur 148020 · 30-Min Main Bazaar Pickup',
  banners: [
    {
      id: 'banner-1',
      title: 'Flat 12% Off MRP on All 308 Grocery Essentials',
      subtitle: '100% sealed branded groceries, atta, basmati rice, desi ghee & spices from Main Bazaar, Sandhaur.',
      badge: 'WHOLESALE SAVINGS',
      ctaLabel: 'Shop 12% Off Deals',
      targetAisle: 'deals',
      imageUrl: '/images/store/hero_shopping_family.jpg',
      active: true,
    },
    {
      id: 'banner-2',
      title: 'Cold-Chain Frozen Veg & Snacks at -18°C',
      subtitle: 'Safal Green Peas, Sweet Corn, McCain Fries, Amul & Mother Dairy Paneer kept strictly frozen.',
      badge: 'COLD CHAIN -18°C',
      ctaLabel: 'Explore Frozen Aisle',
      targetAisle: 'frozen',
      imageUrl: '/images/store/aisle_snacks.jpg',
      active: true,
    },
    {
      id: 'banner-3',
      title: '2-Hour Sandhaur Delivery & 30-Min Store Pickup',
      subtitle: 'Order online with Dynamic UPI QR, COD or Razorpay and collect express at Main Bazaar, Sandhaur.',
      badge: 'SANDHAUR 148020 EXPRESS',
      ctaLabel: 'Browse Atta & Oils',
      targetAisle: 'atta',
      imageUrl: '/images/store/aisle_beverages.jpg',
      active: true,
    },
  ],
};

export const DEFAULT_PAYMENTS: PaymentSettings = {
  upiEnabled: true,
  upiId: '8847370905@ybl',
  upiMerchantName: 'Nav Grocery Store',
  codEnabled: true,
  codMinOrder: 199,
  deliveryFee: 30,
  freeDeliveryAbove: 499,
  razorpayEnabled: true,
  razorpayKeyId: '',
  pickupSlots: [
    '30-Min Store Pickup (Main Bazaar, Sandhaur)',
    '2-Hour Local Delivery (Sandhaur 148020)',
    'Morning Delivery (8:00 AM – 11:00 AM)',
    'Evening Delivery (4:00 PM – 8:00 PM)',
  ],
};

function parseArrayResponse(raw: unknown): Product[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw.filter((item): item is Product => Boolean(item && item.id));
  }
  if (typeof raw === 'object') {
    return Object.values(raw as Record<string, Product>).filter((item): item is Product => Boolean(item && item.id));
  }
  return [];
}

async function fetchJsonWithTimeout(url: string, timeoutMs = 6000): Promise<unknown> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, { signal: controller.signal, cache: 'no-store' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Fetches strictly NAV001..NAV308 from Firebase RTDB and locks every product
 * by immutable SKU ID against our verified 308-SKU catalog so photos never shift.
 */
export async function fetchGroceryProducts(): Promise<Product[]> {
  let localCatalog: Product[] = [];
  try {
    const localRes = await fetch('/data/products_308.json');
    localCatalog = parseArrayResponse(await localRes.json())
      .filter(isStrictlyGrocery308)
      .map(normalizeWholesalePricing);
  } catch {
    localCatalog = [];
  }

  const localSkuMap = new Map<string, Product>(localCatalog.map((p) => [p.id, p]));

  try {
    const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/products.json`);
    const remoteList = parseArrayResponse(remote)
      .filter(isStrictlyGrocery308)
      .map(normalizeWholesalePricing);

    if (remoteList.length > 0) {
      const mergedMap = new Map<string, Product>();
      for (const item of remoteList) {
        const localVerified = localSkuMap.get(item.id);
        const merged: Product = {
          ...(localVerified || {}),
          ...item,
          // Always resolve SKU-locked photo instead of trusting shifted batch_* paths
          image: resolveProductImage({ ...(localVerified || {}), ...item }),
        };
        mergedMap.set(item.id, merged);
      }

      const sorted = Array.from(mergedMap.values()).sort((a, b) =>
        a.id.localeCompare(b.id, undefined, { numeric: true })
      );
      localStorage.setItem(LOCAL_KEYS.GROCERY, JSON.stringify(sorted));
      return sorted;
    }
  } catch {
    // Fall back to local cached or bundled 308 verified catalog
  }

  try {
    const cached = localStorage.getItem(LOCAL_KEYS.GROCERY);
    if (cached) {
      const parsed = parseArrayResponse(JSON.parse(cached))
        .filter(isStrictlyGrocery308)
        .map(normalizeWholesalePricing);
      if (parsed.length > 0) {
        return parsed.map((p) => ({ ...p, image: resolveProductImage(p) }));
      }
    }
  } catch {
    // ignore storage parse failure
  }

  return localCatalog.map((p) => ({ ...p, image: resolveProductImage(p) }));
}

export async function fetchBrandingSettings(): Promise<BrandingSettings> {
  try {
    const remote = (await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/settings/branding.json`, 4500)) as Partial<BrandingSettings> | null;
    if (remote && typeof remote === 'object') {
      // Filter out any NavKart banners so public storefront remains 100% pure Nav Grocery Store
      const safeBanners = Array.isArray(remote.banners)
        ? remote.banners.filter((b) => {
            const txt = `${b.title || ''} ${b.badge || ''} ${b.ctaLabel || ''}`.toLowerCase();
            return b.active !== false && !txt.includes('navkart') && !txt.includes('nav mall');
          })
        : [];
      const merged: BrandingSettings = {
        ...DEFAULT_BRANDING,
        ...remote,
        banners: safeBanners.length > 0 ? safeBanners : DEFAULT_BRANDING.banners,
      };
      localStorage.setItem(LOCAL_KEYS.BRANDING, JSON.stringify(merged));
      return merged;
    }
  } catch {
    // fallback
  }
  return DEFAULT_BRANDING;
}

export async function fetchPaymentSettings(): Promise<PaymentSettings> {
  try {
    const remote = (await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/settings/payments.json`, 4500)) as Partial<PaymentSettings> | null;
    if (remote && typeof remote === 'object') {
      const merged = { ...DEFAULT_PAYMENTS, ...remote };
      localStorage.setItem(LOCAL_KEYS.PAYMENTS, JSON.stringify(merged));
      return merged;
    }
  } catch {
    // fallback
  }
  return DEFAULT_PAYMENTS;
}

export async function saveOrder(order: OrderRecord): Promise<{ ok: boolean }> {
  try {
    const existingRaw = localStorage.getItem(LOCAL_KEYS.ORDERS);
    const existing: OrderRecord[] = existingRaw ? JSON.parse(existingRaw) : [];
    const next = [order, ...existing.filter((o) => o.orderId !== order.orderId)];
    localStorage.setItem(LOCAL_KEYS.ORDERS, JSON.stringify(next));
  } catch {
    // ignore
  }

  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/orders/${encodeURIComponent(order.orderId)}.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(order),
    });
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
}

export function getLocalOrders(): OrderRecord[] {
  try {
    const raw = localStorage.getItem(LOCAL_KEYS.ORDERS);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed)
      ? parsed.filter((o): o is OrderRecord => Boolean(o && typeof o === 'object' && o.orderId))
      : [];
  } catch {
    return [];
  }
}

export async function fetchOrderById(orderId: string): Promise<OrderRecord | null> {
  const cleanId = orderId.trim().toUpperCase();
  if (!cleanId) return null;

  try {
    const remote = await fetchJsonWithTimeout(
      `${FIREBASE_RTDB_BASE}/orders/${encodeURIComponent(cleanId)}.json`,
      5000
    );
    if (remote && typeof remote === 'object' && 'orderId' in remote) {
      const order = remote as OrderRecord;
      try {
        const existing = getLocalOrders();
        const next = [order, ...existing.filter((o) => o.orderId.toUpperCase() !== cleanId)];
        localStorage.setItem(LOCAL_KEYS.ORDERS, JSON.stringify(next));
      } catch {
        // ignore storage error
      }
      return order;
    }
  } catch {
    // Fall back to locally cached orders on this device
  }

  const localMatch = getLocalOrders().find((o) => o.orderId.toUpperCase() === cleanId);
  return localMatch || null;
}
