import React, { useEffect, useState } from 'react';
import { Plus, Minus, Snowflake, Heart } from 'lucide-react';
import { Product } from '../types';
import { buildBrandPackageSvg, resolveProductImage } from '../utils/brandPhotos';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface ProductCardProps {
  product: Product;
  index: number;
  qtyInCart: number;
  lang?: StoreLang;
  isFavorite?: boolean;
  onToggleFavorite?: (productId: string) => void;
  onAdd: (product: Product) => void;
  onUpdateQty: (productId: string, qty: number) => void;
  onQuickView: (product: Product) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  index,
  qtyInCart,
  lang = 'en',
  isFavorite = false,
  onToggleFavorite,
  onAdd,
  onUpdateQty,
  onQuickView,
}) => {
  const t = getStoreTranslations(lang);
  const resolvedUrl = resolveProductImage(product);
  const [imgSrc, setImgSrc] = useState<string>(resolvedUrl);
  const [loaded, setLoaded] = useState<boolean>(false);

  useEffect(() => {
    if (resolvedUrl !== imgSrc) {
      setImgSrc(resolvedUrl);
      setLoaded(false);
    }
  }, [resolvedUrl, imgSrc]);

  const mrp = Math.round(Number(product.mrp) || 0);
  const sale = Math.round(Number(product.sale || product.price) || 0);
  const discountPct = mrp > sale ? Math.round(((mrp - sale) / mrp) * 100) : 0;
  const outOfStock = Number(product.stock) <= 0;
  const isFrozen =
    (product.department || '').toLowerCase().includes('frozen') ||
    (product.category || '').toLowerCase().includes('frozen');

  const handleImageError = () => {
    const svgFallback = buildBrandPackageSvg(product);
    if (imgSrc !== svgFallback) {
      setImgSrc(svgFallback);
    }
    setLoaded(true);
  };

  return (
    <article className="group bg-white rounded-xl border border-[#0F3D28]/10 hover:border-[#0F3D28]/30 hover:shadow-md transition-all duration-150 flex flex-col overflow-hidden">
      <div className="relative aspect-square w-full bg-white border-b border-[#0F3D28]/8 overflow-hidden">
        <button
          type="button"
          onClick={() => onQuickView(product)}
          className="w-full h-full text-left cursor-pointer block"
          aria-label={`Quick view ${product.name}`}
        >
          {!loaded && <div className="absolute inset-0 skeleton-shimmer" aria-hidden="true" />}
          <img
            src={imgSrc}
            alt={`${product.brand} ${product.name}`}
            loading={index < 6 ? 'eager' : 'lazy'}
            decoding="async"
            fetchPriority={index < 6 ? 'high' : 'auto'}
            referrerPolicy="no-referrer"
            onLoad={() => setLoaded(true)}
            onError={handleImageError}
            className={`w-full h-full object-contain p-3.5 transition-all duration-200 group-hover:scale-[1.03] ${
              loaded ? 'opacity-100' : 'opacity-0'
            }`}
          />

          {discountPct > 0 && (
            <span className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded bg-[#E6A817] text-[#14221B] text-[11px] font-bold tracking-wide tabular-nums pointer-events-none">
              -{discountPct}% OFF
            </span>
          )}

          {isFrozen && (
            <span className="absolute top-2.5 right-2.5 inline-flex items-center gap-1 px-2 py-0.5 rounded bg-[#E0F2FE] text-[#0369A1] text-[11px] font-semibold pointer-events-none">
              <Snowflake className="w-3 h-3" />
              -18°C
            </span>
          )}

          <span className="absolute bottom-2.5 right-2.5 px-2.5 py-1 rounded-md bg-[#0F3D28]/90 text-white text-[11px] font-semibold opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
            {t.quickViewLabel}
          </span>
        </button>

        {onToggleFavorite && (
          <button
            type="button"
            onClick={() => onToggleFavorite(product.id)}
            aria-pressed={isFavorite}
            aria-label={isFavorite ? `Remove ${product.name} from Favorites` : `Save ${product.name} to Favorites`}
            className={`absolute bottom-2.5 left-2.5 w-8 h-8 rounded-full flex items-center justify-center border shadow-xs transition-all cursor-pointer z-10 ${
              isFavorite
                ? 'bg-[#0F3D28] border-[#0F3D28] text-[#E6A817]'
                : 'bg-white/95 border-[#0F3D28]/20 text-[#526058] hover:text-[#0F3D28] hover:border-[#0F3D28]/45'
            }`}
          >
            <Heart className={`w-4 h-4 ${isFavorite ? 'fill-[#E6A817]' : ''}`} />
          </button>
        )}
      </div>

      <div className="p-3.5 flex-1 flex flex-col justify-between gap-3">
        <div className="space-y-1">
          <div className="flex items-center justify-between gap-2 text-[11px] uppercase tracking-wider text-[#526058] font-semibold">
            <span className="truncate">{product.brand || 'Nav Store'}</span>
            <span className="font-mono-num text-[10px] shrink-0">{product.id}</span>
          </div>

          <button
            type="button"
            onClick={() => onQuickView(product)}
            className="text-left text-sm font-semibold text-[#14221B] leading-snug line-clamp-2 hover:text-[#0F3D28] transition-colors cursor-pointer"
          >
            {product.name}
          </button>

          <p className="text-xs text-[#526058] truncate">{product.category}</p>
        </div>

        <div className="pt-2 border-t border-[#0F3D28]/8 flex items-end justify-between gap-2">
          <div>
            <div className="flex items-baseline gap-1.5 tabular-nums">
              <span className="text-base font-bold text-[#0F3D28]">₹{sale}</span>
              {mrp > sale && (
                <span className="text-xs text-[#526058] line-through">₹{mrp}</span>
              )}
            </div>
            {mrp > sale && (
              <p className="text-[11px] font-medium text-[#0F3D28]/80 tabular-nums">
                {t.savePrefix} ₹{mrp - sale}
              </p>
            )}
          </div>

          {outOfStock ? (
            <span className="px-2.5 py-1.5 rounded-lg bg-neutral-100 text-neutral-500 text-xs font-semibold whitespace-nowrap">
              {t.outOfStock}
            </span>
          ) : qtyInCart > 0 ? (
            <div className="inline-flex items-center rounded-lg bg-[#0F3D28] text-white overflow-hidden shrink-0">
              <button
                type="button"
                onClick={() => onUpdateQty(product.id, qtyInCart - 1)}
                aria-label={`Decrease ${product.name} quantity`}
                className="w-8 h-8 flex items-center justify-center hover:bg-[#0A2A1B] transition-colors cursor-pointer"
              >
                <Minus className="w-3.5 h-3.5" />
              </button>
              <span className="px-2 text-xs font-bold tabular-nums min-w-[24px] text-center">
                {qtyInCart}
              </span>
              <button
                type="button"
                onClick={() => onUpdateQty(product.id, qtyInCart + 1)}
                aria-label={`Increase ${product.name} quantity`}
                className="w-8 h-8 flex items-center justify-center hover:bg-[#0A2A1B] transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => onAdd(product)}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-[#0F3D28] text-white hover:bg-[#0A2A1B] text-xs font-semibold transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5 text-[#E6A817]" />
              <span>{t.addBtn}</span>
            </button>
          )}
        </div>
      </div>
    </article>
  );
};
