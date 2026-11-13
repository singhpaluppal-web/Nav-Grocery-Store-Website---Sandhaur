import React, { useEffect, useMemo, useState } from 'react';
import {
  Check,
  Copy,
  Download,
  ExternalLink,
  Pencil,
  Printer,
  QrCode,
  ScanLine,
  Search,
  X,
} from 'lucide-react';
import { Product } from '../../types';
import {
  buildSkuAdminDeepLink,
  extractSkuFromScanInput,
  generateSkuQrDataUrl,
  renderPrintableSkuLabelPng,
} from '../../utils/skuQr';

interface SkuQrLabelsPanelProps {
  products: Product[];
  selectedSkus: Set<string>;
  onOpenQrModal: (product: Product) => void;
  onOpenManagePanel: (product: Product) => void;
  onNotify: (type: 'ok' | 'err', text: string) => void;
}

interface QrThumbProps {
  skuId: string;
  productName: string;
  sizeClass?: string;
}

const QrThumb: React.FC<QrThumbProps> = ({
  skuId,
  productName,
  sizeClass = 'w-28 h-28',
}) => {
  const [src, setSrc] = useState<string>('');

  useEffect(() => {
    let cancelled = false;
    const url = buildSkuAdminDeepLink(skuId);
    generateSkuQrDataUrl(url, 220)
      .then((dataUrl) => {
        if (!cancelled) setSrc(dataUrl);
      })
      .catch(() => {
        if (!cancelled) setSrc('');
      });
    return () => {
      cancelled = true;
    };
  }, [skuId]);

  if (!src) {
    return <div className={`${sizeClass} rounded-lg skeleton-shimmer shrink-0`} />;
  }

  return (
    <img
      src={src}
      alt={`QR ${skuId} ${productName}`}
      className={`${sizeClass} rounded-lg border border-[#0B3B24]/15 bg-white p-1 block shrink-0`}
    />
  );
};

function matchesQrRangeOrText(product: Product, rawQuery: string): boolean {
  const q = rawQuery.trim();
  if (!q) return true;

  const upper = q.toUpperCase();
  const skuUpper = String(product.id || '').toUpperCase();
  const numMatch = /^NAV(\d+)$/.exec(skuUpper);
  const skuNum = numMatch ? parseInt(numMatch[1], 10) : NaN;

  // Check if query looks like SKU tokens / ranges (e.g. "NAV001-NAV024" or "1-12" or "NAV081, NAV090")
  const tokens = upper.split(/[\s,;]+/).filter(Boolean);
  const isSkuTokenQuery = tokens.every(
    (tk) =>
      /^(?:NAV)?\d+(?:\s*[-:]\s*(?:NAV)?\d+)?$/.test(tk) || /^NAV\d*$/i.test(tk)
  );

  if (isSkuTokenQuery) {
    return tokens.some((part) => {
      const rangeMatch = /^(?:NAV)?(\d+)\s*[-:]\s*(?:NAV)?(\d+)$/.exec(part);
      if (rangeMatch && Number.isFinite(skuNum)) {
        const lo = Math.min(parseInt(rangeMatch[1], 10), parseInt(rangeMatch[2], 10));
        const hi = Math.max(parseInt(rangeMatch[1], 10), parseInt(rangeMatch[2], 10));
        return skuNum >= lo && skuNum <= hi;
      }
      if (/^\d+$/.test(part) && Number.isFinite(skuNum)) {
        return skuUpper === `NAV${part.padStart(3, '0')}` || skuUpper.includes(part);
      }
      return skuUpper.includes(part);
    });
  }

  const lower = q.toLowerCase();
  const hay = `${product.id} ${product.brand} ${product.name} ${product.category} ${product.department}`.toLowerCase();
  return lower.split(/\s+/).every((tk) => hay.includes(tk));
}

export const SkuQrLabelsPanel: React.FC<SkuQrLabelsPanelProps> = ({
  products,
  selectedSkus,
  onOpenQrModal,
  onOpenManagePanel,
  onNotify,
}) => {
  const [filterQuery, setFilterQuery] = useState('');
  const [onlySelected, setOnlySelected] = useState(false);
  const [scanInput, setScanInput] = useState('');
  const [page, setPage] = useState(1);
  const [copiedSku, setCopiedSku] = useState<string | null>(null);
  const PAGE_SIZE = 24;

  const filteredList = useMemo(() => {
    return products.filter((p) => {
      if (onlySelected && selectedSkus.size > 0 && !selectedSkus.has(p.id)) {
        return false;
      }
      return matchesQrRangeOrText(p, filterQuery);
    });
  }, [products, onlySelected, selectedSkus, filterQuery]);

  useEffect(() => {
    setPage(1);
  }, [filterQuery, onlySelected]);

  const totalPages = Math.max(1, Math.ceil(filteredList.length / PAGE_SIZE));
  const pagedLabels = filteredList.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const handleScanLookupSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const extractedSku = extractSkuFromScanInput(scanInput);
    if (!extractedSku) {
      onNotify('err', 'Enter or scan a SKU ID (e.g. NAV081) or product QR link.');
      return;
    }
    const matched = products.find((p) => p.id.toUpperCase() === extractedSku);
    if (!matched) {
      onNotify('err', `Scanned SKU "${extractedSku}" was not found in the live catalog.`);
      return;
    }
    setScanInput('');
    onOpenManagePanel(matched);
    onNotify('ok', `Opened management panel for scanned SKU ${matched.id} (${matched.name}).`);
  };

  const handleCopyDeepLink = async (skuId: string) => {
    const url = buildSkuAdminDeepLink(skuId);
    try {
      await navigator.clipboard.writeText(url);
      setCopiedSku(skuId);
      window.setTimeout(() => setCopiedSku(null), 1600);
    } catch {
      onNotify('err', 'Clipboard access blocked.');
    }
  };

  const handleDownloadSticker = async (product: Product) => {
    try {
      const pngUrl = await renderPrintableSkuLabelPng(
        product,
        buildSkuAdminDeepLink(product.id)
      );
      const a = document.createElement('a');
      a.href = pngUrl;
      a.download = `${product.id}_QR_Label.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      onNotify('ok', `Downloaded QR shelf sticker for ${product.id}.`);
    } catch {
      onNotify('err', `Could not export QR sticker for ${product.id}.`);
    }
  };

  return (
    <div className="space-y-5">
      {/* Scanner Quick-Jump & Sheet Controls (hidden during print) */}
      <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-4 sm:p-5 space-y-4 no-print">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-[#0B3B24]/10">
          <div>
            <h2 className="font-display text-lg font-bold text-[#0B3B24]">
              In-Store SKU QR Codes &amp; Printable Shelf Labels
            </h2>
            <p className="text-xs text-[#526058] mt-0.5">
              Each SKU encodes a direct Admin deep-link (<span className="font-mono-num">/admin?sku=NAV001</span>). Scanning any sticker opens that item&apos;s stock, pricing, and photo management panel.
            </p>
          </div>

          <button
            type="button"
            disabled={pagedLabels.length === 0}
            onClick={() => window.print()}
            className="px-4 py-2.5 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold inline-flex items-center gap-2 cursor-pointer shrink-0 disabled:opacity-45"
          >
            <Printer className="w-4 h-4 text-[#D4AF37]" />
            <span>Print Label Sheet ({pagedLabels.length})</span>
          </button>
        </div>

        {/* Scanner Gun / Manual Quick-Open Bar */}
        <form
          onSubmit={handleScanLookupSubmit}
          className="p-3.5 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/15 flex flex-col sm:flex-row sm:items-center gap-3"
        >
          <div className="flex items-center gap-2 text-xs font-bold text-[#0B3B24] shrink-0">
            <ScanLine className="w-4 h-4 text-[#0B3B24]" />
            <label htmlFor="sku-scan-jump">Scan QR URL or SKU ID:</label>
          </div>
          <input
            id="sku-scan-jump"
            type="text"
            value={scanInput}
            onChange={(e) => setScanInput(e.target.value)}
            placeholder="Scan QR / barcode or type NAV081 and press Enter..."
            className="flex-1 px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-white font-mono-num text-xs focus:outline-none focus:border-[#0B3B24]"
          />
          <button
            type="submit"
            className="px-4 py-2 rounded-lg bg-[#D4AF37] hover:bg-[#c5a12f] text-[#0B3B24] text-xs font-bold whitespace-nowrap cursor-pointer shrink-0"
          >
            Open Management Panel
          </button>
        </form>

        {/* Filter Controls */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              aria-label="Filter SKU QR codes"
              value={filterQuery}
              onChange={(e) => setFilterQuery(e.target.value)}
              placeholder="Filter by range (1-24), SKU (NAV081), or name..."
              className="w-full pl-9 pr-8 py-2 rounded-lg border border-[#0B3B24]/20 bg-white text-xs sm:text-sm focus:outline-none focus:border-[#0B3B24]"
            />
            {filterQuery && (
              <button
                type="button"
                onClick={() => setFilterQuery('')}
                aria-label="Clear QR filter"
                className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-[#526058] hover:text-[#14221B] cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2 text-xs">
            {selectedSkus.size > 0 && (
              <button
                type="button"
                onClick={() => setOnlySelected((v) => !v)}
                className={`px-3 py-2 rounded-lg border font-bold cursor-pointer transition-colors ${
                  onlySelected
                    ? 'bg-[#0B3B24] border-[#0B3B24] text-white'
                    : 'bg-[#FAF8F3] border-[#0B3B24]/20 text-[#0B3B24]'
                }`}
              >
                {onlySelected
                  ? `Showing Selected SKUs (${selectedSkus.size})`
                  : `Filter to Selected SKUs (${selectedSkus.size})`}
              </button>
            )}
            <span className="font-mono-num font-bold text-[#526058]">
              {filteredList.length} QR Labels Ready
            </span>
          </div>
        </div>
      </section>

      {/* Printable Shelf Labels Grid */}
      {pagedLabels.length === 0 ? (
        <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-10 text-center space-y-2 no-print">
          <QrCode className="w-8 h-8 text-[#526058]/60 mx-auto" />
          <p className="text-sm font-semibold text-[#14221B]">
            No SKUs match your QR filter.
          </p>
        </section>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 print:grid-cols-2 print:gap-3">
          {pagedLabels.map((product) => {
            const deepLink = buildSkuAdminDeepLink(product.id);
            const inStock = Number(product.stock) > 0;

            return (
              <article
                key={product.id}
                className="bg-white rounded-xl border border-[#0B3B24]/15 p-3.5 flex flex-col justify-between gap-3 break-inside-avoid print:border-[#0B3B24]"
              >
                <div className="flex items-start gap-3">
                  <button
                    type="button"
                    onClick={() => onOpenQrModal(product)}
                    title={`Enlarge QR code for ${product.id}`}
                    className="cursor-pointer shrink-0 focus:outline-none"
                  >
                    <QrThumb skuId={product.id} productName={product.name} />
                  </button>

                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center justify-between gap-1.5">
                      <span className="px-2 py-0.5 rounded bg-[#0B3B24] text-[#D4AF37] font-mono-num text-[11px] font-bold">
                        {product.id}
                      </span>
                      <span
                        className={`text-[10px] font-bold font-mono-num ${
                          inStock ? 'text-[#0B3B24]' : 'text-red-700'
                        }`}
                      >
                        {inStock ? `Qty ${product.stock}` : 'Out of Stock'}
                      </span>
                    </div>

                    <h3 className="text-xs sm:text-sm font-bold text-[#14221B] leading-snug line-clamp-2">
                      {product.name}
                    </h3>

                    <div className="text-[11px] text-[#526058] truncate">
                      {product.brand} · {product.category}
                    </div>

                    <div className="pt-1 flex items-baseline gap-2 font-mono-num">
                      <span className="text-sm font-bold text-[#0B3B24]">
                        ₹{product.sale}
                      </span>
                      <span className="text-[11px] text-[#526058] line-through">
                        MRP ₹{product.mrp}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Action Strip (hidden on physical paper printout) */}
                <div className="pt-2.5 border-t border-[#0B3B24]/10 flex items-center justify-between gap-1.5 text-[11px] no-print">
                  <button
                    type="button"
                    onClick={() => onOpenManagePanel(product)}
                    className="px-2.5 py-1.5 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold inline-flex items-center gap-1 cursor-pointer"
                  >
                    <Pencil className="w-3 h-3 text-[#D4AF37]" />
                    <span>Manage</span>
                  </button>

                  <div className="flex items-center gap-1">
                    <button
                      type="button"
                      onClick={() => handleDownloadSticker(product)}
                      title="Download PNG Shelf Sticker"
                      className="px-2 py-1.5 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] hover:bg-[#EFECE2] text-[#0B3B24] font-semibold inline-flex items-center gap-1 cursor-pointer"
                    >
                      <Download className="w-3 h-3" />
                      <span>PNG</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleCopyDeepLink(product.id)}
                      title="Copy scan deep-link URL"
                      className="p-1.5 rounded-lg border border-[#0B3B24]/20 hover:bg-[#FAF8F3] text-[#0B3B24] cursor-pointer"
                    >
                      {copiedSku === product.id ? (
                        <Check className="w-3.5 h-3.5 text-emerald-700" />
                      ) : (
                        <Copy className="w-3.5 h-3.5" />
                      )}
                    </button>
                    <a
                      href={deepLink}
                      title="Test deep-link URL"
                      className="p-1.5 rounded-lg border border-[#0B3B24]/20 hover:bg-[#FAF8F3] text-[#0B3B24]"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="bg-white rounded-xl border border-[#0B3B24]/12 px-4 py-3 flex items-center justify-between text-xs no-print">
          <span className="text-[#526058] font-mono-num">
            Showing {(page - 1) * PAGE_SIZE + 1}–
            {Math.min(page * PAGE_SIZE, filteredList.length)} of {filteredList.length} QR labels
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={page <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 font-semibold disabled:opacity-40 cursor-pointer"
            >
              Previous
            </button>
            <span className="font-mono-num font-bold">
              Page {page} / {totalPages}
            </span>
            <button
              type="button"
              disabled={page >= totalPages}
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 font-semibold disabled:opacity-40 cursor-pointer"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
