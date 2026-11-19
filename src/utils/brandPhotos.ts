import { Product } from '../types';

const LEGACY_PLACEHOLDER_MARKERS = [
  '/images/studio/',
  'batch_',
  '/images/verified/',
  'placeholder',
];

function isLegacyPlaceholder(url: string): boolean {
  const lower = url.trim().toLowerCase();
  if (!lower) return true;
  return LEGACY_PLACEHOLDER_MARKERS.some((marker) => lower.includes(marker));
}

export function isVerifiedSkuFormat(id: string): boolean {
  const m = /^NAV(\d+)$/i.exec(String(id || '').trim());
  if (!m) return false;
  const num = parseInt(m[1], 10);
  return num >= 1 && num <= 308;
}

export function getCanonicalSkuImagePath(skuId: string): string {
  const sku = String(skuId || '').trim().toUpperCase();
  const stem = getSkuNumericStem(sku);
  return stem ? `/products/${stem}.jpg` : `/products/${sku}.jpg`;
}

export function hasCustomAdminPhoto(
  product: Partial<Pick<Product, 'imageUrl' | 'customImage' | 'image'>>
): boolean {
  for (const field of [product.imageUrl, product.customImage, product.image]) {
    const val = String(field || '').trim();
    if (
      val &&
      !isLegacyPlaceholder(val) &&
      (val.startsWith('data:image/') ||
        val.startsWith('https://') ||
        val.startsWith('http://'))
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Builds the ordered deduplicated list of real photo candidates for any SKU:
 * 1) Nav AI Media Studio live `imageUrl` (HTTPS / data URI / explicit /products/...)
 * 2) Admin `customImage` (data URI / HTTPS / explicit /products/...)
 * 3) Product `image` (data URI / HTTPS / explicit /products/...)
 * 4) /products/001.jpg–/products/308.jpg (for NAV001–NAV308) or /products/{SKU}.jpg
 * 5) /products/NAV001.jpg–/products/NAV308.jpg
 * 6) .png variants (/products/001.png, /products/NAV001.png)
 */
export function getProductImageCandidates(
  product: Pick<Product, 'id'> &
    Partial<Pick<Product, 'imageUrl' | 'customImage' | 'image'>>
): string[] {
  const candidates: string[] = [];
  const add = (val?: string) => {
    const clean = String(val || '').trim();
    if (!clean || clean.startsWith('data:image/svg+xml') || isLegacyPlaceholder(clean)) {
      return;
    }
    if (
      clean.startsWith('data:image/') ||
      clean.startsWith('https://') ||
      clean.startsWith('http://') ||
      clean.startsWith('/products/')
    ) {
      if (!candidates.includes(clean)) {
        candidates.push(clean);
      }
    }
  };

  add(product.imageUrl);
  add(product.customImage);
  add(product.image);

  const sku = String(product.id || '').trim().toUpperCase();
  const isNavSku = /^NAV\d+$/i.test(sku);
  const stem = isNavSku ? getSkuNumericStem(sku) : null;

  if (stem) {
    add(`/products/${stem}.jpg`);
  }
  if (sku && isNavSku) {
    add(`/products/${sku}.jpg`);
  }
  if (stem) {
    add(`/products/${stem}.png`);
  }
  if (sku && isNavSku) {
    add(`/products/${sku}.png`);
  }

  return candidates;
}

/**
 * Resolves every SKU image strictly by custom override -> explicit /products/ path ->
 * /products/001.jpg..308.jpg -> /products/NAV001.jpg..NAV308.jpg -> crisp SKU SVG card.
 */
export function resolveProductImage(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'> &
    Partial<Pick<Product, 'imageUrl' | 'customImage' | 'image'>>
): string {
  const candidates = getProductImageCandidates(product);
  if (candidates.length > 0) {
    return candidates[0];
  }

  const current = String(product.imageUrl || product.image || '').trim();
  if (current && !isLegacyPlaceholder(current)) {
    return current;
  }

  return buildBrandPackageSvg(product);
}

function escapeXml(value: string): string {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

/**
 * Pure SKU-specific inline SVG packaging fallback used when no image asset is linked
 * or on <img> load failure so zero mismatched or broken images ever render for any SKU.
 */
export function buildBrandPackageSvg(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'>
): string {
  const sku = escapeXml((product.id || 'NAV').trim().toUpperCase());
  const brand = escapeXml((product.brand || 'NAV KART').trim().toUpperCase().slice(0, 22));
  const rawName = String(product.name || 'Sealed Retail Pack').trim();
  const line1 = escapeXml(rawName.slice(0, 22));
  const line2 = escapeXml(rawName.slice(22, 44));
  const dept = (product.department || '').toLowerCase();
  const cat = (product.category || '').toLowerCase();
  const isFrozen = dept.includes('frozen') || cat.includes('frozen');
  const isElectronics = dept.includes('electronic') || dept.includes('kitchen');
  const isApparel = dept.includes('clothing') || dept.includes('apparel');

  const accent = isFrozen
    ? '#0369A1'
    : isElectronics
    ? '#14221B'
    : isApparel
    ? '#7C2D12'
    : '#0B3B24';

  const sealText = isFrozen
    ? '-18°C COLD CHAIN'
    : isElectronics
    ? 'WARRANTY SEALED · 12% OFF'
    : isApparel
    ? 'NAV KART FASHION · 12% OFF'
    : 'SEALED WHOLESALE PACK';

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
  <rect width="400" height="400" fill="#FFFFFF"/>
  <rect x="34" y="28" width="332" height="344" rx="26" fill="#FAF8F3" stroke="${accent}" stroke-width="3"/>
  <rect x="34" y="28" width="332" height="68" rx="22" fill="${accent}"/>
  <rect x="34" y="72" width="332" height="24" fill="${accent}"/>
  <text x="200" y="66" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="20" font-weight="800" fill="#E6A817" letter-spacing="2">${brand}</text>
  <rect x="132" y="118" width="136" height="34" rx="8" fill="#FFFFFF" stroke="#E6A817" stroke-width="1.5"/>
  <text x="200" y="140" text-anchor="middle" font-family="ui-monospace,monospace" font-size="16" font-weight="700" fill="${accent}">${sku}</text>
  <text x="200" y="206" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="21" font-weight="700" fill="#14221B">${line1}</text>
  <text x="200" y="236" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="19" font-weight="600" fill="#526058">${line2}</text>
  <rect x="64" y="286" width="272" height="44" rx="12" fill="${accent}" fill-opacity="0.1"/>
  <text x="200" y="313" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="12" font-weight="800" fill="${accent}" letter-spacing="1">${sealText}</text>
</svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

export function getSkuNumericStem(skuId: string): string | null {
  const clean = String(skuId || '').trim();
  const navMatch = /^NAV0*(\d+)$/i.exec(clean);
  if (navMatch) {
    return navMatch[1].padStart(3, '0');
  }
  const numMatch = /^0*(\d+)$/.exec(clean);
  if (numMatch) {
    return numMatch[1].padStart(3, '0');
  }
  return null;
}

export function getProductFallbackChain(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'> &
    Partial<Pick<Product, 'imageUrl' | 'customImage' | 'image'>>
): string[] {
  const candidates = getProductImageCandidates(product);
  candidates.push(buildBrandPackageSvg(product));
  return candidates;
}

export function getNextFallbackImage(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'> &
    Partial<Pick<Product, 'imageUrl' | 'customImage' | 'image'>>,
  currentFailedSrc: string
): string {
  const svgFallback = buildBrandPackageSvg(product);
  const cleanFailed = String(currentFailedSrc || '').trim();
  if (!cleanFailed || cleanFailed.startsWith('data:image/svg+xml')) {
    return svgFallback;
  }

  let failedPath = cleanFailed;
  try {
    if (/^https?:\/\//i.test(cleanFailed)) {
      failedPath = new URL(cleanFailed).pathname;
    }
  } catch {
    failedPath = cleanFailed;
  }

  const chain = getProductFallbackChain(product);
  const idx = chain.findIndex(
    (candidate) => candidate === cleanFailed || candidate === failedPath
  );
  if (idx >= 0 && idx + 1 < chain.length) {
    return chain[idx + 1];
  }

  const nextUnvisited = chain.find(
    (candidate) => candidate !== cleanFailed && candidate !== failedPath
  );
  return nextUnvisited || svgFallback;
}
