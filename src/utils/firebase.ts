import { get, onValue, ref, set } from 'firebase/database';
import {
  BrandingSettings,
  CustomerSession,
  OrderRecord,
  PaymentSettings,
  Product,
  ReturnRequest,
} from '../types';
import { FIREBASE_DATABASE_URL, firebaseDb } from '../firebase';
import {
  isAllowedRetailProduct,
  isStrictlyGrocery308,
  normalizeWholesalePricing,
} from './aisles';
import { NAVKART_SUPERMALL_SKUS } from '../data/navKartSupermall';
import { getCanonicalSkuImagePath, resolveProductImage } from './brandPhotos';

export const FIREBASE_RTDB_BASE = FIREBASE_DATABASE_URL;

export const INDIAN_PHONE_REGEX = /^[6-9]\d{9}$/;

export function isCustomerProfileComplete(
  profile?: Partial<CustomerSession> | null
): boolean {
  if (!profile) return false;
  const phone = String(profile.phone || '').replace(/\D/g, '');
  const address = String(profile.address || '').trim();
  const name = String(profile.name || '').trim();
  return INDIAN_PHONE_REGEX.test(phone) && address.length >= 5 && name.length >= 2;
}

const LOCAL_KEYS = {
  GROCERY: 'nav_public_grocery_308_v3',
  UNIFIED: 'nav_unified_catalog_v1',
  BRANDING: 'nav_public_branding_v3',
  PAYMENTS: 'nav_public_payments_v4',
  ORDERS: 'nav_public_orders_v3',
  RETURNS: 'nav_public_returns_v1',
};

export const DEFAULT_BRANDING: BrandingSettings = {
  logoUrl: '/images/logo.png',
  storeTagline: 'Nav Grocery Store & Nav Kart · Main Bazaar, Sandhaur 148020',
  announcementText:
    'Nav Grocery & Nav Kart · Flat 12% Off MRP on 320+ Verified SKUs · 15-Min Express & 2-Hr Delivery in Sandhaur 148020',
  banners: [
    {
      id: 'banner-1',
      title: 'Yellow Friday Wholesale Sale: Flat 12% Off MRP on All 308+ SKUs',
      subtitle:
        '100% factory-sealed pantry staples, pure cow desi ghee, electronics & apparel from Main Bazaar, Sandhaur.',
      badge: 'YELLOW FRIDAY WHOLESALE',
      ctaLabel: 'Shop 12% Off Deals',
      targetAisle: 'deals',
      targetVertical: 'store',
      imageUrl: '/images/store/hero_shopping_family.jpg',
      active: true,
    },
    {
      id: 'banner-2',
      title: 'Cold-Chain Frozen Veg & Snacks at -18°C',
      subtitle:
        'Safal Green Peas, Sweet Corn, McCain Fries, Amul & Mother Dairy Paneer kept strictly frozen.',
      badge: 'COLD CHAIN -18°C',
      ctaLabel: 'Explore Frozen Aisle',
      targetAisle: 'frozen',
      targetVertical: 'essentials',
      imageUrl: '/images/store/aisle_snacks.jpg',
      active: true,
    },
    {
      id: 'banner-3',
      title: 'Nav Kart Supermall: Electronics, Appliances & Apparel',
      subtitle:
        'Audio, LED lighting, kitchen appliances and cotton kurtas — wholesale 12% off MRP with instant 15-min Express or store pickup.',
      badge: 'NAV KART SUPERMALL',
      ctaLabel: 'Explore Supermall',
      targetAisle: 'electronics',
      targetVertical: 'supermall',
      imageUrl: '/images/store/aisle_beverages.jpg',
      active: true,
    },
  ],
};

export const DEFAULT_PAYMENTS: PaymentSettings = {
  upiEnabled: true,
  upiId: '8847370905@paytm',
  upiMerchantName: 'NavGroceryStore',
  codEnabled: true,
  codMinOrder: 199,
  deliveryFee: 30,
  freeDeliveryAbove: 499,
  razorpayEnabled: true,
  razorpayKeyId: '',
  bankTransferEnabled: true,
  bankName: 'Punjab National Bank (Main Bazaar, Sandhaur)',
  bankAccountName: 'Nav Grocery Store Sandhaur',
  bankAccountNumber: '1480208847370905',
  bankIfsc: 'PUNB0148020',
  bankBranch: 'Main Bazaar, Sandhaur, Malerkotla 148020',
  pickupSlots: [
    '15-Min Express Delivery (Sandhaur 148020)',
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

function parseOrdersResponse(raw: unknown): OrderRecord[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw.filter((item): item is OrderRecord => Boolean(item && item.orderId));
  }
  if (typeof raw === 'object') {
    return Object.values(raw as Record<string, OrderRecord>).filter(
      (item): item is OrderRecord => Boolean(item && typeof item === 'object' && item.orderId)
    );
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

function isPlaceholderProductName(name?: string): boolean {
  const clean = String(name || '').trim();
  if (!clean) return true;
  return /^(?:grocery\s*item|sample\s*item|test\s*item|item)\s*#?\d*$/i.test(clean);
}

async function fetchStaticCatalogJson(): Promise<Product[]> {
  for (const path of ['/catalog.json', '/data/products_308.json']) {
    try {
      const res = await fetch(path, { cache: 'no-store' });
      if (!res.ok) continue;
      const list = parseArrayResponse(await res.json())
        .filter((p) => isStrictlyGrocery308(p) && !isPlaceholderProductName(p.name))
        .map(normalizeWholesalePricing);
      if (list.length > 0) {
        return list.sort((a, b) => a.id.localeCompare(b.id, undefined, { numeric: true }));
      }
    } catch {
      // try next static catalog path
    }
  }
  return [];
}

/**
 * Fetches strictly NAV001..NAV308 from Firebase RTDB and locks every product
 * by immutable SKU ID against our verified 308-SKU catalog (public/catalog.json).
 */
export async function fetchGroceryProducts(): Promise<Product[]> {
  const localCatalog = await fetchStaticCatalogJson();
  const localSkuMap = new Map<string, Product>(localCatalog.map((p) => [p.id, p]));

  let rtdbReachable = false;
  try {
    const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/products.json`);
    rtdbReachable = true;
    const remoteList = parseArrayResponse(remote)
      .filter((p) => isStrictlyGrocery308(p) && !isPlaceholderProductName(p.name))
      .map(normalizeWholesalePricing);

    if (remoteList.length > 0) {
      const mergedMap = new Map<string, Product>();
      for (const item of remoteList) {
        const localVerified = localSkuMap.get(item.id);
        const merged: Product = {
          ...(localVerified || {}),
          ...item,
          name: !isPlaceholderProductName(item.name) ? item.name : (localVerified?.name || item.name),
          image: resolveProductImage({ ...(localVerified || {}), ...item }),
        };
        mergedMap.set(item.id, merged);
      }

      const sorted = Array.from(mergedMap.values()).sort((a, b) =>
        a.id.localeCompare(b.id, undefined, { numeric: true })
      );
      try {
        localStorage.setItem(LOCAL_KEYS.GROCERY, JSON.stringify(sorted));
      } catch {
        // ignore quota errors
      }
      return sorted;
    }
  } catch {
    rtdbReachable = false;
  }

  if (localCatalog.length > 0) {
    return localCatalog.map((p) => ({ ...p, image: resolveProductImage(p) }));
  }

  if (!rtdbReachable) {
    try {
      const cached = localStorage.getItem(LOCAL_KEYS.GROCERY);
      if (cached) {
        const parsed = parseArrayResponse(JSON.parse(cached))
          .filter((p) => isStrictlyGrocery308(p) && !isPlaceholderProductName(p.name))
          .map(normalizeWholesalePricing);
        if (parsed.length > 0) {
          return parsed.map((p) => ({ ...p, image: resolveProductImage(p) }));
        }
      }
    } catch {
      // ignore storage parse failure
    }
  }

  return [];
}

/**
 * Fetches unified Nav Grocery (308 SKUs) + Nav Kart Supermall (Electronics, Kitchen Appliances, Apparel)
 * strictly filtering out any banned fresh produce items.
 */
export async function fetchUnifiedCatalogProducts(): Promise<Product[]> {
  const localGrocery = await fetchStaticCatalogJson();
  const baseById = new Map<string, Product>();

  for (const p of localGrocery) {
    baseById.set(p.id, { ...p, image: resolveProductImage(p) });
  }
  for (const sm of NAVKART_SUPERMALL_SKUS) {
    baseById.set(sm.id, { ...sm, image: resolveProductImage(sm) });
  }

  try {
    const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/products.json`);
    const remoteList = parseArrayResponse(remote)
      .filter((p) => isAllowedRetailProduct(p) && !isPlaceholderProductName(p.name))
      .map(normalizeWholesalePricing);

    if (remoteList.length > 0) {
      for (const item of remoteList) {
        const seed = baseById.get(item.id);
        const merged: Product = {
          ...(seed || {}),
          ...item,
          name: !isPlaceholderProductName(item.name) ? item.name : (seed?.name || item.name),
          image: resolveProductImage({ ...(seed || {}), ...item }),
        };
        baseById.set(item.id, merged);
      }
    }
  } catch {
    // Offline fallback uses verified static + supermall catalog seamlessly
  }

  const unified = Array.from(baseById.values()).sort((a, b) =>
    a.id.localeCompare(b.id, undefined, { numeric: true })
  );

  try {
    localStorage.setItem(LOCAL_KEYS.UNIFIED, JSON.stringify(unified.slice(0, 340)));
  } catch {
    // ignore quota limits
  }

  return unified;
}

export async function fetchVerified308Catalog(): Promise<Product[]> {
  const list = await fetchStaticCatalogJson();
  return list.map((normalized) => ({
    ...normalized,
    image: normalized.image || getCanonicalSkuImagePath(normalized.id),
    channels: normalized.channels || {
      navGroceryWeb: true,
      navKartWeb: true,
      androidApps: true,
    },
  }));
}

export async function fetchAllFirebaseProductsRaw(): Promise<{
  products: Product[];
  connected: boolean;
}> {
  try {
    const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/products.json`, 10000);
    const rawList = parseArrayResponse(remote)
      .filter((item) => !isPlaceholderProductName(item.name))
      .map((item) => {
        const sku = String(item.id || '').trim().toUpperCase();
        const mrp = Math.max(1, Math.round(Number(item.mrp) || 0));
        const sale = Math.round(mrp * 0.88);
        const rawCat = String(item.category || '').trim();
        const safeCategory =
          rawCat && !/^-?\d+(?:\.\d+)?%?$/.test(rawCat)
            ? rawCat
            : String(item.department || '').toLowerCase().includes('frozen')
            ? 'Frozen Foods'
            : 'Grocery & Staples';
        return {
          ...item,
          id: sku,
          category: safeCategory,
          mrp,
          sale,
          price: sale,
          stock: Number.isFinite(Number(item.stock)) ? Number(item.stock) : 25,
          channels: item.channels || {
            navGroceryWeb: true,
            navKartWeb: true,
            androidApps: true,
          },
          image: resolveProductImage({ ...item, id: sku }),
        };
      });
    return {
      products: rawList.sort((a, b) => a.id.localeCompare(b.id, undefined, { numeric: true })),
      connected: true,
    };
  } catch {
    return { products: [], connected: false };
  }
}

export async function syncAndroidFeedSnapshot(products: Product[]): Promise<boolean> {
  try {
    const androidItems = products
      .filter((p) => p.channels?.androidApps !== false)
      .map((p) => {
        const mrp = Math.max(1, Math.round(Number(p.mrp) || 0));
        const sale = Math.round(mrp * 0.88);
        const resolvedImg = resolveProductImage(p);
        return {
          id: p.id,
          name: p.name,
          brand: p.brand,
          category: p.category,
          department: p.department,
          mrp,
          sale,
          price: sale,
          stock: Number(p.stock ?? 0),
          gst: p.gst || '5%',
          barcode: p.barcode || '',
          image: resolvedImg,
          ...(p.customImage && p.customImage !== resolvedImg ? { customImage: p.customImage } : {}),
          channels: p.channels || {
            navGroceryWeb: true,
            navKartWeb: true,
            androidApps: true,
          },
          updatedAt: p.updatedAt || new Date().toISOString(),
        };
      });

    const payload = {
      store: 'Nav Grocery Store & Nav Kart (Navo Hub)',
      updatedAt: new Date().toISOString(),
      totalItems: androidItems.length,
      items: androidItems,
    };

    const res = await fetch(`${FIREBASE_RTDB_BASE}/android_feed.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return res.ok;
  } catch {
    return false;
  }
}

export async function upsertProductInFirebase(product: Product): Promise<{ ok: boolean }> {
  const cleanSku = String(product.id || '').trim().toUpperCase();
  if (!cleanSku) return { ok: false };
  const mrp = Math.max(1, Math.round(Number(product.mrp) || 0));
  const sale = Math.round(mrp * 0.88);
  const payload: Product = {
    ...product,
    id: cleanSku,
    mrp,
    sale,
    price: sale,
    image: resolveProductImage({ ...product, id: cleanSku }),
    channels: product.channels || {
      navGroceryWeb: true,
      navKartWeb: true,
      androidApps: true,
    },
    updatedAt: new Date().toISOString(),
  };

  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/products/${encodeURIComponent(cleanSku)}.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
}

export async function deleteProductFromFirebase(productId: string): Promise<{ ok: boolean }> {
  const cleanSku = String(productId || '').trim().toUpperCase();
  if (!cleanSku) return { ok: false };
  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/products/${encodeURIComponent(cleanSku)}.json`, {
      method: 'DELETE',
    });
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
}

export async function patchProductsInFirebase(
  updatesBySku: Record<string, Product | null>
): Promise<{ ok: boolean; error?: string }> {
  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/products.json`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updatesBySku),
    });
    if (!res.ok) {
      let detail = '';
      try {
        detail = (await res.text()).slice(0, 140);
      } catch {
        detail = '';
      }
      return { ok: false, error: `HTTP ${res.status}${detail ? `: ${detail}` : ''}` };
    }
    return { ok: true };
  } catch (err) {
    return {
      ok: false,
      error: err instanceof Error ? err.message : 'Network error during Firebase PATCH',
    };
  }
}

export async function replaceAllProductsInFirebase(products: Product[]): Promise<{ ok: boolean }> {
  const keyed: Record<string, Product> = {};
  const now = new Date().toISOString();
  for (const p of products) {
    const cleanSku = String(p.id || '').trim().toUpperCase();
    if (!cleanSku) continue;
    const mrp = Math.max(1, Math.round(Number(p.mrp) || 0));
    const sale = Math.round(mrp * 0.88);
    keyed[cleanSku] = {
      ...p,
      id: cleanSku,
      mrp,
      sale,
      price: sale,
      image: resolveProductImage({ ...p, id: cleanSku }),
      channels: p.channels || {
        navGroceryWeb: true,
        navKartWeb: true,
        androidApps: true,
      },
      updatedAt: now,
    };
  }
  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/products.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(keyed),
    });
    if (res.ok) {
      await syncAndroidFeedSnapshot(Object.values(keyed));
    }
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
}

export async function fetchBrandingSettings(): Promise<BrandingSettings> {
  try {
    const remote = (await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/settings/branding.json`, 4500)) as Partial<BrandingSettings> | null;
    if (remote && typeof remote === 'object') {
      const safeBanners = Array.isArray(remote.banners)
        ? remote.banners.filter((b) => b.active !== false)
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

export async function savePaymentSettingsToFirebase(settings: PaymentSettings): Promise<{ ok: boolean }> {
  try {
    localStorage.setItem(LOCAL_KEYS.PAYMENTS, JSON.stringify(settings));
  } catch {
    // ignore storage errors
  }
  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/settings/payments.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings),
    });
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
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

export async function fetchAllOrdersFromFirebase(): Promise<{
  orders: OrderRecord[];
  connected: boolean;
}> {
  const local = getLocalOrders();
  try {
    const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/orders.json`, 7000);
    const remoteOrders = parseOrdersResponse(remote);
    const byId = new Map<string, OrderRecord>();
    for (const o of local) {
      byId.set(o.orderId, o);
    }
    for (const o of remoteOrders) {
      byId.set(o.orderId, o);
    }
    const merged = Array.from(byId.values()).sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
    return { orders: merged, connected: true };
  } catch {
    return { orders: local, connected: false };
  }
}

export async function updateOrderInFirebase(
  orderId: string,
  patch: Partial<OrderRecord>
): Promise<{ ok: boolean }> {
  const cleanId = orderId.trim().toUpperCase();
  if (!cleanId) return { ok: false };
  const payload = { ...patch, updatedAt: new Date().toISOString() };

  try {
    const local = getLocalOrders();
    const updatedLocal = local.map((o) =>
      o.orderId.toUpperCase() === cleanId ? { ...o, ...payload } : o
    );
    localStorage.setItem(LOCAL_KEYS.ORDERS, JSON.stringify(updatedLocal));
  } catch {
    // ignore storage errors
  }

  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/orders/${encodeURIComponent(cleanId)}.json`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return { ok: res.ok };
  } catch {
    return { ok: false };
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

export function getLocalReturnRequests(): ReturnRequest[] {
  try {
    const raw = localStorage.getItem(LOCAL_KEYS.RETURNS);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export async function submitReturnRequest(req: ReturnRequest): Promise<{ ok: boolean }> {
  try {
    const existing = getLocalReturnRequests();
    const next = [req, ...existing.filter((r) => r.requestId !== req.requestId)];
    localStorage.setItem(LOCAL_KEYS.RETURNS, JSON.stringify(next));
  } catch {
    // ignore
  }

  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/returns/${encodeURIComponent(req.requestId)}.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
}

/**
 * Reads customer profile from Firebase RTDB at /customers/{uid}.json
 * (uses Firebase Database SDK when initialized, with direct REST fallback).
 */
export async function fetchCustomerProfile(
  uid: string
): Promise<Partial<CustomerSession> | null> {
  const cleanUid = String(uid || '').trim();
  if (!cleanUid) return null;

  if (firebaseDb) {
    try {
      const snap = await get(ref(firebaseDb, `customers/${cleanUid}`));
      if (snap.exists()) {
        return snap.val() as Partial<CustomerSession>;
      }
    } catch {
      // fall through to REST read
    }
  }

  try {
    const remote = await fetchJsonWithTimeout(
      `${FIREBASE_RTDB_BASE}/customers/${encodeURIComponent(cleanUid)}.json`,
      5500
    );
    if (remote && typeof remote === 'object') {
      return remote as Partial<CustomerSession>;
    }
  } catch {
    // offline or empty node
  }

  return null;
}

/**
 * Persists customer profile to Firebase RTDB at /customers/{uid}.json.
 */
export async function saveCustomerProfile(
  uid: string,
  profile: CustomerSession
): Promise<{ ok: boolean }> {
  const cleanUid = String(uid || '').trim();
  if (!cleanUid) return { ok: false };

  const cleanPhone = String(profile.phone || '').replace(/\D/g, '').slice(-10);
  const payload: CustomerSession = {
    ...profile,
    uid: cleanUid,
    phone: cleanPhone,
    name: profile.name.trim(),
    address: profile.address.trim(),
    villageArea: profile.villageArea?.trim() || 'Main Bazaar, Sandhaur 148020',
    profileComplete: isCustomerProfileComplete({ ...profile, phone: cleanPhone }),
    updatedAt: new Date().toISOString(),
  };

  if (firebaseDb) {
    try {
      await set(ref(firebaseDb, `customers/${cleanUid}`), payload);
      return { ok: true };
    } catch {
      // fallback to REST PUT
    }
  }

  try {
    const res = await fetch(
      `${FIREBASE_RTDB_BASE}/customers/${encodeURIComponent(cleanUid)}.json`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }
    );
    return { ok: res.ok };
  } catch {
    return { ok: false };
  }
}

/**
 * Real-time subscription to /orders filtered strictly by the signed-in user's uid,
 * sorted newest first. Uses Firebase RTDB onValue when available, plus live polling fallback
 * so Admin status updates appear automatically without page refresh.
 */
export function subscribeToCustomerOrders(
  uid: string,
  onUpdate: (orders: OrderRecord[], connected: boolean) => void
): () => void {
  const cleanUid = String(uid || '').trim();
  if (!cleanUid) {
    onUpdate([], false);
    return () => {};
  }

  const filterAndSort = (raw: unknown): OrderRecord[] => {
    const remoteOrders = parseOrdersResponse(raw).filter((o) => o.uid === cleanUid);
    const localOrders = getLocalOrders().filter((o) => o.uid === cleanUid);
    const byId = new Map<string, OrderRecord>();

    for (const lo of localOrders) {
      byId.set(lo.orderId, lo);
    }
    for (const ro of remoteOrders) {
      // Remote status always wins over stale local cache
      byId.set(ro.orderId, ro);
    }

    return Array.from(byId.values()).sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  };

  let active = true;
  let unsubscribeSdk: (() => void) | null = null;
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const pollOnce = async () => {
    if (!active) return;
    try {
      const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/orders.json`, 6500);
      if (!active) return;
      onUpdate(filterAndSort(remote), true);
    } catch {
      if (!active) return;
      const localOnly = getLocalOrders()
        .filter((o) => o.uid === cleanUid)
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      onUpdate(localOnly, false);
    }
  };

  if (firebaseDb) {
    try {
      const ordersRef = ref(firebaseDb, 'orders');
      unsubscribeSdk = onValue(
        ordersRef,
        (snapshot) => {
          if (!active) return;
          onUpdate(filterAndSort(snapshot.val()), true);
        },
        () => {
          // If SDK listener fails (e.g., rules/offline), fall back to live polling
          if (active && !pollTimer) {
            void pollOnce();
            pollTimer = setInterval(() => {
              void pollOnce();
            }, 6000);
          }
        }
      );
    } catch {
      void pollOnce();
      pollTimer = setInterval(() => {
        void pollOnce();
      }, 6000);
    }
  } else {
    void pollOnce();
    pollTimer = setInterval(() => {
      void pollOnce();
    }, 6000);
  }

  return () => {
    active = false;
    if (unsubscribeSdk) unsubscribeSdk();
    if (pollTimer) clearInterval(pollTimer);
  };
}
