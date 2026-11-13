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
  return `/products/${sku}.jpg`;
}

export function hasCustomAdminPhoto(
  product: Partial<Pick<Product, 'customImage' | 'image'>>
): boolean {
  const custom = String(product.customImage || '').trim();
  if (
    custom &&
    !isLegacyPlaceholder(custom) &&
    (custom.startsWith('data:image/') ||
      custom.startsWith('https://') ||
      custom.startsWith('http://'))
  ) {
    return true;
  }

  const img = String(product.image || '').trim();
  if (
    img &&
    !isLegacyPlaceholder(img) &&
    (img.startsWith('data:image/') ||
      img.startsWith('https://') ||
      img.startsWith('http://'))
  ) {
    return true;
  }

  return false;
}

/**
 * Resolves every SKU image strictly by exact SKU ID (/products/NAV001.jpg .. /products/NAV308.jpg),
 * giving priority override ONLY when the Admin panel has uploaded a custom replacement photo
 * in Firebase Realtime Database (`customImage` or custom uploaded `image` URL/data URI).
 */
export function resolveProductImage(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'> &
    Partial<Pick<Product, 'customImage' | 'image'>>
): string {
  const sku = String(product.id || '').trim().toUpperCase();

  // Priority 1: Admin-uploaded custom replacement photo in Firebase RTDB
  const custom = String(product.customImage || '').trim();
  if (
    custom &&
    !isLegacyPlaceholder(custom) &&
    (custom.startsWith('data:image/') ||
      custom.startsWith('https://') ||
      custom.startsWith('http://') ||
      custom.startsWith('/products/'))
  ) {
    return custom;
  }

  // Priority 1b: Custom uploaded data/https URL stored directly in `image` field
  const current = String(product.image || '').trim();
  if (
    current &&
    !isLegacyPlaceholder(current) &&
    (current.startsWith('data:image/') ||
      current.startsWith('https://') ||
      current.startsWith('http://'))
  ) {
    return current;
  }

  // Priority 2: Direct exact SKU packaging photo /products/NAV001.jpg .. /products/NAV308.jpg
  if (isVerifiedSkuFormat(sku)) {
    return getCanonicalSkuImagePath(sku);
  }

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
 * Pure SKU-specific inline SVG packaging fallback used ONLY on <img> load failure
 * so zero mismatched or cropped stock photos ever render for any SKU.
 */
export function buildBrandPackageSvg(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'>
): string {
  const sku = escapeXml((product.id || 'NAV').trim().toUpperCase());
  const brand = escapeXml((product.brand || 'NAV GROCERY').trim().toUpperCase().slice(0, 22));
  const rawName = String(product.name || 'Sealed Grocery Pack').trim();
  const line1 = escapeXml(rawName.slice(0, 22));
  const line2 = escapeXml(rawName.slice(22, 44));
  const isFrozen =
    (product.department || '').toLowerCase().includes('frozen') ||
    (product.category || '').toLowerCase().includes('frozen');
  const accent = isFrozen ? '#0369A1' : '#0B3B24';
  const sealText = isFrozen ? '-18°C COLD CHAIN' : 'SEALED WHOLESALE PACK';

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
  <rect width="400" height="400" fill="#FFFFFF"/>
  <rect x="34" y="28" width="332" height="344" rx="26" fill="#FAF8F3" stroke="${accent}" stroke-width="3"/>
  <rect x="34" y="28" width="332" height="68" rx="22" fill="${accent}"/>
  <rect x="34" y="72" width="332" height="24" fill="${accent}"/>
  <text x="200" y="66" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="20" font-weight="800" fill="#D4AF37" letter-spacing="2">${brand}</text>
  <rect x="132" y="118" width="136" height="34" rx="8" fill="#FFFFFF" stroke="#D4AF37" stroke-width="1.5"/>
  <text x="200" y="140" text-anchor="middle" font-family="ui-monospace,monospace" font-size="16" font-weight="700" fill="${accent}">${sku}</text>
  <text x="200" y="206" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="21" font-weight="700" fill="#14221B">${line1}</text>
  <text x="200" y="236" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="19" font-weight="600" fill="#526058">${line2}</text>
  <rect x="76" y="286" width="248" height="44" rx="12" fill="${accent}" fill-opacity="0.1"/>
  <text x="200" y="313" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="13" font-weight="800" fill="${accent}" letter-spacing="1">${sealText}</text>
</svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}
