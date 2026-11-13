import QRCode from 'qrcode';
import { Product } from '../types';

export function buildSkuAdminDeepLink(skuId: string): string {
  const cleanSku = String(skuId || '').trim().toUpperCase();
  const origin =
    typeof window !== 'undefined' && window.location?.origin
      ? window.location.origin
      : 'https://www.navgrocerystore.com';
  return `${origin}/admin?sku=${encodeURIComponent(cleanSku)}`;
}

export function extractSkuFromScanInput(raw: string): string | null {
  const trimmed = String(raw || '').trim();
  if (!trimmed) return null;

  // Try parsing full URL or query string containing ?sku=...
  try {
    if (trimmed.includes('://') || trimmed.startsWith('/')) {
      const url = new URL(trimmed, 'https://www.navgrocerystore.com');
      const fromQuery = url.searchParams.get('sku');
      if (fromQuery && fromQuery.trim()) {
        return fromQuery.trim().toUpperCase();
      }
      if (url.hash && url.hash.includes('?')) {
        const hashQuery = url.hash.slice(url.hash.indexOf('?') + 1);
        const hashSku = new URLSearchParams(hashQuery).get('sku');
        if (hashSku && hashSku.trim()) {
          return hashSku.trim().toUpperCase();
        }
      }
    }
  } catch {
    // Fallback to regex / raw SKU normalization below
  }

  const queryMatch = /[?&#]sku=([^&#\s]+)/i.exec(trimmed);
  if (queryMatch?.[1]) {
    return decodeURIComponent(queryMatch[1]).trim().toUpperCase();
  }

  // Numeric shorthand e.g. "81" -> "NAV081"
  if (/^\d{1,3}$/.test(trimmed)) {
    return `NAV${trimmed.padStart(3, '0')}`;
  }

  return trimmed.toUpperCase().replace(/\s+/g, '');
}

export function readDeepLinkSkuFromLocation(): string | null {
  if (typeof window === 'undefined') return null;
  const searchSku = new URLSearchParams(window.location.search).get('sku');
  if (searchSku && searchSku.trim()) {
    return searchSku.trim().toUpperCase();
  }
  const hash = window.location.hash || '';
  const qIdx = hash.indexOf('?');
  if (qIdx !== -1) {
    const hashSku = new URLSearchParams(hash.slice(qIdx + 1)).get('sku');
    if (hashSku && hashSku.trim()) {
      return hashSku.trim().toUpperCase();
    }
  }
  return null;
}

export async function generateSkuQrDataUrl(
  payloadUrl: string,
  width = 320
): Promise<string> {
  return QRCode.toDataURL(payloadUrl, {
    width,
    margin: 2,
    errorCorrectionLevel: 'M',
    color: {
      dark: '#0B3B24',
      light: '#FFFFFF',
    },
  });
}

function wrapCanvasText(
  ctx: CanvasRenderingContext2D,
  text: string,
  maxWidth: number,
  maxLines: number
): string[] {
  const words = text.split(/\s+/).filter(Boolean);
  const lines: string[] = [];
  let current = '';

  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word;
    if (ctx.measureText(candidate).width <= maxWidth) {
      current = candidate;
    } else {
      if (current) lines.push(current);
      current = word;
      if (lines.length >= maxLines) break;
    }
  }
  if (current && lines.length < maxLines) {
    lines.push(current);
  }
  return lines;
}

export async function renderPrintableSkuLabelPng(
  product: Product,
  deepLinkUrl: string
): Promise<string> {
  const qrDataUrl = await generateSkuQrDataUrl(deepLinkUrl, 240);
  const qrImg = await new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Failed to render QR bitmap'));
    img.src = qrDataUrl;
  });

  const canvas = document.createElement('canvas');
  canvas.width = 680;
  canvas.height = 340;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas 2D context unavailable');

  // Card background & outer border
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = '#0B3B24';
  ctx.lineWidth = 4;
  ctx.strokeRect(2, 2, canvas.width - 4, canvas.height - 4);

  // Top emerald header bar
  ctx.fillStyle = '#0B3B24';
  ctx.fillRect(0, 0, canvas.width, 54);
  ctx.fillStyle = '#D4AF37';
  ctx.font = 'bold 18px sans-serif';
  ctx.fillText('NAV GROCERY STORE · NAVKART ADMIN', 22, 34);

  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold 20px monospace';
  const skuText = product.id;
  const skuMetrics = ctx.measureText(skuText);
  ctx.fillText(skuText, canvas.width - skuMetrics.width - 22, 34);

  // Left QR Code Box
  ctx.fillStyle = '#FAF8F3';
  ctx.fillRect(20, 74, 240, 240);
  ctx.strokeStyle = '#D4AF37';
  ctx.lineWidth = 2;
  ctx.strokeRect(20, 74, 240, 240);
  ctx.drawImage(qrImg, 28, 82, 224, 224);

  // Right Details Column
  const rx = 284;
  const maxW = canvas.width - rx - 24;

  ctx.fillStyle = '#526058';
  ctx.font = 'bold 15px sans-serif';
  ctx.fillText(`${product.brand} · ${product.department}`.toUpperCase(), rx, 102);

  ctx.fillStyle = '#14221B';
  ctx.font = 'bold 23px sans-serif';
  const titleLines = wrapCanvasText(ctx, product.name, maxW, 3);
  titleLines.forEach((line, idx) => {
    ctx.fillText(line, rx, 138 + idx * 30);
  });

  // Price Box
  const priceY = 236;
  ctx.fillStyle = '#FAF8F3';
  ctx.fillRect(rx, priceY, maxW, 56);
  ctx.strokeStyle = '#0B3B24';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(rx, priceY, maxW, 56);

  ctx.fillStyle = '#0B3B24';
  ctx.font = 'bold 26px monospace';
  ctx.fillText(`12% OFF: ₹${product.sale}`, rx + 14, priceY + 37);

  ctx.fillStyle = '#526058';
  ctx.font = 'bold 17px monospace';
  ctx.fillText(`MRP ₹${product.mrp}`, rx + 230, priceY + 36);

  ctx.fillStyle = '#526058';
  ctx.font = '13px monospace';
  ctx.fillText(`Scan to open Admin Panel · ${product.id}`, rx, 314);

  return canvas.toDataURL('image/png');
}
