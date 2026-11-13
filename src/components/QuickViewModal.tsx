import React, { useEffect, useState } from 'react';
import {
  X,
  Plus,
  Minus,
  Snowflake,
  ShieldCheck,
  CheckCircle2,
  ChevronDown,
  Clock,
  AlertTriangle,
  ListChecks,
  Heart,
  Share2,
  Check,
  Calculator,
} from 'lucide-react';
import { Product } from '../types';
import { buildBrandPackageSvg, resolveProductImage } from '../utils/brandPhotos';
import { getProductSpecification } from '../utils/productDetails';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface QuickViewModalProps {
  product: Product | null;
  qtyInCart: number;
  lang?: StoreLang;
  isFavorite?: boolean;
  onToggleFavorite?: (productId: string) => void;
  onClose: () => void;
  onAdd: (product: Product, qty?: number) => void;
  onUpdateQty: (productId: string, qty: number) => void;
}

type DetailSection = 'ingredients' | 'shelfLife' | 'allergens';

export const QuickViewModal: React.FC<QuickViewModalProps> = ({
  product,
  qtyInCart,
  lang = 'en',
  isFavorite = false,
  onToggleFavorite,
  onClose,
  onAdd,
  onUpdateQty,
}) => {
  const t = getStoreTranslations(lang);
  const [imgSrc, setImgSrc] = useState<string>('');
  const [linkCopied, setLinkCopied] = useState<boolean>(false);
  const [bulkQty, setBulkQty] = useState<number>(2);
  const [bulkAdded, setBulkAdded] = useState<boolean>(false);
  const [openSections, setOpenSections] = useState<Record<DetailSection, boolean>>({
    ingredients: true,
    shelfLife: true,
    allergens: true,
  });

  useEffect(() => {
    if (product) {
      setImgSrc(resolveProductImage(product));
      setLinkCopied(false);
      setBulkAdded(false);
      const stockCap = Number(product.stock) > 0 ? Math.min(2, Number(product.stock)) : 2;
      setBulkQty(stockCap);
      setOpenSections({
        ingredients: true,
        shelfLife: true,
        allergens: true,
      });
    }
  }, [product]);

  useEffect(() => {
    if (!product) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [product, onClose]);

  if (!product) return null;

  const mrp = Math.round(Number(product.mrp) || 0);
  const sale = Math.round(Number(product.sale || product.price) || 0);
  const savings = Math.max(0, mrp - sale);
  const discountPct = mrp > sale ? Math.round((savings / mrp) * 100) : 0;
  const maxBulkQty = Number(product.stock) > 0 ? Math.min(99, Number(product.stock)) : 99;
  const safeBulkQty = Math.max(1, Math.min(maxBulkQty, bulkQty));
  const bulkMrpTotal = mrp * safeBulkQty;
  const bulkSaleTotal = sale * safeBulkQty;
  const bulkSavings = Math.max(0, bulkMrpTotal - bulkSaleTotal);
  const spec = getProductSpecification(product);
  const allExpanded =
    openSections.ingredients && openSections.shelfLife && openSections.allergens;

  const handleAddBulkToCart = () => {
    if (Number(product.stock) <= 0) return;
    onAdd(product, safeBulkQty);
    setBulkAdded(true);
    window.setTimeout(() => setBulkAdded(false), 1800);
  };

  const toggleSection = (key: DetailSection) => {
    setOpenSections((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleExpandAll = () => {
    const next = !allExpanded;
    setOpenSections({
      ingredients: next,
      shelfLife: next,
      allergens: next,
    });
  };

  const copyProductLink = async () => {
    const url = new URL(window.location.href);
    url.search = `?sku=${encodeURIComponent(product.id)}`;
    url.hash = '';
    const shareUrl = url.toString();

    let copied = false;
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(shareUrl);
        copied = true;
      }
    } catch {
      copied = false;
    }

    if (!copied) {
      try {
        const ta = document.createElement('textarea');
        ta.value = shareUrl;
        ta.setAttribute('readonly', '');
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        copied = document.execCommand('copy');
        document.body.removeChild(ta);
      } catch {
        copied = false;
      }
    }

    if (copied) {
      setLinkCopied(true);
      window.setTimeout(() => setLinkCopied(false), 2500);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={`${product.name} Detailed Quick View`}
    >
      <div
        className="bg-white rounded-2xl max-w-3xl w-full max-h-[92vh] flex flex-col overflow-hidden shadow-2xl border border-[#0F3D28]/15"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-3.5 bg-[#0F3D28] text-white shrink-0 gap-2">
          <div className="flex flex-wrap items-center gap-2 text-xs font-mono-num min-w-0">
            <span className="text-[#E6A817] font-bold">{product.id}</span>
            <span>·</span>
            <span className="truncate">{product.department}</span>
            <span className="hidden sm:inline">·</span>
            <span
              className={`hidden sm:inline px-2 py-0.5 rounded font-sans text-[11px] font-semibold ${
                spec.isNonFood
                  ? 'bg-white/15 text-white'
                  : 'bg-[#15803D] text-white border border-white/25'
              }`}
            >
              {spec.isNonFood ? 'Household & Personal Care' : '100% Vegetarian'}
            </span>
          </div>
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            <button
              type="button"
              onClick={copyProductLink}
              aria-label={`Copy direct link to ${product.name}`}
              className={`px-2.5 py-1 rounded-lg text-xs font-semibold inline-flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap ${
                linkCopied
                  ? 'bg-[#E6A817] text-[#14221B]'
                  : 'bg-white/10 hover:bg-white/20 text-white'
              }`}
            >
              {linkCopied ? <Check className="w-3.5 h-3.5" /> : <Share2 className="w-3.5 h-3.5" />}
              <span>{linkCopied ? t.quickViewLinkCopied : t.quickViewShare}</span>
            </button>
            {onToggleFavorite && (
              <button
                type="button"
                onClick={() => onToggleFavorite(product.id)}
                aria-pressed={isFavorite}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold inline-flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap ${
                  isFavorite
                    ? 'bg-[#E6A817] text-[#14221B]'
                    : 'bg-white/10 hover:bg-white/20 text-white'
                }`}
              >
                <Heart className={`w-3.5 h-3.5 ${isFavorite ? 'fill-[#14221B]' : ''}`} />
                <span>{isFavorite ? 'Favorited' : 'Save'}</span>
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              aria-label="Close product quick view"
              className="p-1.5 rounded-lg hover:bg-white/15 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="overflow-y-auto grid grid-cols-1 md:grid-cols-12 flex-1">
          {/* Left Studio Packaging Image Column */}
          <div className="md:col-span-5 bg-white p-6 flex flex-col items-center justify-between gap-4 border-b md:border-b-0 md:border-r border-[#0F3D28]/10">
            <div className="relative w-full aspect-square max-h-64 flex items-center justify-center bg-white rounded-xl border border-[#0F3D28]/10 p-3">
              <img
                src={imgSrc || resolveProductImage(product)}
                alt={product.name}
                className="w-full h-full object-contain"
                referrerPolicy="no-referrer"
                onError={() => {
                  const svg = buildBrandPackageSvg(product);
                  if (imgSrc !== svg) {
                    setImgSrc(svg);
                  }
                }}
              />
              {discountPct > 0 && (
                <span className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded bg-[#E6A817] text-[#14221B] text-[11px] font-bold tabular-nums">
                  -{discountPct}% OFF
                </span>
              )}
              {spec.isFrozen && (
                <span className="absolute top-2.5 right-2.5 inline-flex items-center gap-1 px-2 py-0.5 rounded bg-[#E0F2FE] text-[#0369A1] text-[11px] font-semibold">
                  <Snowflake className="w-3 h-3" />
                  -18°C
                </span>
              )}
            </div>

            <div className="w-full space-y-2 text-xs text-[#526058] bg-[#F8F7F2] rounded-xl p-3.5 border border-[#0F3D28]/10">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-[#0F3D28] shrink-0" />
                <span>Sealed Factory Pack · GST {product.gst || '5%'} Included</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#0F3D28] shrink-0" />
                <span>
                  {Number(product.stock) > 0
                    ? `In Stock (${product.stock} units at Main Bazaar, Sandhaur)`
                    : 'Currently Out of Stock'}
                </span>
              </div>
              {product.barcode && (
                <div className="font-mono-num text-[11px] text-[#526058] pt-0.5">
                  EAN / Barcode: {product.barcode}
                </div>
              )}
            </div>
          </div>

          {/* Right Expandable Product Specifications & Cart Column */}
          <div className="md:col-span-7 p-5 sm:p-6 flex flex-col justify-between gap-5">
            <div className="space-y-4">
              <div>
                <div className="text-xs font-semibold uppercase tracking-wider text-[#526058]">
                  {product.brand} · {product.category}
                </div>
                <h2 className="font-display text-xl sm:text-2xl font-bold text-[#14221B] leading-snug mt-1">
                  {product.name}
                </h2>

                <div className="flex flex-wrap items-baseline gap-2.5 tabular-nums pt-2">
                  <span className="text-2xl font-bold text-[#0F3D28]">₹{sale}</span>
                  {mrp > sale && (
                    <>
                      <span className="text-sm text-[#526058] line-through">MRP ₹{mrp}</span>
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-[#E6A817]/25 text-[#14221B]">
                        Save ₹{savings} ({discountPct}% Off)
                      </span>
                    </>
                  )}
                </div>
              </div>

              {/* Interactive Bulk Price & Savings Calculator */}
              <div className="rounded-xl border border-[#0F3D28]/20 bg-[#F8F7F2] p-3.5 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#0F3D28]">
                    <Calculator className="w-4 h-4 text-[#0F3D28]" />
                    <span>{t.bulkCalcTitle}</span>
                  </span>
                  <div className="flex items-center gap-1">
                    {[2, 5, 10, 20].map((preset) => {
                      const disabledPreset = Number(product.stock) > 0 && preset > Number(product.stock);
                      const isActive = safeBulkQty === preset;
                      return (
                        <button
                          key={preset}
                          type="button"
                          disabled={disabledPreset}
                          onClick={() => setBulkQty(preset)}
                          className={`px-2 py-0.5 rounded-md text-[11px] font-bold font-mono-num transition-colors cursor-pointer disabled:opacity-35 disabled:cursor-not-allowed ${
                            isActive
                              ? 'bg-[#0F3D28] text-[#E6A817]'
                              : 'bg-white border border-[#0F3D28]/20 text-[#14221B] hover:border-[#0F3D28]/50'
                          }`}
                        >
                          {preset}×
                        </button>
                      );
                    })}
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="inline-flex items-center gap-1.5">
                    <span className="text-[11px] font-semibold text-[#526058] mr-1">{t.bulkQtyLabel}:</span>
                    <div className="inline-flex items-center rounded-lg border border-[#0F3D28]/25 bg-white overflow-hidden">
                      <button
                        type="button"
                        onClick={() => setBulkQty(Math.max(1, safeBulkQty - 1))}
                        aria-label="Decrease bulk quantity"
                        className="w-8 h-8 flex items-center justify-center text-[#0F3D28] hover:bg-[#EFECE2] cursor-pointer"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <input
                        type="number"
                        min={1}
                        max={maxBulkQty}
                        aria-label="Bulk quantity"
                        value={safeBulkQty}
                        onChange={(e) => {
                          const v = parseInt(e.target.value, 10);
                          setBulkQty(Number.isNaN(v) ? 1 : Math.max(1, Math.min(maxBulkQty, v)));
                        }}
                        className="w-12 text-center text-xs font-bold font-mono-num text-[#14221B] focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                      />
                      <button
                        type="button"
                        onClick={() => setBulkQty(Math.min(maxBulkQty, safeBulkQty + 1))}
                        aria-label="Increase bulk quantity"
                        className="w-8 h-8 flex items-center justify-center text-[#0F3D28] hover:bg-[#EFECE2] cursor-pointer"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  <button
                    type="button"
                    disabled={Number(product.stock) <= 0}
                    onClick={handleAddBulkToCart}
                    className={`px-3.5 py-2 rounded-lg text-xs font-bold transition-colors cursor-pointer whitespace-nowrap disabled:opacity-45 disabled:cursor-not-allowed ${
                      bulkAdded
                        ? 'bg-[#E6A817] text-[#14221B]'
                        : 'bg-[#0F3D28] text-white hover:bg-[#0A2A1B]'
                    }`}
                  >
                    {bulkAdded
                      ? `✓ ${safeBulkQty} ${t.bulkPacksUnit}`
                      : `+ ${t.bulkAddCta} ${safeBulkQty} ${t.bulkPacksUnit} · ₹${bulkSaleTotal.toLocaleString('en-IN')}`}
                  </button>
                </div>

                <div className="grid grid-cols-3 gap-2 pt-2 border-t border-[#0F3D28]/12 text-xs tabular-nums">
                  <div className="bg-white rounded-lg px-2.5 py-2 border border-[#0F3D28]/10">
                    <div className="text-[10px] font-semibold text-[#526058] uppercase">{t.bulkTotalLabel}</div>
                    <div className="text-sm font-bold text-[#0F3D28] font-mono-num mt-0.5">
                      ₹{bulkSaleTotal.toLocaleString('en-IN')}
                    </div>
                  </div>
                  <div className="bg-white rounded-lg px-2.5 py-2 border border-[#0F3D28]/10">
                    <div className="text-[10px] font-semibold text-[#526058] uppercase">{t.bulkMrpLabel}</div>
                    <div className="text-sm font-semibold text-[#526058] line-through font-mono-num mt-0.5">
                      ₹{bulkMrpTotal.toLocaleString('en-IN')}
                    </div>
                  </div>
                  <div className="bg-[#0F3D28] rounded-lg px-2.5 py-2 text-white">
                    <div className="text-[10px] font-semibold text-[#E6A817] uppercase">{t.bulkSavingsLabel}</div>
                    <div className="text-sm font-bold text-white font-mono-num mt-0.5">
                      -₹{bulkSavings.toLocaleString('en-IN')}
                    </div>
                  </div>
                </div>

                {bulkSaleTotal >= 499 && (
                  <p className="text-[11px] font-semibold text-[#0F3D28] flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#0F3D28] shrink-0" />
                    <span>{t.bulkFreeDelivery}</span>
                  </p>
                )}
              </div>

              {/* Expandable Specifications Header */}
              <div className="space-y-2.5 pt-2 border-t border-[#0F3D28]/10">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wider text-[#0F3D28]">
                    Product Details &amp; Safety Info
                  </span>
                  <button
                    type="button"
                    onClick={toggleExpandAll}
                    className="text-xs font-semibold text-[#0F3D28] underline cursor-pointer"
                  >
                    {allExpanded ? 'Collapse All' : 'Expand All'}
                  </button>
                </div>

                {/* 1. Ingredients / Composition */}
                <div className="rounded-xl border border-[#0F3D28]/15 overflow-hidden bg-[#F8F7F2]">
                  <button
                    type="button"
                    aria-expanded={openSections.ingredients}
                    onClick={() => toggleSection('ingredients')}
                    className="w-full px-3.5 py-2.5 flex items-center justify-between text-left text-xs font-bold text-[#14221B] hover:bg-[#EFECE2] transition-colors cursor-pointer"
                  >
                    <span className="inline-flex items-center gap-2">
                      <ListChecks className="w-4 h-4 text-[#0F3D28]" />
                      <span>{spec.isNonFood ? t.quickViewComposition : t.quickViewIngredients}</span>
                    </span>
                    <ChevronDown
                      className={`w-4 h-4 text-[#526058] transition-transform ${
                        openSections.ingredients ? 'rotate-180' : ''
                      }`}
                    />
                  </button>
                  {openSections.ingredients && (
                    <div className="px-3.5 pb-3 pt-1 text-xs text-[#14221B]/90 leading-relaxed bg-white border-t border-[#0F3D28]/10">
                      {spec.ingredients}
                    </div>
                  )}
                </div>

                {/* 2. Shelf Life & Storage */}
                <div className="rounded-xl border border-[#0F3D28]/15 overflow-hidden bg-[#F8F7F2]">
                  <button
                    type="button"
                    aria-expanded={openSections.shelfLife}
                    onClick={() => toggleSection('shelfLife')}
                    className="w-full px-3.5 py-2.5 flex items-center justify-between text-left text-xs font-bold text-[#14221B] hover:bg-[#EFECE2] transition-colors cursor-pointer"
                  >
                    <span className="inline-flex items-center gap-2">
                      <Clock className="w-4 h-4 text-[#0F3D28]" />
                      <span>{t.quickViewShelfLife}</span>
                    </span>
                    <ChevronDown
                      className={`w-4 h-4 text-[#526058] transition-transform ${
                        openSections.shelfLife ? 'rotate-180' : ''
                      }`}
                    />
                  </button>
                  {openSections.shelfLife && (
                    <div className="px-3.5 pb-3 pt-1.5 text-xs text-[#14221B]/90 leading-relaxed bg-white border-t border-[#0F3D28]/10 space-y-1">
                      <p>
                        <strong className="text-[#0F3D28]">Shelf Life:</strong> {spec.shelfLife}
                      </p>
                      <p>
                        <strong className="text-[#0F3D28]">Storage:</strong> {spec.storage}
                      </p>
                    </div>
                  )}
                </div>

                {/* 3. Allergen Warnings */}
                <div className="rounded-xl border border-[#E6A817]/50 overflow-hidden bg-[#FFFBEB]">
                  <button
                    type="button"
                    aria-expanded={openSections.allergens}
                    onClick={() => toggleSection('allergens')}
                    className="w-full px-3.5 py-2.5 flex items-center justify-between text-left text-xs font-bold text-[#14221B] hover:bg-[#FEF3C7] transition-colors cursor-pointer"
                  >
                    <span className="inline-flex items-center gap-2">
                      <AlertTriangle className="w-4 h-4 text-[#B45309]" />
                      <span>{t.quickViewAllergens}</span>
                    </span>
                    <ChevronDown
                      className={`w-4 h-4 text-[#92400E] transition-transform ${
                        openSections.allergens ? 'rotate-180' : ''
                      }`}
                    />
                  </button>
                  {openSections.allergens && (
                    <div className="px-3.5 pb-3 pt-1.5 text-xs text-[#78350F] font-medium leading-relaxed bg-white border-t border-[#E6A817]/30">
                      {spec.allergens}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 pt-2 border-t border-[#0F3D28]/10">
              {Number(product.stock) <= 0 ? (
                <button
                  type="button"
                  disabled
                  className="w-full py-3 rounded-xl bg-neutral-200 text-neutral-500 font-semibold text-sm cursor-not-allowed"
                >
                  Out of Stock
                </button>
              ) : qtyInCart > 0 ? (
                <div className="flex items-center justify-between w-full gap-3">
                  <div className="inline-flex items-center rounded-xl bg-[#0F3D28] text-white overflow-hidden">
                    <button
                      type="button"
                      onClick={() => onUpdateQty(product.id, qtyInCart - 1)}
                      aria-label={`Decrease ${product.name} quantity`}
                      className="w-11 h-11 flex items-center justify-center hover:bg-[#0A2A1B] cursor-pointer"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <span className="px-4 text-sm font-bold tabular-nums">{qtyInCart}</span>
                    <button
                      type="button"
                      onClick={() => onUpdateQty(product.id, qtyInCart + 1)}
                      aria-label={`Increase ${product.name} quantity`}
                      className="w-11 h-11 flex items-center justify-center hover:bg-[#0A2A1B] cursor-pointer"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>
                  <button
                    type="button"
                    onClick={onClose}
                    className="flex-1 py-3 px-4 rounded-xl bg-[#E6A817] text-[#14221B] font-bold text-sm hover:bg-[#f0b72b] transition-colors cursor-pointer"
                  >
                    Done ({qtyInCart} in Basket)
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => onAdd(product)}
                  className="w-full py-3 rounded-xl bg-[#0F3D28] text-white font-semibold text-sm hover:bg-[#0A2A1B] transition-colors flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Plus className="w-4 h-4 text-[#E6A817]" />
                  <span>{t.quickViewAddBtn} — ₹{sale}</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
