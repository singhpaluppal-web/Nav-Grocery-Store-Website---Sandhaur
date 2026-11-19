import { get, onValue, ref, set, update } from 'firebase/database';
import {
  BrandingSettings,
  CustomerSession,
  OrderItemSummary,
  OrderRecord,
  PaymentMethod,
  PaymentSettings,
  Product,
  ReturnRequest,
  StoreConfig,
  StoreStatusSettings,
} from '../types';
import { FIREBASE_DATABASE_URL, firebaseDb } from '../firebase';
import {
  hasZeroFreshProduce,
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
  STORE_STATUS: 'nav_public_store_status_v1',
  STORE_CONFIG: 'nav_public_store_config_v1',
  ORDERS: 'nav_public_orders_v3',
  RETURNS: 'nav_public_returns_v1',
};

export const DEFAULT_STORE_CONFIG: StoreConfig = {
  storeName: 'Nav Grocery Store',
  isOpen: true,
  hoursLabel: '8:00 AM – 9:30 PM',
  address: 'Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020',
  phones: ['+91 8847370905', '+91 9915752905'],
  announcementText:
    'Nav Grocery Store · Main Bazaar, Sandhaur 148020 · Flat 12% Off MRP · Open 8:00 AM – 9:30 PM',
  deliveryFee: 30,
  freeDeliveryAbove: 499,
  codMinOrder: 199,
  expressDeliveryEnabled: true,
  pickupEnabled: true,
  deliveryZones: [
    'Main Bazaar, Sandhaur 148020 (15-Min Express / 2-Hr)',
    'Malerkotla / Ludhiana Hub (Same-Day)',
    'All-India Parcel Courier (3–5 Days)',
    'International NRI Gift Dispatch',
  ],
};

export const DEFAULT_STORE_STATUS: StoreStatusSettings = {
  storeName: DEFAULT_STORE_CONFIG.storeName,
  isOpen: DEFAULT_STORE_CONFIG.isOpen,
  hoursLabel: DEFAULT_STORE_CONFIG.hoursLabel,
  address: DEFAULT_STORE_CONFIG.address,
  phones: DEFAULT_STORE_CONFIG.phones,
  dispatchNotice: '15-Min Express & 2-Hr Delivery in Sandhaur 148020 · 30-Min Store Pickup',
};

export const DEFAULT_BRANDING: BrandingSettings = {
  logoUrl: '/images/logo.png',
  storeTagline:
    'Nav Grocery Store & Nav Kart · Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020',
  announcementText:
    'Nav Grocery Store · Main Bazaar, Sandhaur 148020 · Flat 12% Off MRP · Open 8:00 AM – 9:30 PM',
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
    return raw.filter((item): item is Product => Boolean(item && typeof item === 'object' && item.id));
  }
  if (typeof raw === 'object') {
    const out: Product[] = [];
    for (const [key, val] of Object.entries(raw as Record<string, unknown>)) {
      if (!val || typeof val !== 'object') continue;
      const rec = val as Partial<Product>;
      const id = String(rec.id || key || '').trim().toUpperCase();
      if (!id) continue;
      out.push({ ...(rec as Product), id });
    }
    return out;
  }
  return [];
}

function normalizeIncomingOrder(rawItem: unknown, fallbackKey?: string): OrderRecord | null {
  if (!rawItem || typeof rawItem !== 'object') return null;
  const obj = rawItem as Record<string, unknown>;
  const orderId = String(obj.orderId || obj.id || fallbackKey || '').trim();
  if (!orderId) return null;

  const rawSource = String(obj.source || obj.platform || obj.channel || '').trim().toLowerCase();
  const inferredSource: 'web' | 'android' =
    rawSource.includes('android') ||
    rawSource.includes('navkart') ||
    rawSource.includes('mobile') ||
    rawSource.includes('app') ||
    /^nk[-_]/i.test(orderId)
      ? 'android'
      : 'web';

  const rawPaymentMethod = String(obj.paymentMethod || obj.paymentMode || 'cod')
    .trim()
    .toLowerCase();
  const paymentMethod: PaymentMethod =
    rawPaymentMethod === 'upi' ||
    rawPaymentMethod === 'razorpay' ||
    rawPaymentMethod === 'bank_transfer' ||
    rawPaymentMethod === 'cod'
      ? (rawPaymentMethod as PaymentMethod)
      : rawPaymentMethod.includes('upi')
      ? 'upi'
      : rawPaymentMethod.includes('razor')
      ? 'razorpay'
      : rawPaymentMethod.includes('bank') || rawPaymentMethod.includes('neft')
      ? 'bank_transfer'
      : 'cod';

  const rawItems = Array.isArray(obj.items)
    ? obj.items
    : obj.items && typeof obj.items === 'object'
    ? Object.values(obj.items as Record<string, unknown>)
    : [];

  const items: OrderItemSummary[] = rawItems
    .filter((it): it is Record<string, unknown> => Boolean(it && typeof it === 'object'))
    .map((it) => {
      const mrp = Math.max(1, Math.round(Number(it.mrp ?? it.price ?? 0) || 1));
      const sale = Math.round(mrp * 0.88);
      return {
        id: String(it.id || it.sku || 'SKU').toUpperCase(),
        name: String(it.name || it.title || 'Packaged Grocery Item'),
        brand: String(it.brand || 'Nav Grocery'),
        qty: Math.max(1, Number(it.qty ?? it.quantity ?? 1) || 1),
        mrp,
        sale: Number(it.sale) > 0 ? Math.round(Number(it.sale)) : sale,
        gst: String(it.gst || '5%'),
      };
    });

  const computedSale = items.reduce((sum, i) => sum + i.sale * i.qty, 0);
  const computedMrp = items.reduce((sum, i) => sum + i.mrp * i.qty, 0);
  const totalPayable = Number(
    obj.totalPayable ?? obj.totalAmount ?? obj.total ?? obj.grandTotal ?? computedSale
  ) || 0;

  const createdRaw = obj.createdAt ?? obj.timestamp ?? obj.date;
  const createdAt =
    typeof createdRaw === 'number'
      ? new Date(createdRaw).toISOString()
      : typeof createdRaw === 'string' && createdRaw.trim()
      ? createdRaw
      : new Date().toISOString();

  const paymentStatusRaw = String(obj.paymentStatus || '').trim();
  const paymentStatus: OrderRecord['paymentStatus'] =
    paymentStatusRaw === 'Paid / UTR Submitted' ||
    paymentStatusRaw === 'UTR Verified' ||
    paymentStatusRaw === 'Pay on Delivery' ||
    paymentStatusRaw === 'Pay at Pickup' ||
    paymentStatusRaw === 'Gateway Paid' ||
    paymentStatusRaw === 'Bank Transfer / UTR Submitted'
      ? paymentStatusRaw
      : paymentMethod === 'cod'
      ? 'Pay on Delivery'
      : 'Paid / UTR Submitted';

  return {
    ...(obj as unknown as OrderRecord),
    orderId,
    source: inferredSource,
    uid: obj.uid ? String(obj.uid) : obj.userId ? String(obj.userId) : undefined,
    customerEmail: obj.customerEmail ? String(obj.customerEmail) : undefined,
    createdAt,
    updatedAt: obj.updatedAt ? String(obj.updatedAt) : undefined,
    customerName: String(obj.customerName || obj.userName || obj.name || 'Sandhaur Customer'),
    customerPhone: String(obj.customerPhone || obj.phone || obj.mobile || '').replace(/\D/g, '').slice(-10),
    address: String(obj.address || obj.deliveryAddress || 'Main Bazaar, Sandhaur 148020'),
    fulfillment: obj.fulfillment === 'pickup' ? 'pickup' : 'delivery',
    slot: String(obj.slot || obj.deliverySlot || '2-Hour Local Delivery (Sandhaur 148020)'),
    paymentMethod,
    paymentStatus,
    subtotalMrp: Number(obj.subtotalMrp ?? computedMrp) || computedMrp,
    subtotalSale: Number(obj.subtotalSale ?? computedSale) || computedSale,
    savings: Number(obj.savings ?? Math.max(0, computedMrp - computedSale)) || 0,
    deliveryFee: Number(obj.deliveryFee ?? 0) || 0,
    totalPayable,
    status: String(obj.status || 'Pending'),
    items,
  };
}

function parseOrdersResponse(raw: unknown): OrderRecord[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw
      .map((item) => normalizeIncomingOrder(item))
      .filter((item): item is OrderRecord => Boolean(item));
  }
  if (typeof raw === 'object') {
    const out: OrderRecord[] = [];
    for (const [key, val] of Object.entries(raw as Record<string, unknown>)) {
      const normalized = normalizeIncomingOrder(val, key);
      if (normalized) out.push(normalized);
    }
    return out;
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
 * Reads a Firebase RTDB path via the initialized Firebase Database SDK first,
 * falling back gracefully to REST JSON fetch if SDK rules or network fail.
 */
async function readRtdbPath(path: string, timeoutMs = 6000): Promise<unknown> {
  const cleanPath = path.replace(/^\/+/, '');
  if (firebaseDb) {
    try {
      const snap = await Promise.race([
        get(ref(firebaseDb, cleanPath)),
        new Promise<never>((_, reject) =>
          setTimeout(() => reject(new Error('RTDB SDK timeout')), timeoutMs)
        ),
      ]);
      return snap.exists() ? snap.val() : null;
    } catch {
      // Gracefully fall back to REST endpoint if restricted or timed out
    }
  }
  return await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/${cleanPath}.json`, timeoutMs);
}

/**
 * Writes a Firebase RTDB path via the initialized Firebase Database SDK first,
 * falling back gracefully to REST PUT/PATCH if needed.
 */
async function writeRtdbPath(
  path: string,
  payload: unknown,
  method: 'PUT' | 'PATCH' = 'PUT'
): Promise<boolean> {
  const cleanPath = path.replace(/^\/+/, '');
  if (firebaseDb) {
    try {
      const nodeRef = ref(firebaseDb, cleanPath);
      if (method === 'PATCH' && payload && typeof payload === 'object') {
        await update(nodeRef, payload as Record<string, unknown>);
      } else {
        await set(nodeRef, payload);
      }
      return true;
    } catch {
      // Gracefully fall back to REST write
    }
  }

  try {
    const res = await fetch(`${FIREBASE_RTDB_BASE}/${cleanPath}.json`, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return res.ok;
  } catch {
    return false;
  }
}

function isBannerFreeOfFreshProduce(banner: { title?: string; subtitle?: string; badge?: string }): boolean {
  const text = `${banner.title || ''} ${banner.subtitle || ''} ${banner.badge || ''}`.toLowerCase();
  if (text.includes('zero fresh') || text.includes('no fresh') || text.includes('frozen')) {
    return !/\bfresh\s+(?:fruits?|vegetables?|produce|sabzi|greens?)\b/i.test(
      text.replace(/(?:zero|no)\s+fresh\s+(?:fruits?|vegetables?|produce|sabzi)/gi, '')
    );
  }
  return !/\bfresh\s+(?:fruits?|vegetables?|produce|sabzi|greens?)\b/i.test(text);
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

function mergeRemoteProductsWithSeeds(
  remote: unknown,
  baseSeeds: Map<string, Product>
): Product[] {
  const mergedMap = new Map<string, Product>(baseSeeds);
  const remoteList = parseArrayResponse(remote);

  for (const rawItem of remoteList) {
    const seed = mergedMap.get(rawItem.id);
    // Merge remote patch (e.g. Nav AI Media Studio publishing imageUrl & mrp) onto verified seed
    const candidate: Product = {
      ...(seed || ({} as Product)),
      ...rawItem,
      id: rawItem.id,
      name: !isPlaceholderProductName(rawItem.name)
        ? rawItem.name
        : seed?.name || rawItem.name || '',
      brand: rawItem.brand || seed?.brand || 'Nav Grocery',
      category: rawItem.category || seed?.category || 'Grocery & Staples',
      department: rawItem.department || seed?.department || 'Packaged Groceries',
      mrp: Number(rawItem.mrp) > 0 ? Number(rawItem.mrp) : seed?.mrp || 1,
      imageUrl: rawItem.imageUrl || seed?.imageUrl,
    };

    if (!candidate.name || isPlaceholderProductName(candidate.name)) continue;
    if (!isAllowedRetailProduct(candidate) || !hasZeroFreshProduce(candidate)) continue;

    const normalized = normalizeWholesalePricing({
      ...candidate,
      image: resolveProductImage(candidate),
    });
    mergedMap.set(normalized.id, normalized);
  }

  return Array.from(mergedMap.values()).sort((a, b) =>
    a.id.localeCompare(b.id, undefined, { numeric: true })
  );
}

/**
 * Fetches unified Nav Grocery (308 SKUs) + Nav Kart Supermall (Electronics, Kitchen Appliances, Apparel)
 * strictly filtering out any banned fresh produce items and applying Math.round(mrp * 0.88).
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

  let unified = Array.from(baseById.values()).sort((a, b) =>
    a.id.localeCompare(b.id, undefined, { numeric: true })
  );

  try {
    const remote = await readRtdbPath('products', 6500);
    if (remote) {
      unified = mergeRemoteProductsWithSeeds(remote, baseById);
    }
  } catch {
    // Graceful fallback uses verified static + supermall catalog seamlessly
  }

  try {
    localStorage.setItem(LOCAL_KEYS.UNIFIED, JSON.stringify(unified.slice(0, 340)));
  } catch {
    // ignore quota limits
  }

  return unified;
}

/**
 * Subscribes live to /products in Firebase Realtime Database so any new photo (`imageUrl`)
 * or MRP update published from Nav AI Media Studio reflects immediately on the storefront
 * with flat 12% wholesale pricing (`Math.round(mrp * 0.88)`) and strict zero-fresh-produce rules.
 */
export function subscribeToLiveProducts(
  onUpdate: (products: Product[], connected: boolean) => void
): () => void {
  let active = true;
  let unsubscribeSdk: (() => void) | null = null;
  let pollTimer: ReturnType<typeof setInterval> | null = null;
  const baseById = new Map<string, Product>();

  const emitFromRemote = (remote: unknown, connected: boolean) => {
    if (!active) return;
    const merged = remote
      ? mergeRemoteProductsWithSeeds(remote, baseById)
      : Array.from(baseById.values()).sort((a, b) =>
          a.id.localeCompare(b.id, undefined, { numeric: true })
        );
    try {
      localStorage.setItem(LOCAL_KEYS.UNIFIED, JSON.stringify(merged.slice(0, 340)));
    } catch {
      // ignore storage quota errors
    }
    onUpdate(merged, connected);
  };

  const pollOnce = async () => {
    if (!active) return;
    try {
      const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/products.json`, 6500);
      emitFromRemote(remote, true);
    } catch {
      emitFromRemote(null, false);
    }
  };

  const init = async () => {
    const localGrocery = await fetchStaticCatalogJson();
    for (const p of localGrocery) {
      baseById.set(p.id, { ...p, image: resolveProductImage(p) });
    }
    for (const sm of NAVKART_SUPERMALL_SKUS) {
      baseById.set(sm.id, { ...sm, image: resolveProductImage(sm) });
    }

    if (!active) return;

    if (firebaseDb) {
      try {
        const productsRef = ref(firebaseDb, 'products');
        unsubscribeSdk = onValue(
          productsRef,
          (snapshot) => {
            if (!active) return;
            emitFromRemote(snapshot.val(), true);
          },
          () => {
            if (active && !pollTimer) {
              void pollOnce();
              pollTimer = setInterval(() => void pollOnce(), 12000);
            }
          }
        );
        return;
      } catch {
        // fallback to REST polling
      }
    }

    void pollOnce();
    pollTimer = setInterval(() => void pollOnce(), 12000);
  };

  void init();

  return () => {
    active = false;
    if (unsubscribeSdk) unsubscribeSdk();
    if (pollTimer) clearInterval(pollTimer);
  };
}

function normalizeRawAdminProducts(remote: unknown): Product[] {
  return parseArrayResponse(remote)
    .filter((item) => !isPlaceholderProductName(item.name) && hasZeroFreshProduce(item))
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
    })
    .sort((a, b) => a.id.localeCompare(b.id, undefined, { numeric: true }));
}

/**
 * Live Admin RTDB subscription to /products so Nav AI Media Studio updates appear
 * instantaneously inside the Website Admin Catalog.
 */
export function subscribeToAdminProductsRaw(
  onUpdate: (products: Product[], connected: boolean) => void
): () => void {
  let active = true;
  let unsubscribeSdk: (() => void) | null = null;
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const pollOnce = async () => {
    if (!active) return;
    try {
      const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/products.json`, 9000);
      if (!active) return;
      onUpdate(normalizeRawAdminProducts(remote), true);
    } catch {
      if (!active) return;
      onUpdate([], false);
    }
  };

  if (firebaseDb) {
    try {
      unsubscribeSdk = onValue(
        ref(firebaseDb, 'products'),
        (snapshot) => {
          if (!active) return;
          onUpdate(normalizeRawAdminProducts(snapshot.val()), true);
        },
        () => {
          if (active && !pollTimer) {
            void pollOnce();
            pollTimer = setInterval(() => void pollOnce(), 12000);
          }
        }
      );
    } catch {
      void pollOnce();
      pollTimer = setInterval(() => void pollOnce(), 12000);
    }
  } else {
    void pollOnce();
    pollTimer = setInterval(() => void pollOnce(), 12000);
  }

  return () => {
    active = false;
    if (unsubscribeSdk) unsubscribeSdk();
    if (pollTimer) clearInterval(pollTimer);
  };
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
  const resolvedImage = resolveProductImage({ ...product, id: cleanSku });
  const payload: Product = {
    ...product,
    id: cleanSku,
    mrp,
    sale,
    price: sale,
    image: resolvedImage,
    imageUrl: product.imageUrl || product.customImage || resolvedImage,
    channels: product.channels || {
      navGroceryWeb: true,
      navKartWeb: true,
      androidApps: true,
    },
    updatedAt: new Date().toISOString(),
  };

  const ok = await writeRtdbPath(`products/${encodeURIComponent(cleanSku)}`, payload, 'PUT');
  return { ok };
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
    const remote = (await readRtdbPath('settings/branding', 4500)) as Partial<BrandingSettings> | null;
    if (remote && typeof remote === 'object') {
      const safeBanners = Array.isArray(remote.banners)
        ? remote.banners.filter((b) => b.active !== false && isBannerFreeOfFreshProduce(b))
        : [];
      const merged: BrandingSettings = {
        ...DEFAULT_BRANDING,
        ...remote,
        banners: safeBanners.length > 0 ? safeBanners : DEFAULT_BRANDING.banners,
      };
      try {
        localStorage.setItem(LOCAL_KEYS.BRANDING, JSON.stringify(merged));
      } catch {
        // ignore storage quota errors
      }
      return merged;
    }
  } catch {
    // Graceful fallback to cached or default branding
  }
  try {
    const cached = localStorage.getItem(LOCAL_KEYS.BRANDING);
    if (cached) {
      const parsed = JSON.parse(cached) as Partial<BrandingSettings>;
      return {
        ...DEFAULT_BRANDING,
        ...parsed,
        banners:
          Array.isArray(parsed.banners) && parsed.banners.length > 0
            ? parsed.banners.filter((b) => b.active !== false && isBannerFreeOfFreshProduce(b))
            : DEFAULT_BRANDING.banners,
      };
    }
  } catch {
    // ignore parse failure
  }
  return DEFAULT_BRANDING;
}

export async function fetchPaymentSettings(): Promise<PaymentSettings> {
  try {
    const remote = (await readRtdbPath('settings/payments', 4500)) as Partial<PaymentSettings> | null;
    if (remote && typeof remote === 'object') {
      const merged = { ...DEFAULT_PAYMENTS, ...remote };
      try {
        localStorage.setItem(LOCAL_KEYS.PAYMENTS, JSON.stringify(merged));
      } catch {
        // ignore storage quota errors
      }
      return merged;
    }
  } catch {
    // Graceful fallback to cached or default payment settings
  }
  try {
    const cached = localStorage.getItem(LOCAL_KEYS.PAYMENTS);
    if (cached) {
      return { ...DEFAULT_PAYMENTS, ...(JSON.parse(cached) as Partial<PaymentSettings>) };
    }
  } catch {
    // ignore parse failure
  }
  return DEFAULT_PAYMENTS;
}

export async function fetchStoreStatusSettings(): Promise<StoreStatusSettings> {
  try {
    const remote = (await readRtdbPath(
      'settings/storeStatus',
      4500
    )) as Partial<StoreStatusSettings> | null;
    if (remote && typeof remote === 'object') {
      const merged: StoreStatusSettings = {
        ...DEFAULT_STORE_STATUS,
        ...remote,
        hoursLabel: remote.hoursLabel?.trim() || DEFAULT_STORE_STATUS.hoursLabel,
        address: remote.address?.trim() || DEFAULT_STORE_STATUS.address,
        phones:
          Array.isArray(remote.phones) && remote.phones.length > 0
            ? remote.phones
            : DEFAULT_STORE_STATUS.phones,
      };
      try {
        localStorage.setItem(LOCAL_KEYS.STORE_STATUS, JSON.stringify(merged));
      } catch {
        // ignore storage quota errors
      }
      return merged;
    }
  } catch {
    // Graceful fallback to local cache or defaults
  }

  try {
    const cached = localStorage.getItem(LOCAL_KEYS.STORE_STATUS);
    if (cached) {
      return { ...DEFAULT_STORE_STATUS, ...(JSON.parse(cached) as Partial<StoreStatusSettings>) };
    }
  } catch {
    // ignore parse error
  }
  return DEFAULT_STORE_STATUS;
}

export async function saveStoreStatusSettings(
  status: StoreStatusSettings
): Promise<{ ok: boolean }> {
  const payload: StoreStatusSettings = {
    ...DEFAULT_STORE_STATUS,
    ...status,
    updatedAt: new Date().toISOString(),
  };
  try {
    localStorage.setItem(LOCAL_KEYS.STORE_STATUS, JSON.stringify(payload));
  } catch {
    // ignore storage errors
  }
  const ok = await writeRtdbPath('settings/storeStatus', payload, 'PUT');
  return { ok };
}

export async function savePaymentSettingsToFirebase(settings: PaymentSettings): Promise<{ ok: boolean }> {
  try {
    localStorage.setItem(LOCAL_KEYS.PAYMENTS, JSON.stringify(settings));
  } catch {
    // ignore storage errors
  }
  const ok = await writeRtdbPath('settings/payments', settings, 'PUT');
  return { ok };
}

function sanitizeStoreAnnouncement(raw?: string): string {
  const clean = String(raw || '').trim();
  if (!clean) return DEFAULT_STORE_CONFIG.announcementText;
  if (!isBannerFreeOfFreshProduce({ title: clean })) {
    return DEFAULT_STORE_CONFIG.announcementText;
  }
  return clean;
}

function normalizeStoreConfig(raw: unknown): StoreConfig {
  if (!raw || typeof raw !== 'object') return DEFAULT_STORE_CONFIG;
  const obj = raw as Partial<StoreConfig>;
  return {
    storeName: obj.storeName?.trim() || DEFAULT_STORE_CONFIG.storeName,
    isOpen: typeof obj.isOpen === 'boolean' ? obj.isOpen : DEFAULT_STORE_CONFIG.isOpen,
    hoursLabel: obj.hoursLabel?.trim() || DEFAULT_STORE_CONFIG.hoursLabel,
    address: obj.address?.trim() || DEFAULT_STORE_CONFIG.address,
    phones:
      Array.isArray(obj.phones) && obj.phones.length > 0
        ? obj.phones.map((p) => String(p))
        : DEFAULT_STORE_CONFIG.phones,
    announcementText: sanitizeStoreAnnouncement(obj.announcementText),
    deliveryFee:
      Number.isFinite(Number(obj.deliveryFee)) && Number(obj.deliveryFee) >= 0
        ? Math.round(Number(obj.deliveryFee))
        : DEFAULT_STORE_CONFIG.deliveryFee,
    freeDeliveryAbove:
      Number.isFinite(Number(obj.freeDeliveryAbove)) && Number(obj.freeDeliveryAbove) >= 0
        ? Math.round(Number(obj.freeDeliveryAbove))
        : DEFAULT_STORE_CONFIG.freeDeliveryAbove,
    codMinOrder:
      Number.isFinite(Number(obj.codMinOrder)) && Number(obj.codMinOrder) >= 0
        ? Math.round(Number(obj.codMinOrder))
        : DEFAULT_STORE_CONFIG.codMinOrder,
    expressDeliveryEnabled:
      typeof obj.expressDeliveryEnabled === 'boolean'
        ? obj.expressDeliveryEnabled
        : DEFAULT_STORE_CONFIG.expressDeliveryEnabled,
    pickupEnabled:
      typeof obj.pickupEnabled === 'boolean'
        ? obj.pickupEnabled
        : DEFAULT_STORE_CONFIG.pickupEnabled,
    deliveryZones:
      Array.isArray(obj.deliveryZones) && obj.deliveryZones.length > 0
        ? obj.deliveryZones.map((z) => String(z))
        : DEFAULT_STORE_CONFIG.deliveryZones,
    updatedAt: obj.updatedAt,
  };
}

export async function fetchStoreConfig(): Promise<StoreConfig> {
  try {
    const remote = await readRtdbPath('storeConfig', 4500);
    if (remote && typeof remote === 'object') {
      const merged = normalizeStoreConfig(remote);
      try {
        localStorage.setItem(LOCAL_KEYS.STORE_CONFIG, JSON.stringify(merged));
      } catch {
        // ignore storage quota
      }
      return merged;
    }
  } catch {
    // Graceful fallback
  }

  try {
    const cached = localStorage.getItem(LOCAL_KEYS.STORE_CONFIG);
    if (cached) {
      return normalizeStoreConfig(JSON.parse(cached));
    }
  } catch {
    // ignore parse failure
  }
  return DEFAULT_STORE_CONFIG;
}

export async function saveStoreConfigToFirebase(config: StoreConfig): Promise<{ ok: boolean }> {
  const payload: StoreConfig = {
    ...normalizeStoreConfig(config),
    updatedAt: new Date().toISOString(),
  };

  try {
    localStorage.setItem(LOCAL_KEYS.STORE_CONFIG, JSON.stringify(payload));
  } catch {
    // ignore storage errors
  }

  const ok = await writeRtdbPath('storeConfig', payload, 'PUT');
  // Also mirror store status node so legacy readers remain consistent
  void writeRtdbPath(
    'settings/storeStatus',
    {
      storeName: payload.storeName,
      isOpen: payload.isOpen,
      hoursLabel: payload.hoursLabel,
      address: payload.address,
      phones: payload.phones,
      dispatchNotice: payload.announcementText,
      updatedAt: payload.updatedAt,
    },
    'PUT'
  );
  return { ok };
}

/**
 * Live Firebase RTDB subscription to /storeConfig for store open/close status,
 * delivery rules, and announcement banner across Web & Navkart Android app.
 */
export function subscribeToStoreConfig(
  onUpdate: (config: StoreConfig, connected: boolean) => void
): () => void {
  let active = true;
  let unsubscribeSdk: (() => void) | null = null;
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const emit = (raw: unknown, connected: boolean) => {
    if (!active) return;
    const normalized = normalizeStoreConfig(raw);
    try {
      localStorage.setItem(LOCAL_KEYS.STORE_CONFIG, JSON.stringify(normalized));
    } catch {
      // ignore storage quota
    }
    onUpdate(normalized, connected);
  };

  const pollOnce = async () => {
    if (!active) return;
    try {
      const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/storeConfig.json`, 4500);
      emit(remote, true);
    } catch {
      const fallback = await fetchStoreConfig();
      if (active) onUpdate(fallback, false);
    }
  };

  if (firebaseDb) {
    try {
      unsubscribeSdk = onValue(
        ref(firebaseDb, 'storeConfig'),
        (snapshot) => {
          if (!active) return;
          if (snapshot.exists()) {
            emit(snapshot.val(), true);
          } else {
            onUpdate(DEFAULT_STORE_CONFIG, true);
          }
        },
        () => {
          if (active && !pollTimer) {
            void pollOnce();
            pollTimer = setInterval(() => void pollOnce(), 15000);
          }
        }
      );
    } catch {
      void pollOnce();
      pollTimer = setInterval(() => void pollOnce(), 15000);
    }
  } else {
    void pollOnce();
    pollTimer = setInterval(() => void pollOnce(), 15000);
  }

  return () => {
    active = false;
    if (unsubscribeSdk) unsubscribeSdk();
    if (pollTimer) clearInterval(pollTimer);
  };
}

export async function saveOrder(order: OrderRecord): Promise<{ ok: boolean }> {
  const taggedOrder: OrderRecord = {
    ...order,
    source: order.source || 'web',
  };
  try {
    const existingRaw = localStorage.getItem(LOCAL_KEYS.ORDERS);
    const existing: OrderRecord[] = existingRaw ? JSON.parse(existingRaw) : [];
    const next = [taggedOrder, ...existing.filter((o) => o.orderId !== taggedOrder.orderId)];
    localStorage.setItem(LOCAL_KEYS.ORDERS, JSON.stringify(next));
  } catch {
    // ignore
  }

  const ok = await writeRtdbPath(
    `orders/${encodeURIComponent(taggedOrder.orderId)}`,
    taggedOrder,
    'PUT'
  );
  return { ok };
}

/**
 * Live unified subscription to /orders for Website Admin dashboard so orders from
 * both www.navgrocerystore.com AND the Navkart Android app appear immediately.
 */
export function subscribeToAllOrders(
  onUpdate: (orders: OrderRecord[], connected: boolean) => void
): () => void {
  let active = true;
  let unsubscribeSdk: (() => void) | null = null;
  let pollTimer: ReturnType<typeof setInterval> | null = null;

  const mergeAndSort = (raw: unknown): OrderRecord[] => {
    const remoteOrders = parseOrdersResponse(raw);
    const localOrders = getLocalOrders();
    const byId = new Map<string, OrderRecord>();
    for (const lo of localOrders) {
      byId.set(lo.orderId, lo);
    }
    for (const ro of remoteOrders) {
      byId.set(ro.orderId, ro);
    }
    return Array.from(byId.values()).sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  };

  const pollOnce = async () => {
    if (!active) return;
    try {
      const remote = await fetchJsonWithTimeout(`${FIREBASE_RTDB_BASE}/orders.json`, 7000);
      if (!active) return;
      onUpdate(mergeAndSort(remote), true);
    } catch {
      if (!active) return;
      onUpdate(getLocalOrders(), false);
    }
  };

  if (firebaseDb) {
    try {
      unsubscribeSdk = onValue(
        ref(firebaseDb, 'orders'),
        (snapshot) => {
          if (!active) return;
          onUpdate(mergeAndSort(snapshot.val()), true);
        },
        () => {
          if (active && !pollTimer) {
            void pollOnce();
            pollTimer = setInterval(() => void pollOnce(), 6000);
          }
        }
      );
    } catch {
      void pollOnce();
      pollTimer = setInterval(() => void pollOnce(), 6000);
    }
  } else {
    void pollOnce();
    pollTimer = setInterval(() => void pollOnce(), 6000);
  }

  return () => {
    active = false;
    if (unsubscribeSdk) unsubscribeSdk();
    if (pollTimer) clearInterval(pollTimer);
  };
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

  const ok = await writeRtdbPath(`orders/${encodeURIComponent(cleanId)}`, payload, 'PATCH');
  return { ok };
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
