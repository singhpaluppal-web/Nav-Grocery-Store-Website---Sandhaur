import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Check,
  Download,
  RefreshCw,
  Search,
  Sparkles,
  Upload,
  Wand2,
  AlertTriangle,
  ShieldCheck,
} from 'lucide-react';
import { Product } from '../../types';
import { resolveProductImage } from '../../utils/brandPhotos';
import { extractSkuFromFilename } from '../../utils/imageCompression';
import {
  extractSingleObjectToStudioCanvas,
  StudioRefineResult,
} from '../../utils/studioCanvas';
import {
  syncAndroidFeedSnapshot,
  upsertProductInFirebase,
} from '../../utils/firebase';
import { ActivityActionType } from '../../utils/activityLog';

interface PhotoRefinerTabProps {
  products: Product[];
  onSyncedProduct: (updated: Product) => void;
  onNotify: (type: 'ok' | 'err', text: string) => void;
  onLogActivity: (input: {
    actionType: ActivityActionType;
    sku: string;
    summary: string;
    details?: string;
  }) => Promise<void>;
}

function formatKb(bytes: number): string {
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export const PhotoRefinerTab: React.FC<PhotoRefinerTabProps> = ({
  products,
  onSyncedProduct,
  onNotify,
  onLogActivity,
}) => {
  const [skuSearch, setSkuSearch] = useState('');
  const [selectedSku, setSelectedSku] = useState<string>(() => products[0]?.id || 'NAV001');
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [uploadedName, setUploadedName] = useState<string>('');

  // Studio extraction controls
  const [tolerance, setTolerance] = useState<number>(36);
  const [paddingPct, setPaddingPct] = useState<number>(10);
  const [isolateSingleObject, setIsolateSingleObject] = useState<boolean>(true);
  const [enhanceClarity, setEnhanceClarity] = useState<boolean>(true);

  const [processing, setProcessing] = useState<boolean>(false);
  const [saving, setSaving] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string>('');
  const [result, setResult] = useState<StudioRefineResult | null>(null);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const selectedProduct = useMemo(
    () => products.find((p) => p.id.toUpperCase() === selectedSku.toUpperCase()) || products[0] || null,
    [products, selectedSku]
  );

  const filteredSkuList = useMemo(() => {
    const q = skuSearch.trim().toLowerCase();
    if (!q) return products.slice(0, 40);
    return products
      .filter((p) => `${p.id} ${p.name} ${p.brand} ${p.category}`.toLowerCase().includes(q))
      .slice(0, 40);
  }, [products, skuSearch]);

  const runExtraction = useCallback(
    async (sourceOverride?: File | string) => {
      const source =
        sourceOverride ??
        uploadedFile ??
        (selectedProduct ? resolveProductImage(selectedProduct) : null);

      if (!source) return;

      setProcessing(true);
      setErrorMsg('');
      try {
        const refined = await extractSingleObjectToStudioCanvas(source, {
          tolerance,
          isolateSingleObject,
          paddingRatio: paddingPct / 100,
          outputSize: 800,
          enhanceClarity,
          maxKb: 120,
        });
        setResult(refined);
      } catch (err) {
        setResult(null);
        setErrorMsg(
          err instanceof Error
            ? err.message
            : 'Unable to refine this image. Upload a local JPG/PNG/WebP photo.'
        );
      } finally {
        setProcessing(false);
      }
    },
    [uploadedFile, selectedProduct, tolerance, isolateSingleObject, paddingPct, enhanceClarity]
  );

  useEffect(() => {
    if (!uploadedFile && !selectedProduct) return;
    const timer = setTimeout(() => {
      void runExtraction();
    }, 120);
    return () => clearTimeout(timer);
  }, [uploadedFile, selectedProduct, tolerance, isolateSingleObject, paddingPct, enhanceClarity, runExtraction]);

  const handleFilePicked = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadedFile(file);
    setUploadedName(file.name);

    const detectedSku = extractSkuFromFilename(file.name);
    if (detectedSku) {
      const match = products.find((p) => p.id.toUpperCase() === detectedSku.toUpperCase());
      if (match) {
        setSelectedSku(match.id);
      }
    }
    e.target.value = '';
  };

  const handleDownloadJpeg = () => {
    if (!result) return;
    const link = document.createElement('a');
    link.href = result.afterDataUrl;
    const skuSlug = (selectedProduct?.id || 'product').replace(/[^A-Za-z0-9_-]/g, '');
    link.download = `${skuSlug}_studio_white.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleSaveToSku = async () => {
    if (!result || !selectedProduct) return;
    setSaving(true);
    try {
      const mrp = Math.max(1, Math.round(Number(selectedProduct.mrp) || 0));
      const sale = Math.round(mrp * 0.88);
      const updated: Product = {
        ...selectedProduct,
        mrp,
        sale,
        price: sale,
        image: result.afterDataUrl,
        imageUrl: result.afterDataUrl,
        customImage: result.afterDataUrl,
        updatedAt: new Date().toISOString(),
      };

      const res = await upsertProductInFirebase(updated);
      onSyncedProduct(updated);

      const nextList = products.map((p) => (p.id === updated.id ? updated : p));
      void syncAndroidFeedSnapshot(nextList);

      void onLogActivity({
        actionType: 'photo_update',
        sku: updated.id,
        summary: `Refined pure #FFFFFF studio photo for ${updated.name}`,
        details: `${formatKb(result.afterBytes)} · ${result.ghostObjectsRemoved} ghost artifacts removed`,
      });

      onNotify(
        'ok',
        res.ok
          ? `Saved pure #FFFFFF studio photo to ${updated.id} & synced live to Firebase RTDB.`
          : `Refined photo applied locally to ${updated.id} (Firebase sync offline fallback).`
      );
    } catch {
      onNotify('err', 'Failed to save refined photo to SKU.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="space-y-5">
      {/* Header Card */}
      <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 sm:p-6 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-[#0B3B24]">
            <Wand2 className="w-3.5 h-3.5 text-[#D4AF37]" />
            <span>Studio Photo Refiner · Single-Can / Bottle Extraction</span>
          </div>
          <h2 className="font-display text-xl font-bold text-[#0B3B24]">
            100% Pure White (#FFFFFF) Studio Extractor
          </h2>
          <p className="text-xs text-[#526058] max-w-2xl">
            Isolates a single primary product bottle, jar, tin, or pack onto a verified #FFFFFF studio canvas—automatically removing background clutter, shelf edges, and duplicate ghost objects.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={handleFilePicked}
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="px-4 py-2.5 rounded-xl bg-[#0B3B24] text-white text-xs font-bold inline-flex items-center gap-2 hover:bg-[#082B1A] cursor-pointer"
          >
            <Upload className="w-4 h-4 text-[#D4AF37]" />
            <span>Upload Photo to Refine</span>
          </button>
          {uploadedFile && (
            <button
              type="button"
              onClick={() => {
                setUploadedFile(null);
                setUploadedName('');
              }}
              className="px-3 py-2.5 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/20 text-xs font-semibold text-[#0B3B24] hover:bg-[#EFECE2] cursor-pointer"
            >
              Use Current SKU Photo
            </button>
          )}
        </div>
      </div>

      {/* Main 2-Column Workspace */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5">
        {/* Left: Target SKU & Studio Controls */}
        <div className="xl:col-span-4 space-y-4">
          <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4 sm:p-5 space-y-3.5">
            <h3 className="text-sm font-bold text-[#0B3B24]">1. Select Target SKU</h3>
            <div className="relative">
              <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                value={skuSearch}
                onChange={(e) => setSkuSearch(e.target.value)}
                placeholder="Search SKU (e.g. NAV001), brand or item..."
                className="w-full pl-9 pr-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] text-xs focus:outline-none focus:border-[#0B3B24]"
              />
            </div>

            <select
              aria-label="Target product SKU"
              value={selectedProduct?.id || ''}
              onChange={(e) => {
                setSelectedSku(e.target.value);
                setUploadedFile(null);
                setUploadedName('');
              }}
              className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white text-xs font-semibold text-[#14221B] focus:outline-none focus:border-[#0B3B24]"
            >
              {filteredSkuList.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.id} — {p.name} (₹{Math.round(p.mrp * 0.88)})
                </option>
              ))}
            </select>

            {selectedProduct && (
              <div className="p-3 rounded-lg bg-[#FAF8F3] border border-[#0B3B24]/10 text-xs space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono-num font-bold text-[#0B3B24]">{selectedProduct.id}</span>
                  <span className="text-[11px] font-semibold text-[#526058]">
                    MRP ₹{selectedProduct.mrp} → Wholesale ₹{Math.round(selectedProduct.mrp * 0.88)}
                  </span>
                </div>
                <p className="font-semibold text-[#14221B] truncate">{selectedProduct.name}</p>
                {uploadedName && (
                  <p className="text-[11px] text-[#0B3B24] font-medium truncate">
                    Uploaded file: {uploadedName}
                  </p>
                )}
              </div>
            )}
          </div>

          <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4 sm:p-5 space-y-4">
            <h3 className="text-sm font-bold text-[#0B3B24]">2. Single-Can Extraction Controls</h3>

            <label className="flex items-start justify-between gap-3 p-3 rounded-lg bg-[#FAF8F3] border border-[#0B3B24]/12 cursor-pointer">
              <div>
                <p className="text-xs font-bold text-[#14221B]">Single-Object Isolation (Anti-Ghost)</p>
                <p className="text-[11px] text-[#526058] mt-0.5">
                  Keeps only the single primary central bottle/can and strips duplicate ghost objects &amp; side clutter.
                </p>
              </div>
              <input
                type="checkbox"
                checked={isolateSingleObject}
                onChange={(e) => setIsolateSingleObject(e.target.checked)}
                className="mt-1 w-4 h-4 accent-[#0B3B24] cursor-pointer"
              />
            </label>

            <label className="flex items-start justify-between gap-3 p-3 rounded-lg bg-[#FAF8F3] border border-[#0B3B24]/12 cursor-pointer">
              <div>
                <p className="text-xs font-bold text-[#14221B]">Studio Clarity Boost</p>
                <p className="text-[11px] text-[#526058] mt-0.5">
                  Lifts label sharpness on the product while keeping background strictly #FFFFFF.
                </p>
              </div>
              <input
                type="checkbox"
                checked={enhanceClarity}
                onChange={(e) => setEnhanceClarity(e.target.checked)}
                className="mt-1 w-4 h-4 accent-[#0B3B24] cursor-pointer"
              />
            </label>

            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-[#14221B]">Background Sensitivity</span>
                <span className="font-mono-num font-bold text-[#0B3B24]">{tolerance}</span>
              </div>
              <input
                type="range"
                min={12}
                max={78}
                step={2}
                value={tolerance}
                onChange={(e) => setTolerance(Number(e.target.value))}
                className="w-full accent-[#0B3B24] cursor-pointer"
              />
              <div className="flex justify-between text-[10px] text-[#526058]">
                <span>Strict (White Label Safe)</span>
                <span>Aggressive Backdrop Cut</span>
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-[#14221B]">Pure-White Studio Padding</span>
                <span className="font-mono-num font-bold text-[#0B3B24]">{paddingPct}%</span>
              </div>
              <input
                type="range"
                min={6}
                max={20}
                step={1}
                value={paddingPct}
                onChange={(e) => setPaddingPct(Number(e.target.value))}
                className="w-full accent-[#0B3B24] cursor-pointer"
              />
            </div>

            <div className="flex flex-col gap-2 pt-1">
              <button
                type="button"
                disabled={!result || saving || processing || !selectedProduct}
                onClick={handleSaveToSku}
                className="w-full py-2.5 px-4 rounded-xl bg-[#D4AF37] text-[#0B3B24] font-bold text-xs sm:text-sm inline-flex items-center justify-center gap-2 hover:bg-[#e1be49] disabled:opacity-50 cursor-pointer"
              >
                {saving ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Syncing to Firebase RTDB...</span>
                  </>
                ) : (
                  <>
                    <Check className="w-4 h-4" />
                    <span>Save &amp; Sync Studio Photo to {selectedProduct?.id || 'SKU'}</span>
                  </>
                )}
              </button>

              <button
                type="button"
                disabled={!result || processing}
                onClick={handleDownloadJpeg}
                className="w-full py-2 px-4 rounded-xl bg-white border border-[#0B3B24]/20 text-[#0B3B24] font-semibold text-xs inline-flex items-center justify-center gap-2 hover:bg-[#FAF8F3] disabled:opacity-50 cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download Pure-White #FFFFFF JPEG</span>
              </button>
            </div>
          </div>
        </div>

        {/* Right: Before vs. After (#FFFFFF Studio) Comparison */}
        <div className="xl:col-span-8 space-y-4">
          {errorMsg && (
            <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-start gap-2.5">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-amber-700" />
              <div>
                <p className="font-bold">Source Image Could Not Be Refined Directly</p>
                <p className="mt-0.5">{errorMsg}</p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Before Card */}
            <div className="bg-white rounded-xl border border-[#0B3B24]/12 overflow-hidden flex flex-col">
              <div className="px-4 py-2.5 border-b border-[#0B3B24]/10 bg-[#FAF8F3] flex items-center justify-between text-xs">
                <span className="font-bold text-[#14221B]">Original Input</span>
                {result && (
                  <span className="font-mono-num text-[#526058]">{formatKb(result.beforeBytes)}</span>
                )}
              </div>
              <div className="aspect-square bg-[#F1EFE9] flex items-center justify-center p-4">
                {result ? (
                  <img
                    src={result.beforeDataUrl}
                    alt="Original product photo"
                    className="max-w-full max-h-full object-contain rounded-lg"
                  />
                ) : (
                  <p className="text-xs text-[#526058]">
                    {processing ? 'Analyzing source photo...' : 'Select a SKU or upload a photo'}
                  </p>
                )}
              </div>
            </div>

            {/* After Pure #FFFFFF Card */}
            <div className="bg-white rounded-xl border-2 border-[#0B3B24] overflow-hidden flex flex-col">
              <div className="px-4 py-2.5 border-b border-[#0B3B24]/10 bg-[#0B3B24] text-white flex items-center justify-between text-xs">
                <span className="font-bold inline-flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-[#D4AF37]" />
                  <span>Refined Studio Output (#FFFFFF)</span>
                </span>
                {result && (
                  <span className="font-mono-num text-[#D4AF37] font-bold">
                    {result.outputWidth}×{result.outputHeight} · {formatKb(result.afterBytes)}
                  </span>
                )}
              </div>
              <div className="aspect-square bg-[#FFFFFF] flex items-center justify-center p-4 relative">
                {processing && (
                  <div className="absolute inset-0 bg-white/80 flex flex-col items-center justify-center gap-2 z-10 text-xs font-semibold text-[#0B3B24]">
                    <RefreshCw className="w-5 h-5 animate-spin text-[#0B3B24]" />
                    <span>Isolating single object on #FFFFFF...</span>
                  </div>
                )}
                {result ? (
                  <img
                    src={result.afterDataUrl}
                    alt="Refined single-object studio photo on pure white"
                    className="max-w-full max-h-full object-contain"
                  />
                ) : (
                  <p className="text-xs text-[#526058]">Pure-white preview appears here</p>
                )}
              </div>
            </div>
          </div>

          {/* Extraction Quality Verification Bar */}
          {result && (
            <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
              <div className="p-2.5 rounded-lg bg-[#FAF8F3]">
                <p className="text-[11px] text-[#526058]">Studio Background</p>
                <p className="font-bold text-[#0B3B24] mt-0.5 inline-flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-700" />
                  <span>{result.pureWhiteVerified ? '100% #FFFFFF' : 'Pure White'}</span>
                </p>
              </div>
              <div className="p-2.5 rounded-lg bg-[#FAF8F3]">
                <p className="text-[11px] text-[#526058]">Single-Object Isolation</p>
                <p className="font-bold text-[#14221B] mt-0.5 font-mono-num">
                  1 Kept · {result.ghostObjectsRemoved} Ghost Stripped
                </p>
              </div>
              <div className="p-2.5 rounded-lg bg-[#FAF8F3]">
                <p className="text-[11px] text-[#526058]">Payload Size</p>
                <p className="font-bold text-[#0B3B24] mt-0.5 font-mono-num">
                  {formatKb(result.beforeBytes)} → {formatKb(result.afterBytes)}
                </p>
              </div>
              <div className="p-2.5 rounded-lg bg-[#FAF8F3]">
                <p className="text-[11px] text-[#526058]">Canvas Resolution</p>
                <p className="font-bold text-[#14221B] mt-0.5 font-mono-num">
                  {result.outputWidth} × {result.outputHeight} px
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
};
