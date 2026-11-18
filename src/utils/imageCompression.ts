export interface CompressedImageResult {
  dataUrl: string;
  beforeBytes: number;
  afterBytes: number;
  width: number;
  height: number;
}

export function extractSkuFromFilename(filename: string): string | null {
  const base = String(filename || '').replace(/\.[^/.]+$/, '').trim().toUpperCase();
  if (!base) return null;

  // Match NAV001, NAV-001, NAV_001, NAV 1, etc.
  const navMatch = /NAV[-_\s]?0*(\d{1,4})/i.exec(base);
  if (navMatch) {
    const num = parseInt(navMatch[1], 10);
    if (num >= 1 && num <= 999) {
      return `NAV${String(num).padStart(3, '0')}`;
    }
    if (num > 999) {
      return `NAV${num}`;
    }
  }

  // Match bare or prefixed numeric filenames (001.jpg, 1.jpg, 308.jpg, 001_pack.jpg, photo-001.jpg)
  const exactNumMatch = /^0*(\d{1,3})$/.exec(base);
  if (exactNumMatch) {
    const num = parseInt(exactNumMatch[1], 10);
    if (num >= 1 && num <= 308) {
      return `NAV${String(num).padStart(3, '0')}`;
    }
  }

  const leadingNumMatch = /^0*(\d{1,3})(?:[-_\s]+.*)?$/.exec(base);
  if (leadingNumMatch) {
    const num = parseInt(leadingNumMatch[1], 10);
    if (num >= 1 && num <= 308) {
      return `NAV${String(num).padStart(3, '0')}`;
    }
  }

  const trailingNumMatch = /^[A-Z]+[-_\s]+0*(\d{1,3})$/.exec(base);
  if (trailingNumMatch) {
    const num = parseInt(trailingNumMatch[1], 10);
    if (num >= 1 && num <= 308) {
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
  maxWidth = 800,
  jpegQuality = 0.82
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

    const natW = Math.max(1, img.naturalWidth || maxWidth);
    const natH = Math.max(1, img.naturalHeight || maxWidth);
    const scale = natW > maxWidth ? maxWidth / natW : 1;
    const width = Math.max(1, Math.round(natW * scale));
    const height = Math.max(1, Math.round(natH * scale));

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas 2D context unavailable');

    // Fill pure white studio canvas so transparent PNG packs stay crisp on JPEG export
    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, width, height);
    ctx.drawImage(img, 0, 0, width, height);

    const safeQuality = Math.min(0.95, Math.max(0.1, jpegQuality));
    const dataUrl = canvas.toDataURL('image/jpeg', safeQuality);
    const afterBytes = estimateDataUrlBytes(dataUrl);

    return {
      dataUrl,
      beforeBytes,
      afterBytes,
      width,
      height,
    };
  } finally {
    URL.revokeObjectURL(imageUrl);
  }
}
