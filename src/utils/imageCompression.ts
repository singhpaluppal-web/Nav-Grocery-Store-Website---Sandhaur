export interface CompressedImageResult {
  dataUrl: string;
  beforeBytes: number;
  afterBytes: number;
  width: number;
  height: number;
}

export function extractSkuFromFilename(filename: string): string | null {
  const base = String(filename || '').replace(/\.[^/.]+$/, '').trim().toUpperCase();
  const navMatch = /NAV[-_\s]?(\d{1,3})/i.exec(base);
  if (navMatch) {
    const num = parseInt(navMatch[1], 10);
    if (num >= 1 && num <= 999) {
      return `NAV${String(num).padStart(3, '0')}`;
    }
  }
  const cleanAlnum = base.replace(/[^A-Z0-9_-]/g, '');
  return cleanAlnum || null;
}

export function estimateDataUrlBytes(dataUrl: string): number {
  const commaIdx = dataUrl.indexOf(',');
  if (commaIdx === -1) return dataUrl.length;
  const b64 = dataUrl.slice(commaIdx + 1);
  return Math.round((b64.length * 3) / 4);
}

export async function compressImageFileToDataUrl(
  file: File,
  maxDimension = 400,
  targetMaxBytes = 32 * 1024
): Promise<CompressedImageResult> {
  const beforeBytes = file.size;

  const imageUrl = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error(`Unable to decode image: ${file.name}`));
      el.src = imageUrl;
    });

    const scale = Math.min(1, maxDimension / Math.max(img.naturalWidth || 1, img.naturalHeight || 1));
    const width = Math.max(1, Math.round((img.naturalWidth || maxDimension) * scale));
    const height = Math.max(1, Math.round((img.naturalHeight || maxDimension) * scale));

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas 2D context unavailable');

    // Fill pure white studio canvas so transparent PNG packs stay crisp on JPEG export
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(img, 0, 0, width, height);

    const qualities = [0.84, 0.72, 0.6, 0.48, 0.36];
    let bestDataUrl = canvas.toDataURL('image/jpeg', 0.82);
    let bestBytes = estimateDataUrlBytes(bestDataUrl);

    for (const q of qualities) {
      const candidate = canvas.toDataURL('image/jpeg', q);
      const bytes = estimateDataUrlBytes(candidate);
      bestDataUrl = candidate;
      bestBytes = bytes;
      if (bytes <= targetMaxBytes) {
        break;
      }
    }

    return {
      dataUrl: bestDataUrl,
      beforeBytes,
      afterBytes: bestBytes,
      width,
      height,
    };
  } finally {
    URL.revokeObjectURL(imageUrl);
  }
}
