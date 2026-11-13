import React, { useEffect, useState } from 'react';
import { Check, Copy, Download, ExternalLink, Pencil, QrCode, X } from 'lucide-react';
import { Product } from '../../types';
import {
  buildSkuAdminDeepLink,
  generateSkuQrDataUrl,
  renderPrintableSkuLabelPng,
} from '../../utils/skuQr';

interface SkuQrModalProps {
  product: Product | null;
  onClose: () => void;
  onOpenManagePanel: (product: Product) => void;
  onNotify: (type: 'ok' | 'err', text: string) => void;
}

export const SkuQrModal: React.FC<SkuQrModalProps> = ({
  product,
  onClose,
  onOpenManagePanel,
  onNotify,
}) => {
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [copied, setCopied] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const deepLink = product ? buildSkuAdminDeepLink(product.id) : '';

  useEffect(() => {
    if (!product || !deepLink) {
      setQrDataUrl('');
      return;
    }
    let cancelled = false;
    generateSkuQrDataUrl(deepLink, 340)
      .then((url) => {
        if (!cancelled) setQrDataUrl(url);
      })
      .catch(() => {
        if (!cancelled) onNotify('err', `Could not generate QR code for ${product.id}.`);
      });
    return () => {
      cancelled = true;
    };
  }, [product, deepLink, onNotify]);

  if (!product) return null;

  const inStock = Number(product.stock) > 0;

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(deepLink);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      onNotify('err', 'Clipboard access blocked. Copy the deep-link URL manually.');
    }
  };

  const handleDownloadSticker = async () => {
    setDownloading(true);
    try {
      const pngUrl = await renderPrintableSkuLabelPng(product, deepLink);
      const a = document.createElement('a');
      a.href = pngUrl;
      a.download = `${product.id}_QR_Label.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      onNotify('ok', `Downloaded printable QR sticker for ${product.id}.`);
    } catch {
      onNotify('err', `Could not create PNG sticker for ${product.id}.`);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 no-print"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl max-w-md w-full overflow-hidden shadow-2xl border border-[#0B3B24]/20"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-5 py-3.5 bg-[#0B3B24] text-white flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-bold font-mono-num">
            <QrCode className="w-4 h-4 text-[#D4AF37]" />
            <span>SKU QR Code · {product.id}</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close QR modal"
            className="p-1 rounded hover:bg-white/15 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-5 space-y-4">
          {/* Scannable QR Preview */}
          <div className="bg-[#FAF8F3] border border-[#0B3B24]/15 rounded-xl p-4 flex flex-col items-center text-center">
            {qrDataUrl ? (
              <div className="bg-white p-3 rounded-xl border border-[#0B3B24]/15 shadow-sm">
                <img
                  src={qrDataUrl}
                  alt={`QR code for ${product.id} ${product.name}`}
                  className="w-52 h-52 block"
                />
              </div>
            ) : (
              <div className="w-52 h-52 rounded-xl skeleton-shimmer" />
            )}

            <div className="mt-3 space-y-1">
              <div className="inline-flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded bg-[#0B3B24] text-[#D4AF37] font-mono-num text-xs font-bold">
                  {product.id}
                </span>
                <span
                  className={`px-2 py-0.5 rounded text-[11px] font-bold font-mono-num ${
                    inStock
                      ? 'bg-emerald-100 text-[#0B3B24]'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {inStock ? `In Stock (${product.stock})` : 'Out of Stock'}
                </span>
              </div>
              <h3 className="font-bold text-[#14221B] text-sm leading-snug">
                {product.name}
              </h3>
              <p className="text-xs text-[#526058]">
                {product.brand} · MRP{' '}
                <span className="line-through font-mono-num">₹{product.mrp}</span> ·{' '}
                <strong className="text-[#0B3B24] font-mono-num">
                  12% Off ₹{product.sale}
                </strong>
              </p>
            </div>
          </div>

          {/* Scan Target URL */}
          <div className="space-y-1.5">
            <div className="text-[11px] font-bold uppercase tracking-wider text-[#526058]">
              In-Store Scan Deep-Link
            </div>
            <div className="flex items-center gap-2">
              <code className="flex-1 text-[11px] font-mono-num bg-[#FAF8F3] px-2.5 py-2 rounded-lg border border-[#0B3B24]/15 text-[#14221B] overflow-x-auto whitespace-nowrap">
                {deepLink}
              </code>
              <button
                type="button"
                onClick={handleCopyLink}
                className="px-2.5 py-2 rounded-lg border border-[#0B3B24]/25 bg-white hover:bg-[#FAF8F3] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1 shrink-0 cursor-pointer"
              >
                {copied ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-700" />
                    <span>Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>Copy</span>
                  </>
                )}
              </button>
              <a
                href={deepLink}
                aria-label={`Open management link for ${product.id}`}
                className="p-2 rounded-lg border border-[#0B3B24]/20 bg-white text-[#0B3B24] hover:bg-[#FAF8F3] shrink-0"
              >
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>
          </div>

          {/* Actions */}
          <div className="grid grid-cols-2 gap-2.5 pt-2 border-t border-[#0B3B24]/10">
            <button
              type="button"
              disabled={downloading || !qrDataUrl}
              onClick={handleDownloadSticker}
              className="px-3.5 py-2.5 rounded-xl border border-[#0B3B24]/25 bg-[#FAF8F3] hover:bg-[#EFECE2] text-[#0B3B24] text-xs font-bold inline-flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-45"
            >
              <Download className="w-4 h-4" />
              <span>{downloading ? 'Rendering...' : 'Download Sticker'}</span>
            </button>

            <button
              type="button"
              onClick={() => {
                onClose();
                onOpenManagePanel(product);
              }}
              className="px-3.5 py-2.5 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold inline-flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <Pencil className="w-4 h-4 text-[#D4AF37]" />
              <span>Manage SKU</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
