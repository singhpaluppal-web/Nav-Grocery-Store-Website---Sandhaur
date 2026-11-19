/**
 * Pure-White (#FFFFFF) Studio Product Extractor & Single-Can / Single-Bottle Isolator.
 *
 * Pipeline:
 *  1. Border-seeded adaptive BFS flood fill removes shelf/studio background without
 *     eating white labels inside the bottle/can.
 *  2. Morphological opening severs thin shadow bridges to adjacent duplicate objects.
 *  3. Connected-component labeling isolates strictly the single primary central
 *     bottle/can, eliminating duplicate ghost objects, side clutter, and stray artifacts.
 *  4. 1px anti-aliased edge feathering + tight bounding-box centering onto a 100%
 *     pure white (#FFFFFF) square studio canvas.
 */

export interface StudioRefineOptions {
  /** Color distance tolerance for border flood-fill background removal (10–85). Default 36. */
  tolerance?: number;
  /** Keep strictly the single largest central bottle/can and strip duplicate/ghost objects. Default true. */
  isolateSingleObject?: boolean;
  /** Margin around the isolated bottle/can as a fraction of canvas size (0.05–0.22). Default 0.10. */
  paddingRatio?: number;
  /** Output square studio canvas dimensions in px. Default 800. */
  outputSize?: number;
  /** Gentle studio brightness/contrast clarity boost on product pixels only. Default true. */
  enhanceClarity?: boolean;
  /** Maximum target data URL payload in KB for Firebase RTDB friendliness. Default 120. */
  maxKb?: number;
}

export interface StudioBoundingBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface StudioRefineResult {
  beforeDataUrl: string;
  afterDataUrl: string;
  beforeBytes: number;
  afterBytes: number;
  outputWidth: number;
  outputHeight: number;
  componentsFound: number;
  ghostObjectsRemoved: number;
  boundingBox: StudioBoundingBox;
  pureWhiteVerified: boolean;
}

function estimateDataUrlBytes(dataUrl: string): number {
  const commaIdx = dataUrl.indexOf(',');
  if (commaIdx === -1) return dataUrl.length;
  return Math.round(((dataUrl.length - commaIdx - 1) * 3) / 4);
}

function loadImageElement(source: File | string): Promise<{ img: HTMLImageElement; beforeBytes: number }> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    let objectUrlToRevoke: string | null = null;
    let beforeBytes = 0;

    img.onload = () => {
      if (objectUrlToRevoke) URL.revokeObjectURL(objectUrlToRevoke);
      resolve({ img, beforeBytes });
    };
    img.onerror = () => {
      if (objectUrlToRevoke) URL.revokeObjectURL(objectUrlToRevoke);
      reject(new Error('Could not decode product image. Use a local JPG/PNG/WebP or same-origin SKU image.'));
    };

    if (typeof source === 'string') {
      if (!source.startsWith('data:')) {
        img.crossOrigin = 'anonymous';
      } else {
        beforeBytes = estimateDataUrlBytes(source);
      }
      img.src = source;
    } else {
      beforeBytes = source.size;
      objectUrlToRevoke = URL.createObjectURL(source);
      img.src = objectUrlToRevoke;
    }
  });
}

function colorDist(
  r1: number,
  g1: number,
  b1: number,
  r2: number,
  g2: number,
  b2: number
): number {
  const dr = r1 - r2;
  const dg = g1 - g2;
  const db = b1 - b2;
  return Math.sqrt(dr * dr * 0.3 + dg * dg * 0.59 + db * db * 0.11) * 1.732;
}

function isNearWhiteLowSat(r: number, g: number, b: number, threshold: number): boolean {
  const maxC = Math.max(r, g, b);
  const minC = Math.min(r, g, b);
  const sat = maxC - minC;
  return minC >= 255 - threshold && sat <= Math.max(18, threshold * 0.55);
}

interface BgSeed {
  r: number;
  g: number;
  b: number;
}

function sampleBorderSeeds(data: Uint8ClampedArray, w: number, h: number): BgSeed[] {
  const samples: BgSeed[] = [];
  const stepX = Math.max(1, Math.floor(w / 48));
  const stepY = Math.max(1, Math.floor(h / 48));

  const pushPx = (x: number, y: number) => {
    const idx = (y * w + x) * 4;
    if (data[idx + 3] < 20) return;
    samples.push({ r: data[idx], g: data[idx + 1], b: data[idx + 2] });
  };

  for (let x = 0; x < w; x += stepX) {
    pushPx(x, 0);
    pushPx(x, Math.min(h - 1, 2));
    pushPx(x, h - 1);
    pushPx(x, Math.max(0, h - 3));
  }
  for (let y = 0; y < h; y += stepY) {
    pushPx(0, y);
    pushPx(Math.min(w - 1, 2), y);
    pushPx(w - 1, y);
    pushPx(Math.max(0, w - 3), y);
  }

  if (samples.length === 0) {
    return [{ r: 255, g: 255, b: 255 }];
  }

  // Greedy clustering into up to 4 dominant backdrop tones (handles gentle gradients & corners)
  const clusters: Array<{ r: number; g: number; b: number; count: number }> = [];
  for (const s of samples) {
    let matched = false;
    for (const c of clusters) {
      const avgR = c.r / c.count;
      const avgG = c.g / c.count;
      const avgB = c.b / c.count;
      if (colorDist(s.r, s.g, s.b, avgR, avgG, avgB) < 26) {
        c.r += s.r;
        c.g += s.g;
        c.b += s.b;
        c.count += 1;
        matched = true;
        break;
      }
    }
    if (!matched && clusters.length < 6) {
      clusters.push({ r: s.r, g: s.g, b: s.b, count: 1 });
    }
  }

  clusters.sort((a, b) => b.count - a.count);
  const top = clusters.slice(0, 3).map((c) => ({
    r: Math.round(c.r / c.count),
    g: Math.round(c.g / c.count),
    b: Math.round(c.b / c.count),
  }));

  return top.length > 0 ? top : [{ r: 255, g: 255, b: 255 }];
}

function minSeedDist(r: number, g: number, b: number, seeds: BgSeed[]): number {
  let best = 999;
  for (let i = 0; i < seeds.length; i++) {
    const d = colorDist(r, g, b, seeds[i].r, seeds[i].g, seeds[i].b);
    if (d < best) best = d;
  }
  return best;
}

/**
 * 1-pixel morphological erosion followed by 1-pixel dilation on a binary foreground mask
 * (severs hair-thin shadow bridges between adjacent cans/bottles while preserving silhouette).
 */
function morphOpenForeground(fg: Uint8Array, w: number, h: number): Uint8Array {
  const eroded = new Uint8Array(w * h);
  for (let y = 1; y < h - 1; y++) {
    const row = y * w;
    for (let x = 1; x < w - 1; x++) {
      const i = row + x;
      if (
        fg[i] === 1 &&
        fg[i - 1] === 1 &&
        fg[i + 1] === 1 &&
        fg[i - w] === 1 &&
        fg[i + w] === 1
      ) {
        eroded[i] = 1;
      }
    }
  }

  const opened = new Uint8Array(w * h);
  for (let y = 1; y < h - 1; y++) {
    const row = y * w;
    for (let x = 1; x < w - 1; x++) {
      const i = row + x;
      if (
        eroded[i] === 1 ||
        eroded[i - 1] === 1 ||
        eroded[i + 1] === 1 ||
        eroded[i - w] === 1 ||
        eroded[i + w] === 1
      ) {
        opened[i] = 1;
      }
    }
  }
  return opened;
}

interface ComponentStats {
  id: number;
  area: number;
  sumX: number;
  sumY: number;
  minX: number;
  minY: number;
  maxX: number;
  maxY: number;
}

function isolatePrimaryComponent(
  fgMask: Uint8Array,
  w: number,
  h: number,
  isolateSingle: boolean
): {
  finalMask: Uint8Array;
  componentsFound: number;
  ghostObjectsRemoved: number;
  bbox: StudioBoundingBox;
} {
  const totalPixels = w * h;
  const labels = new Int32Array(totalPixels);
  const queue = new Int32Array(totalPixels);
  const components: ComponentStats[] = [];
  let nextLabel = 1;

  for (let i = 0; i < totalPixels; i++) {
    if (fgMask[i] !== 1 || labels[i] !== 0) continue;

    const label = nextLabel++;
    let head = 0;
    let tail = 0;
    queue[tail++] = i;
    labels[i] = label;

    let area = 0;
    let sumX = 0;
    let sumY = 0;
    let minX = w;
    let minY = h;
    let maxX = 0;
    let maxY = 0;

    while (head < tail) {
      const cur = queue[head++];
      const cx = cur % w;
      const cy = (cur - cx) / w;
      area++;
      sumX += cx;
      sumY += cy;
      if (cx < minX) minX = cx;
      if (cx > maxX) maxX = cx;
      if (cy < minY) minY = cy;
      if (cy > maxY) maxY = cy;

      if (cx > 0) {
        const n = cur - 1;
        if (fgMask[n] === 1 && labels[n] === 0) {
          labels[n] = label;
          queue[tail++] = n;
        }
      }
      if (cx < w - 1) {
        const n = cur + 1;
        if (fgMask[n] === 1 && labels[n] === 0) {
          labels[n] = label;
          queue[tail++] = n;
        }
      }
      if (cy > 0) {
        const n = cur - w;
        if (fgMask[n] === 1 && labels[n] === 0) {
          labels[n] = label;
          queue[tail++] = n;
        }
      }
      if (cy < h - 1) {
        const n = cur + w;
        if (fgMask[n] === 1 && labels[n] === 0) {
          labels[n] = label;
          queue[tail++] = n;
        }
      }
    }

    // Ignore tiny dust specks (< 0.25% of frame)
    if (area >= Math.max(24, Math.floor(totalPixels * 0.0025))) {
      components.push({ id: label, area, sumX, sumY, minX, minY, maxX, maxY });
    }
  }

  if (components.length === 0) {
    // Fallback: keep entire center image if background removal was overly aggressive
    const fallback = new Uint8Array(totalPixels);
    fallback.fill(1);
    return {
      finalMask: fallback,
      componentsFound: 1,
      ghostObjectsRemoved: 0,
      bbox: { x: 0, y: 0, width: w, height: h },
    };
  }

  const centerX = w / 2;
  const centerY = h / 2;
  const maxDist = Math.hypot(centerX, centerY) || 1;

  // Score each object: large primary central bottle/can wins over side duplicate cans & corner clutter
  let primary = components[0];
  let bestScore = -1;
  for (const c of components) {
    const cx = c.sumX / c.area;
    const cy = c.sumY / c.area;
    const normDist = Math.hypot(cx - centerX, cy - centerY) / maxDist;
    const touchesEdgePenalty =
      (c.minX <= 1 || c.maxX >= w - 2) && c.area < totalPixels * 0.14 ? 0.45 : 1.0;
    const score = c.area * (1.3 - 0.55 * normDist) * touchesEdgePenalty;
    if (score > bestScore) {
      bestScore = score;
      primary = c;
    }
  }

  const keepIds = new Set<number>();
  if (isolateSingle) {
    keepIds.add(primary.id);
  } else {
    for (const c of components) {
      if (c.area >= primary.area * 0.08) {
        keepIds.add(c.id);
      }
    }
  }

  const finalMask = new Uint8Array(totalPixels);
  let minX = w;
  let minY = h;
  let maxX = 0;
  let maxY = 0;

  for (let i = 0; i < totalPixels; i++) {
    if (keepIds.has(labels[i])) {
      finalMask[i] = 1;
      const x = i % w;
      const y = (i - x) / w;
      if (x < minX) minX = x;
      if (x > maxX) maxX = x;
      if (y < minY) minY = y;
      if (y > maxY) maxY = y;
    }
  }

  if (maxX <= minX || maxY <= minY) {
    minX = 0;
    minY = 0;
    maxX = w - 1;
    maxY = h - 1;
  }

  return {
    finalMask,
    componentsFound: components.length,
    ghostObjectsRemoved: Math.max(0, components.length - keepIds.size),
    bbox: {
      x: minX,
      y: minY,
      width: maxX - minX + 1,
      height: maxY - minY + 1,
    },
  };
}

/**
 * Extracts a single clean product bottle/can from a source photo onto a 100% pure white (#FFFFFF)
 * square studio canvas with zero ghost duplicates or stray background artifacts.
 */
export async function extractSingleObjectToStudioCanvas(
  source: File | string,
  options: StudioRefineOptions = {}
): Promise<StudioRefineResult> {
  const tolerance = Math.min(85, Math.max(10, options.tolerance ?? 36));
  const isolateSingleObject = options.isolateSingleObject ?? true;
  const paddingRatio = Math.min(0.22, Math.max(0.05, options.paddingRatio ?? 0.1));
  const outputSize = Math.min(1200, Math.max(400, options.outputSize ?? 800));
  const enhanceClarity = options.enhanceClarity ?? true;
  const maxKb = options.maxKb ?? 120;

  const { img, beforeBytes: rawBeforeBytes } = await loadImageElement(source);

  const naturalW = img.naturalWidth || img.width || 600;
  const naturalH = img.naturalHeight || img.height || 600;

  // Working resolution capped at 720px on longest side for fast, sharp mask calculation
  const maxWorkDim = 720;
  const workScale = Math.min(1, maxWorkDim / Math.max(naturalW, naturalH));
  const workW = Math.max(32, Math.round(naturalW * workScale));
  const workH = Math.max(32, Math.round(naturalH * workScale));

  const workCanvas = document.createElement('canvas');
  workCanvas.width = workW;
  workCanvas.height = workH;
  const workCtx = workCanvas.getContext('2d', { willReadFrequently: true });
  if (!workCtx) {
    throw new Error('Canvas 2D context is unavailable in this browser.');
  }

  // Composite original onto pure white first so transparent PNGs behave consistently
  workCtx.fillStyle = '#FFFFFF';
  workCtx.fillRect(0, 0, workW, workH);
  workCtx.drawImage(img, 0, 0, workW, workH);

  const beforeDataUrl = workCanvas.toDataURL('image/jpeg', 0.86);
  const beforeBytes = rawBeforeBytes || estimateDataUrlBytes(beforeDataUrl);

  let imageData: ImageData;
  try {
    imageData = workCtx.getImageData(0, 0, workW, workH);
  } catch {
    throw new Error(
      'Unable to read pixels from cross-origin image. Upload the product photo file directly to refine it.'
    );
  }

  const data = imageData.data;
  const totalPixels = workW * workH;
  const seeds = sampleBorderSeeds(data, workW, workH);

  // Step 1: Border-seeded BFS background flood fill
  const isBg = new Uint8Array(totalPixels);
  const queue = new Int32Array(totalPixels);
  let head = 0;
  let tail = 0;

  const trySeedBorder = (x: number, y: number) => {
    const p = y * workW + x;
    if (isBg[p]) return;
    const idx = p * 4;
    const r = data[idx];
    const g = data[idx + 1];
    const b = data[idx + 2];
    const dSeed = minSeedDist(r, g, b, seeds);
    if (dSeed <= tolerance * 1.25 || isNearWhiteLowSat(r, g, b, tolerance)) {
      isBg[p] = 1;
      queue[tail++] = p;
    }
  };

  for (let x = 0; x < workW; x++) {
    trySeedBorder(x, 0);
    trySeedBorder(x, workH - 1);
  }
  for (let y = 0; y < workH; y++) {
    trySeedBorder(0, y);
    trySeedBorder(workW - 1, y);
  }

  const localStepLimit = Math.max(12, tolerance * 0.48);

  while (head < tail) {
    const cur = queue[head++];
    const cx = cur % workW;
    const cy = (cur - cx) / workW;
    const cIdx = cur * 4;
    const cr = data[cIdx];
    const cg = data[cIdx + 1];
    const cb = data[cIdx + 2];

    const checkNeighbor = (np: number) => {
      if (isBg[np]) return;
      const nIdx = np * 4;
      const nr = data[nIdx];
      const ng = data[nIdx + 1];
      const nb = data[nIdx + 2];

      const dSeed = minSeedDist(nr, ng, nb, seeds);
      const dLocal = colorDist(cr, cg, cb, nr, ng, nb);

      if (
        (dSeed <= tolerance && dLocal <= localStepLimit * 1.65) ||
        (dLocal <= localStepLimit * 0.72 && dSeed <= tolerance * 1.45) ||
        (isNearWhiteLowSat(nr, ng, nb, tolerance * 0.85) && dLocal <= localStepLimit * 1.2)
      ) {
        isBg[np] = 1;
        queue[tail++] = np;
      }
    };

    if (cx > 0) checkNeighbor(cur - 1);
    if (cx < workW - 1) checkNeighbor(cur + 1);
    if (cy > 0) checkNeighbor(cur - workW);
    if (cy < workH - 1) checkNeighbor(cur + workW);
  }

  // Build raw foreground mask (1 = product, 0 = background)
  const rawFg = new Uint8Array(totalPixels);
  let rawFgCount = 0;
  for (let i = 0; i < totalPixels; i++) {
    if (!isBg[i]) {
      rawFg[i] = 1;
      rawFgCount++;
    }
  }

  // Step 2: Morphological opening to break thin shadow bridges to ghost duplicates
  const openedFg = rawFgCount > totalPixels * 0.02 ? morphOpenForeground(rawFg, workW, workH) : rawFg;

  // Step 3: Connected-component single-can / single-bottle isolation
  const { finalMask, componentsFound, ghostObjectsRemoved, bbox } = isolatePrimaryComponent(
    openedFg,
    workW,
    workH,
    isolateSingleObject
  );

  // Step 4: Build clean RGBA isolated foreground buffer with 1px feathered anti-aliased edge
  const fgCanvas = document.createElement('canvas');
  fgCanvas.width = bbox.width;
  fgCanvas.height = bbox.height;
  const fgCtx = fgCanvas.getContext('2d');
  if (!fgCtx) {
    throw new Error('Unable to initialize foreground extraction buffer.');
  }

  const fgImgData = fgCtx.createImageData(bbox.width, bbox.height);
  const out = fgImgData.data;

  for (let dy = 0; dy < bbox.height; dy++) {
    const sy = bbox.y + dy;
    for (let dx = 0; dx < bbox.width; dx++) {
      const sx = bbox.x + dx;
      const p = sy * workW + sx;
      const dstIdx = (dy * bbox.width + dx) * 4;

      if (finalMask[p] === 0) {
        out[dstIdx] = 255;
        out[dstIdx + 1] = 255;
        out[dstIdx + 2] = 255;
        out[dstIdx + 3] = 0;
        continue;
      }

      // Compute 3x3 neighborhood coverage for soft 1px edge feathering
      let fgNeighbors = 0;
      let sampleCount = 0;
      for (let ny = Math.max(0, sy - 1); ny <= Math.min(workH - 1, sy + 1); ny++) {
        const nRow = ny * workW;
        for (let nx = Math.max(0, sx - 1); nx <= Math.min(workW - 1, sx + 1); nx++) {
          sampleCount++;
          if (finalMask[nRow + nx] === 1) fgNeighbors++;
        }
      }

      const srcIdx = p * 4;
      let r = data[srcIdx];
      let g = data[srcIdx + 1];
      let b = data[srcIdx + 2];

      // Subtle studio clarity lift strictly on foreground bottle/can pixels
      if (enhanceClarity) {
        r = Math.min(255, Math.max(0, Math.round((r - 128) * 1.06 + 131)));
        g = Math.min(255, Math.max(0, Math.round((g - 128) * 1.06 + 131)));
        b = Math.min(255, Math.max(0, Math.round((b - 128) * 1.06 + 131)));
      }

      let alpha = 255;
      if (fgNeighbors < sampleCount) {
        const edgeRatio = fgNeighbors / sampleCount;
        alpha = Math.min(255, Math.max(45, Math.round(edgeRatio * 255)));
        // Gentle white-edge de-fringe to prevent dark halo on pure #FFFFFF backdrop
        const lift = (1 - edgeRatio) * 0.25;
        r = Math.round(r + (255 - r) * lift);
        g = Math.round(g + (255 - g) * lift);
        b = Math.round(b + (255 - b) * lift);
      }

      out[dstIdx] = r;
      out[dstIdx + 1] = g;
      out[dstIdx + 2] = b;
      out[dstIdx + 3] = alpha;
    }
  }

  fgCtx.putImageData(fgImgData, 0, 0);

  // Step 5: Composite onto 100% pure white (#FFFFFF) square studio canvas
  const studioCanvas = document.createElement('canvas');
  studioCanvas.width = outputSize;
  studioCanvas.height = outputSize;
  const studioCtx = studioCanvas.getContext('2d', { willReadFrequently: true });
  if (!studioCtx) {
    throw new Error('Unable to initialize pure-white studio canvas.');
  }

  studioCtx.fillStyle = '#FFFFFF';
  studioCtx.fillRect(0, 0, outputSize, outputSize);
  studioCtx.imageSmoothingEnabled = true;
  studioCtx.imageSmoothingQuality = 'high';

  const usableSpan = outputSize * (1 - paddingRatio * 2);
  const fitScale = Math.min(usableSpan / bbox.width, usableSpan / bbox.height);
  const drawW = Math.max(1, Math.round(bbox.width * fitScale));
  const drawH = Math.max(1, Math.round(bbox.height * fitScale));
  const drawX = Math.round((outputSize - drawW) / 2);
  const drawY = Math.round((outputSize - drawH) / 2);

  studioCtx.drawImage(fgCanvas, 0, 0, bbox.width, bbox.height, drawX, drawY, drawW, drawH);

  // Verify all 4 outer studio corners are strictly #FFFFFF (255, 255, 255)
  const checkCornerWhite = (cx: number, cy: number): boolean => {
    const px = studioCtx.getImageData(cx, cy, 1, 1).data;
    return px[0] === 255 && px[1] === 255 && px[2] === 255;
  };
  const pureWhiteVerified =
    checkCornerWhite(0, 0) &&
    checkCornerWhite(outputSize - 1, 0) &&
    checkCornerWhite(0, outputSize - 1) &&
    checkCornerWhite(outputSize - 1, outputSize - 1);

  // Export compressed pure-white JPEG targeting <= maxKb
  let quality = 0.9;
  let afterDataUrl = studioCanvas.toDataURL('image/jpeg', quality);
  let afterBytes = estimateDataUrlBytes(afterDataUrl);

  while (afterBytes > maxKb * 1024 && quality > 0.56) {
    quality = Number((quality - 0.08).toFixed(2));
    afterDataUrl = studioCanvas.toDataURL('image/jpeg', quality);
    afterBytes = estimateDataUrlBytes(afterDataUrl);
  }

  return {
    beforeDataUrl,
    afterDataUrl,
    beforeBytes,
    afterBytes,
    outputWidth: outputSize,
    outputHeight: outputSize,
    componentsFound,
    ghostObjectsRemoved,
    boundingBox: bbox,
    pureWhiteVerified,
  };
}
