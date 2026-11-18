import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Search,
  MapPin,
  ShoppingBasket,
  LogOut,
  ArrowUpDown,
  Phone,
  Mail,
  ShieldCheck,
  SlidersHorizontal,
  Globe,
  Instagram,
  Youtube,
  Package,
  Snowflake,
  LayoutGrid,
  X,
  Heart,
  PackageCheck,
  ScanBarcode,
  Zap,
  ShoppingBag,
  Cpu,
  Shirt,
  User,
  ArrowRight,
} from 'lucide-react';
import {
  BrandingSettings,
  CartItem,
  CustomerSession,
  FulfillmentMode,
  HeroBanner,
  PaymentSettings,
  Product,
  ServiceVertical,
} from '../types';
import {
  DepartmentFilter,
  GROCERY_AISLES,
  isApparelProduct,
  isElectronicsOrHomeProduct,
  isFrozenProduct,
  isGeneralRetailProduct,
  isGroceryCoreProduct,
  matchesDepartmentCategory,
  matchesGroceryAisle,
  matchesServiceVertical,
} from '../utils/aisles';
import { getNextFallbackImage, resolveProductImage } from '../utils/brandPhotos';
import { StoreLogo } from './StoreLogo';
import { HeroCarousel } from './HeroCarousel';
import { ProductCard } from './ProductCard';
import { QuickViewModal } from './QuickViewModal';
import { CartCheckoutDrawer } from './CartCheckoutDrawer';
import { GoogleSignInButton } from './GoogleSignInButton';
import { StoreAICopilot } from './StoreAICopilot';
import { DigitalVisitingCard } from './DigitalVisitingCard';
import { OrderTrackerModal } from './OrderTrackerModal';
import { BarcodeScannerModal } from './BarcodeScannerModal';
import { LanguageSwitcher } from './LanguageSwitcher';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface StorefrontViewProps {
  products: Product[];
  loading: boolean;
  branding: BrandingSettings;
  paymentSettings: PaymentSettings;
  lang: StoreLang;
  onChangeLang: (lang: StoreLang) => void;
  favoriteIds: string[];
  onToggleFavorite: (productId: string) => void;
  cart: CartItem[];
  onAddToCart: (product: Product, qty?: number) => void;
  onUpdateCartQty: (productId: string, qty: number) => void;
  onRemoveCartItem: (productId: string) => void;
  onClearCart: () => void;
  customerSession: CustomerSession | null;
  onUpdateSession: (session: CustomerSession | null) => void;
  onOpenAccount: () => void;
  authLoading: boolean;
  authError: string;
  onSignInWithGoogle: () => Promise<void>;
  onSignOut: () => Promise<void>;
  onClearAuthError: () => void;
}

const BATCH_SIZE = 24;

const SHIPPING_ZONES = [
  'Main Bazaar, Sandhaur 148020 (15-Min Express / 2-Hr)',
  'Malerkotla / Ludhiana Hub (Same-Day)',
  'All-India Parcel Courier (3–5 Days)',
  'International NRI Gift Dispatch',
];

export const StorefrontView: React.FC<StorefrontViewProps> = ({
  products,
  loading,
  branding,
  paymentSettings,
  lang,
  onChangeLang,
  favoriteIds,
  onToggleFavorite,
  cart,
  onAddToCart,
  onUpdateCartQty,
  onRemoveCartItem,
  onClearCart,
  customerSession,
  onUpdateSession,
  onOpenAccount,
  authLoading,
  authError,
  onSignInWithGoogle,
  onSignOut,
  onClearAuthError,
}) => {
  const [activeVertical, setActiveVertical] = useState<ServiceVertical>('store');
  const [shippingZone, setShippingZone] = useState<string>(SHIPPING_ZONES[0]);
  const [departmentFilter, setDepartmentFilter] = useState<DepartmentFilter>('all');
  const [selectedAisle, setSelectedAisle] = useState<string>('all');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchOpen, setSearchOpen] = useState<boolean>(false);
  const [sortBy, setSortBy] = useState<'popular' | 'price_asc' | 'price_desc' | 'savings'>('popular');
  const [visibleCount, setVisibleCount] = useState<number>(BATCH_SIZE);

  const [fulfillment, setFulfillment] = useState<FulfillmentMode>('delivery');
  const [cartOpen, setCartOpen] = useState<boolean>(false);
  const [trackerOpen, setTrackerOpen] = useState<boolean>(false);
  const [scannerOpen, setScannerOpen] = useState<boolean>(false);
  const [trackerOrderId, setTrackerOrderId] = useState<string>('');
  const [quickViewProduct, setQuickViewProduct] = useState<Product | null>(null);
  const [showFavoritesOnly, setShowFavoritesOnly] = useState<boolean>(false);

  const t = useMemo(() => getStoreTranslations(lang), [lang]);

  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const catalogRef = useRef<HTMLElement | null>(null);
  const deepLinkedSkuRef = useRef<boolean>(false);

  useEffect(() => {
    if (deepLinkedSkuRef.current || products.length === 0) return;
    try {
      const params = new URLSearchParams(window.location.search);
      const rawSku = (params.get('sku') || params.get('product') || '').trim().toUpperCase();
      if (!rawSku) return;
      const match = products.find((p) => p.id.toUpperCase() === rawSku);
      if (match) {
        deepLinkedSkuRef.current = true;
        setQuickViewProduct(match);
      }
    } catch {
      // ignore URL parsing error
    }
  }, [products]);

  const favoriteSet = useMemo(() => new Set(favoriteIds), [favoriteIds]);

  const verticalScopedProducts = useMemo(
    () => products.filter((p) => matchesServiceVertical(p, activeVertical)),
    [products, activeVertical]
  );

  const departmentCounts = useMemo(() => {
    let packaged = 0;
    let frozen = 0;
    let electronics = 0;
    let apparel = 0;
    let retail = 0;
    let favorites = 0;
    for (const p of verticalScopedProducts) {
      if (isElectronicsOrHomeProduct(p)) electronics++;
      else if (isApparelProduct(p)) apparel++;
      else if (isGeneralRetailProduct(p)) retail++;
      else if (isFrozenProduct(p)) frozen++;
      else if (isGroceryCoreProduct(p)) packaged++;

      if (favoriteSet.has(p.id)) favorites++;
    }
    return {
      all: verticalScopedProducts.length,
      packaged,
      frozen,
      electronics,
      apparel,
      retail,
      favorites,
    };
  }, [verticalScopedProducts, favoriteSet]);

  const visibleAisles = useMemo(() => {
    const allowedIds =
      activeVertical === 'supermall'
        ? new Set(['all', 'deals', 'electronics', 'apparel', 'retail'])
        : new Set(['all', 'deals', 'frozen', 'atta', 'ghee', 'spices', 'beverages', 'household']);
    return GROCERY_AISLES.filter((aisle) => allowedIds.has(aisle.id));
  }, [activeVertical]);

  const productsMatchingNonCategoryFilters = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return verticalScopedProducts.filter((p) => {
      if (showFavoritesOnly && !favoriteSet.has(p.id)) return false;
      if (!matchesDepartmentCategory(p, departmentFilter)) return false;
      if (!matchesGroceryAisle(p, selectedAisle)) return false;
      if (!q) return true;
      const hay = `${p.id} ${p.brand} ${p.name} ${p.category} ${p.department} ${p.barcode || ''}`.toLowerCase();
      return q.split(/\s+/).every((token) => hay.includes(token));
    });
  }, [verticalScopedProducts, showFavoritesOnly, favoriteSet, departmentFilter, selectedAisle, searchQuery]);

  const categoryOptions = useMemo(() => {
    const counts = new Map<string, number>();
    for (const p of productsMatchingNonCategoryFilters) {
      const cat = String(p.category || '').trim();
      if (!cat) continue;
      counts.set(cat, (counts.get(cat) || 0) + 1);
    }

    const entries = Array.from(counts.entries()).sort((a, b) => {
      if (b[1] !== a[1]) return b[1] - a[1];
      return a[0].localeCompare(b[0]);
    });

    if (selectedCategory !== 'all' && !counts.has(selectedCategory)) {
      entries.push([selectedCategory, 0]);
    }

    return [
      { id: 'all', count: productsMatchingNonCategoryFilters.length },
      ...entries.map(([id, count]) => ({ id, count })),
    ];
  }, [productsMatchingNonCategoryFilters, selectedCategory]);

  const filteredProducts = useMemo(() => {
    const list =
      selectedCategory === 'all'
        ? productsMatchingNonCategoryFilters
        : productsMatchingNonCategoryFilters.filter(
            (p) => String(p.category || '').trim() === selectedCategory
          );

    if (sortBy === 'price_asc') return [...list].sort((a, b) => a.sale - b.sale);
    if (sortBy === 'price_desc') return [...list].sort((a, b) => b.sale - a.sale);
    if (sortBy === 'savings') return [...list].sort((a, b) => b.mrp - b.sale - (a.mrp - a.sale));
    return list;
  }, [productsMatchingNonCategoryFilters, selectedCategory, sortBy]);

  const predictiveSuggestions = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (q.length < 2) return [];
    return products
      .filter((p) => {
        const hay = `${p.id} ${p.brand} ${p.name} ${p.category} ${p.barcode || ''}`.toLowerCase();
        return q.split(/\s+/).every((tk) => hay.includes(tk));
      })
      .slice(0, 6);
  }, [products, searchQuery]);

  useEffect(() => {
    setVisibleCount(BATCH_SIZE);
  }, [activeVertical, showFavoritesOnly, departmentFilter, selectedAisle, selectedCategory, searchQuery, sortBy]);

  const handleSelectVertical = (vertical: ServiceVertical) => {
    setActiveVertical(vertical);
    setSelectedCategory('all');
    setSelectedAisle('all');
    setDepartmentFilter('all');
  };

  const handleSelectDepartment = (dept: DepartmentFilter) => {
    setDepartmentFilter(dept);
    setSelectedCategory('all');
    setSelectedAisle('all');
  };

  const handleSelectAisle = (aisleId: string) => {
    setSelectedAisle(aisleId);
    setSelectedCategory('all');
    if (aisleId === 'electronics' || aisleId === 'apparel' || aisleId === 'retail') {
      setActiveVertical('supermall');
    } else if (
      activeVertical === 'supermall' &&
      ['frozen', 'atta', 'ghee', 'spices', 'beverages', 'household'].includes(aisleId)
    ) {
      setActiveVertical('store');
    }
    setDepartmentFilter('all');
  };

  useEffect(() => {
    const node = sentinelRef.current;
    if (!node) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          setVisibleCount((prev) => Math.min(prev + BATCH_SIZE, filteredProducts.length));
        }
      },
      { rootMargin: '260px' }
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [filteredProducts.length]);

  const visibleProducts = filteredProducts.slice(0, visibleCount);
  const cartQtyMap = useMemo(() => {
    const m = new Map<string, number>();
    cart.forEach((item) => m.set(item.product.id, item.quantity));
    return m;
  }, [cart]);

  const totalCartItems = cart.reduce((acc, item) => acc + item.quantity, 0);
  const totalCartValue = cart.reduce((acc, item) => acc + item.product.sale * item.quantity, 0);

  const handleBannerAction = (banner: HeroBanner) => {
    if (banner.targetVertical) {
      setActiveVertical(banner.targetVertical);
    }
    handleSelectAisle(banner.targetAisle || 'all');
    catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const verticalTabs: Array<{
    id: ServiceVertical;
    label: string;
    badge: string;
    icon: React.ReactNode;
  }> = [
    {
      id: 'store',
      label: t.verticals.store.label,
      badge: t.verticals.store.badge,
      icon: <ShoppingBag className="w-3.5 h-3.5 shrink-0" />,
    },
    {
      id: 'supermall',
      label: t.verticals.supermall.label,
      badge: t.verticals.supermall.badge,
      icon: <Cpu className="w-3.5 h-3.5 shrink-0" />,
    },
    {
      id: 'express',
      label: t.verticals.express.label,
      badge: t.verticals.express.badge,
      icon: <Zap className="w-3.5 h-3.5 shrink-0" />,
    },
    {
      id: 'essentials',
      label: t.verticals.essentials.label,
      badge: t.verticals.essentials.badge,
      icon: <Snowflake className="w-3.5 h-3.5 shrink-0" />,
    },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-[#F8F7F2] text-[#14221B]">
      {/* Announcement Strip */}
      <div className="bg-[#0A2A1B] text-[#F8F7F2] text-[11px] sm:text-xs py-1.5 px-4 flex flex-wrap items-center justify-center gap-x-3 gap-y-0.5 font-medium">
        <span>{lang === 'en' ? branding.announcementText : t.announcementFallback}</span>
        <span className="hidden md:inline text-[#E6A817]">
          · {t.helplineLabel} +91 8847370905 / +91 9915752905
        </span>
      </div>

      {/* Main Sticky Header */}
      <header className="sticky top-0 z-30 bg-[#0F3D28] text-white border-b border-[#E6A817]/25 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-3">
          {/* Logo & Active Location / Zone Selector */}
          <div className="flex items-center gap-3 shrink-0">
            <a href="/" className="flex items-center gap-2.5 text-left shrink-0">
              <StoreLogo logoUrl={branding.logoUrl} darkSurface sizeClass="w-10 h-10 sm:w-11 sm:h-11" />
              <div>
                <div className="font-display font-bold text-base sm:text-lg leading-tight text-white whitespace-nowrap">
                  {t.storeName}
                </div>
                <div className="flex items-center gap-1 text-[11px] text-[#E6A817] font-medium whitespace-nowrap">
                  <MapPin className="w-3 h-3 shrink-0" />
                  <span>{t.headerLocation}</span>
                </div>
              </div>
            </a>

            <select
              aria-label="Select Shipping Zone"
              value={shippingZone}
              onChange={(e) => setShippingZone(e.target.value)}
              className="hidden xl:block bg-[#0A2A1B] text-white/95 border border-white/15 rounded-lg px-2.5 py-1.5 text-[11px] font-semibold focus:outline-none cursor-pointer"
            >
              {SHIPPING_ZONES.map((z) => (
                <option key={z} value={z}>
                  {z}
                </option>
              ))}
            </select>
          </div>

          {/* Global Predictive Search Bar + Barcode Scanner Trigger */}
          <div className="relative flex-1 min-w-[220px] max-w-xl order-last lg:order-none w-full lg:w-auto">
            <div className="flex items-center bg-white rounded-xl overflow-hidden text-[#14221B] border border-white/20">
              <Search className="w-4 h-4 text-[#526058] ml-3 shrink-0" />
              <input
                type="search"
                value={searchQuery}
                onFocus={() => setSearchOpen(true)}
                onBlur={() => setTimeout(() => setSearchOpen(false), 180)}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setSearchOpen(true);
                }}
                placeholder={t.searchPlaceholder}
                className="w-full px-3 py-2 text-xs sm:text-sm bg-transparent focus:outline-none"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="px-2 text-xs font-semibold text-[#526058] hover:text-[#14221B] cursor-pointer"
                >
                  {t.clearLabel}
                </button>
              )}
              <button
                type="button"
                onClick={() => setScannerOpen(true)}
                title="Scan Pack Barcode or SKU"
                className="px-3 py-2 bg-[#F8F7F2] hover:bg-[#EFECE2] border-l border-[#0F3D28]/15 text-[#0F3D28] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
              >
                <ScanBarcode className="w-4 h-4" />
                <span className="hidden sm:inline">{t.scanBarcodeLabel}</span>
              </button>
            </div>

            {searchOpen && predictiveSuggestions.length > 0 && (
              <div className="absolute left-0 right-0 top-full mt-1.5 bg-white text-[#14221B] rounded-xl shadow-2xl border border-[#0F3D28]/15 overflow-hidden z-40">
                {predictiveSuggestions.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onMouseDown={() => {
                      setQuickViewProduct(item);
                      setSearchOpen(false);
                    }}
                    className="w-full px-3.5 py-2.5 flex items-center justify-between gap-3 hover:bg-[#F8F7F2] text-left border-b border-[#0F3D28]/8 last:border-b-0 cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <img
                        src={resolveProductImage(item)}
                        alt={item.name}
                        className="w-9 h-9 rounded-lg object-contain bg-[#F8F7F2] shrink-0"
                        referrerPolicy="no-referrer"
                        onError={(e) => {
                          const el = e.currentTarget;
                          if (el.src.startsWith('data:image/svg+xml')) {
                            el.onerror = null;
                            return;
                          }
                          const nextSrc = getNextFallbackImage(item, el.getAttribute('src') || el.src);
                          if (nextSrc.startsWith('data:image/svg+xml')) {
                            el.onerror = null;
                          }
                          el.src = nextSrc;
                        }}
                      />
                      <div className="min-w-0">
                        <p className="text-xs font-semibold truncate">{item.name}</p>
                        <p className="text-[11px] text-[#526058] truncate">
                          {item.brand} · {item.category} · {item.id}
                        </p>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-[#0F3D28] tabular-nums shrink-0">
                      ₹{item.sale}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Right Action Controls (Strict Single-Line Integrity) */}
          <div className="flex items-center gap-2 shrink-0">
            <LanguageSwitcher lang={lang} onChange={onChangeLang} />

            <button
              type="button"
              onClick={() => {
                setShowFavoritesOnly((prev) => !prev);
                catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
              }}
              aria-pressed={showFavoritesOnly}
              className={`px-2.5 sm:px-3 py-2 rounded-xl text-xs font-semibold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer transition-colors ${
                showFavoritesOnly
                  ? 'bg-[#E6A817] text-[#14221B]'
                  : 'bg-white/10 hover:bg-white/20 text-white'
              }`}
            >
              <Heart
                className={`w-3.5 h-3.5 ${
                  showFavoritesOnly ? 'fill-[#14221B] text-[#14221B]' : 'text-[#E6A817]'
                }`}
              />
              <span className="hidden md:inline">{t.favoritesLabel}</span>
              <span
                className={`px-1.5 py-0.5 rounded-md text-[11px] font-mono-num ${
                  showFavoritesOnly ? 'bg-[#0F3D28] text-white' : 'bg-[#0A2A1B] text-[#E6A817]'
                }`}
              >
                {departmentCounts.favorites}
              </span>
            </button>

            <button
              type="button"
              onClick={() => {
                setTrackerOrderId('');
                setTrackerOpen(true);
              }}
              className="px-2.5 sm:px-3 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
            >
              <PackageCheck className="w-3.5 h-3.5 text-[#E6A817]" />
              <span className="hidden md:inline">{t.trackOrderLabel}</span>
            </button>

            <button
              type="button"
              onClick={onOpenAccount}
              title={customerSession ? `${customerSession.name} (${customerSession.email})` : t.accountLabel}
              className="px-2.5 sm:px-3 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
            >
              {customerSession?.photoURL ? (
                <img
                  src={customerSession.photoURL}
                  alt={customerSession.name}
                  referrerPolicy="no-referrer"
                  className="w-4 h-4 rounded-full object-cover border border-[#E6A817]"
                />
              ) : (
                <User className="w-3.5 h-3.5 text-[#E6A817]" />
              )}
              <span className="hidden sm:inline">
                {customerSession ? customerSession.name.split(' ')[0] : t.accountLabel}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setCartOpen(true)}
              className="relative px-3.5 py-2 rounded-xl bg-[#E6A817] text-[#14221B] hover:bg-[#f0b72b] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
            >
              <ShoppingBasket className="w-4 h-4" />
              <span>{t.basketLabel}</span>
              <span className="px-1.5 py-0.5 rounded-md bg-[#0F3D28] text-white text-[11px] tabular-nums">
                {totalCartItems}
              </span>
              {totalCartValue > 0 && (
                <span className="hidden sm:inline font-mono-num text-[11px]">₹{totalCartValue}</span>
              )}
            </button>
          </div>
        </div>

        {/* Multi-Vertical Service Switcher Bar */}
        <div className="bg-[#0A2A1B] border-t border-white/10">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 py-2 flex items-center justify-between gap-3 overflow-x-auto no-scrollbar">
            <div className="flex items-center gap-2 shrink-0">
              {verticalTabs.map((vTab) => {
                const active = activeVertical === vTab.id;
                return (
                  <button
                    key={vTab.id}
                    type="button"
                    onClick={() => handleSelectVertical(vTab.id)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold inline-flex items-center gap-2 whitespace-nowrap shrink-0 cursor-pointer transition-colors ${
                      active
                        ? 'bg-[#E6A817] text-[#14221B]'
                        : 'text-white/85 hover:bg-white/10 hover:text-white'
                    }`}
                  >
                    {vTab.icon}
                    <span>{vTab.label}</span>
                    <span
                      className={`text-[10px] font-mono-num px-1.5 py-0.5 rounded ${
                        active ? 'bg-[#0F3D28] text-white' : 'bg-white/10 text-[#E6A817]'
                      }`}
                    >
                      {vTab.badge}
                    </span>
                  </button>
                );
              })}
            </div>

            {customerSession ? (
              <div className="hidden md:flex items-center gap-2.5 shrink-0">
                <button
                  type="button"
                  onClick={onOpenAccount}
                  className="inline-flex items-center gap-2 text-left cursor-pointer group"
                >
                  {customerSession.photoURL ? (
                    <img
                      src={customerSession.photoURL}
                      alt={customerSession.name}
                      referrerPolicy="no-referrer"
                      className="w-6 h-6 rounded-full border border-[#E6A817] object-cover"
                    />
                  ) : (
                    <span className="w-6 h-6 rounded-full bg-[#E6A817] text-[#14221B] text-[10px] font-bold inline-flex items-center justify-center">
                      {customerSession.name.slice(0, 1).toUpperCase()}
                    </span>
                  )}
                  <span className="text-[11px] leading-tight">
                    <span className="block font-bold text-white group-hover:text-[#E6A817]">
                      {customerSession.name}
                    </span>
                    <span className="block text-[10px] text-white/70">{customerSession.email}</span>
                  </span>
                </button>
                <button
                  type="button"
                  onClick={() => void onSignOut()}
                  className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-[11px] font-semibold text-[#E6A817] inline-flex items-center gap-1 whitespace-nowrap cursor-pointer"
                >
                  <LogOut className="w-3 h-3" />
                  <span>{t.signOutLabel}</span>
                </button>
              </div>
            ) : (
              <GoogleSignInButton
                onClick={() => void onSignInWithGoogle()}
                loading={authLoading}
                label={t.googleSignInLabel}
                variant="compact"
                className="hidden md:inline-flex shrink-0"
              />
            )}
          </div>
        </div>

        {authError && (
          <div className="bg-amber-50 text-[#78350F] border-t border-amber-200 px-4 py-2 text-xs flex items-center justify-between gap-3">
            <span className="font-semibold">{authError}</span>
            <button
              type="button"
              onClick={onClearAuthError}
              aria-label="Dismiss auth notice"
              className="p-0.5 hover:bg-amber-200/60 rounded cursor-pointer shrink-0"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-8">
        {/* Flash Deal Campaign Hero Carousel */}
        <HeroCarousel
          banners={branding.banners}
          lang={lang}
          onAction={handleBannerAction}
          onShopAll={() => {
            setActiveVertical('store');
            setDepartmentFilter('all');
            setSelectedAisle('all');
            setSelectedCategory('all');
            catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }}
        />

        {/* Category Spotlight Grid (Wholesale Groceries, Electronics & Appliances, Fashion & Apparel) */}
        <section aria-label="Category Spotlights" className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <button
            type="button"
            onClick={() => {
              handleSelectVertical('store');
              handleSelectAisle('deals');
              catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }}
            className="group relative rounded-2xl overflow-hidden bg-[#0F3D28] text-white text-left border border-[#0F3D28]/20 min-h-[148px] p-5 flex flex-col justify-between cursor-pointer hover:shadow-md transition-shadow"
          >
            <img
              src="/images/store/aisle_beverages.jpg"
              alt="Wholesale Grocery Deals"
              className="absolute inset-0 w-full h-full object-cover opacity-25 group-hover:scale-105 transition-transform duration-300"
            />
            <div className="relative z-10 space-y-1">
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#E6A817]">
                308 Verified SKUs · 12% Off
              </span>
              <h3 className="font-display text-lg font-bold">Wholesale Grocery Deals</h3>
              <p className="text-xs text-white/85">
                Atta, Basmati Rice, Pure Desi Ghee, Spices &amp; -18°C Frozen Packs
              </p>
            </div>
            <span className="relative z-10 inline-flex items-center gap-1 text-xs font-bold text-[#E6A817] mt-3">
              <span>Explore Groceries</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </button>

          <button
            type="button"
            onClick={() => {
              setActiveVertical('supermall');
              setDepartmentFilter('electronics');
              setSelectedAisle('all');
              setSelectedCategory('all');
              catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }}
            className="group relative rounded-2xl overflow-hidden bg-[#14221B] text-white text-left border border-[#0F3D28]/20 min-h-[148px] p-5 flex flex-col justify-between cursor-pointer hover:shadow-md transition-shadow"
          >
            <img
              src="/images/store/aisle_snacks.jpg"
              alt="Electronics and Home Appliances"
              className="absolute inset-0 w-full h-full object-cover opacity-20 group-hover:scale-105 transition-transform duration-300"
            />
            <div className="relative z-10 space-y-1">
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#E6A817]">
                Nav Kart Supermall · Warranty Sealed
              </span>
              <h3 className="font-display text-lg font-bold">Electronics &amp; Appliances</h3>
              <p className="text-xs text-white/85">
                boAt Audio, Mi Power Banks, Philips LEDs, Kettles &amp; Home Storage
              </p>
            </div>
            <span className="relative z-10 inline-flex items-center gap-1 text-xs font-bold text-[#E6A817] mt-3">
              <span>Shop Supermall Tech</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </button>

          <button
            type="button"
            onClick={() => {
              setActiveVertical('supermall');
              setDepartmentFilter('apparel');
              setSelectedAisle('all');
              setSelectedCategory('all');
              catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }}
            className="group relative rounded-2xl overflow-hidden bg-[#0A2A1B] text-white text-left border border-[#0F3D28]/20 min-h-[148px] p-5 flex flex-col justify-between cursor-pointer hover:shadow-md transition-shadow"
          >
            <img
              src="/images/store/aisle_personal.jpg"
              alt="Fashion and Apparel"
              className="absolute inset-0 w-full h-full object-cover opacity-20 group-hover:scale-105 transition-transform duration-300"
            />
            <div className="relative z-10 space-y-1">
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#E6A817]">
                New Arrivals · 12% Wholesale Off
              </span>
              <h3 className="font-display text-lg font-bold">Fashion &amp; Apparel</h3>
              <p className="text-xs text-white/85">
                Cotton Kurtas, Polo Tees &amp; Casual Wear ready for Sandhaur pickup
              </p>
            </div>
            <span className="relative z-10 inline-flex items-center gap-1 text-xs font-bold text-[#E6A817] mt-3">
              <span>Browse Apparel</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </span>
          </button>
        </section>

        {/* Shop by Aisle / Department Cards */}
        <section aria-label="Shop by Aisle" className="space-y-3">
          <div className="flex items-center justify-between gap-2">
            <h2 className="font-display text-xl sm:text-2xl font-bold text-[#14221B]">
              {t.aislesHeading}
            </h2>
            <span className="text-xs font-medium text-[#526058] tabular-nums">
              {t.aislesSubnote}
            </span>
          </div>

          <div className="flex gap-3 overflow-x-auto no-scrollbar pb-1">
            {visibleAisles.map((aisle) => {
              const active = selectedAisle === aisle.id;
              const loc = t.aisles[aisle.id] || aisle;
              return (
                <button
                  key={aisle.id}
                  type="button"
                  onClick={() => handleSelectAisle(aisle.id)}
                  className={`min-w-[165px] sm:min-w-[180px] p-3.5 rounded-xl border text-left transition-all shrink-0 cursor-pointer ${
                    active
                      ? 'bg-[#0F3D28] text-white border-[#0F3D28] shadow-sm'
                      : 'bg-white text-[#14221B] border-[#0F3D28]/12 hover:border-[#0F3D28]/35'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <span
                      className={`text-[11px] font-semibold uppercase tracking-wider ${
                        active ? 'text-[#E6A817]' : 'text-[#0F3D28]'
                      }`}
                    >
                      {loc.shortLabel}
                    </span>
                  </div>
                  <p className="text-sm font-bold leading-snug line-clamp-1">{loc.label}</p>
                  <p
                    className={`text-[11px] mt-0.5 line-clamp-1 ${
                      active ? 'text-white/75' : 'text-[#526058]'
                    }`}
                  >
                    {loc.description}
                  </p>
                </button>
              );
            })}
          </div>
        </section>

        {/* Department Toggle & Product Catalog Grid */}
        <section ref={catalogRef} aria-label="Product Catalog" className="space-y-4">
          <div className="bg-white p-3.5 rounded-xl border border-[#0F3D28]/10 space-y-3">
            {/* Multi-Department Tabs */}
            <div
              role="tablist"
              aria-label="Filter by Department Category"
              className="flex flex-wrap items-center gap-2 pb-3 border-b border-[#0F3D28]/10"
            >
              <button
                type="button"
                role="tab"
                aria-selected={departmentFilter === 'all'}
                onClick={() => handleSelectDepartment('all')}
                className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                  departmentFilter === 'all'
                    ? 'bg-[#0F3D28] text-white shadow-xs'
                    : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#EFECE2] border border-[#0F3D28]/15'
                }`}
              >
                <LayoutGrid className={`w-4 h-4 ${departmentFilter === 'all' ? 'text-[#E6A817]' : 'text-[#0F3D28]'}`} />
                <span>{t.deptAll}</span>
                <span
                  className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                    departmentFilter === 'all' ? 'bg-[#E6A817] text-[#14221B]' : 'bg-white text-[#0F3D28]'
                  }`}
                >
                  {departmentCounts.all}
                </span>
              </button>

              {departmentCounts.packaged > 0 && (
                <button
                  type="button"
                  role="tab"
                  aria-selected={departmentFilter === 'packaged'}
                  onClick={() => handleSelectDepartment('packaged')}
                  className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                    departmentFilter === 'packaged'
                      ? 'bg-[#0F3D28] text-white shadow-xs'
                      : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#EFECE2] border border-[#0F3D28]/15'
                  }`}
                >
                  <Package className={`w-4 h-4 ${departmentFilter === 'packaged' ? 'text-[#E6A817]' : 'text-[#0F3D28]'}`} />
                  <span>{t.deptPackaged}</span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                      departmentFilter === 'packaged' ? 'bg-[#E6A817] text-[#14221B]' : 'bg-white text-[#0F3D28]'
                    }`}
                  >
                    {departmentCounts.packaged}
                  </span>
                </button>
              )}

              {departmentCounts.frozen > 0 && (
                <button
                  type="button"
                  role="tab"
                  aria-selected={departmentFilter === 'frozen'}
                  onClick={() => handleSelectDepartment('frozen')}
                  className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                    departmentFilter === 'frozen'
                      ? 'bg-[#0369A1] text-white shadow-xs'
                      : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#E0F2FE]/60 border border-[#0369A1]/25'
                  }`}
                >
                  <Snowflake className={`w-4 h-4 ${departmentFilter === 'frozen' ? 'text-white' : 'text-[#0369A1]'}`} />
                  <span>{t.deptFrozen}</span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                      departmentFilter === 'frozen' ? 'bg-white text-[#0369A1]' : 'bg-[#E0F2FE] text-[#0369A1]'
                    }`}
                  >
                    {departmentCounts.frozen}
                  </span>
                </button>
              )}

              {departmentCounts.electronics > 0 && (
                <button
                  type="button"
                  role="tab"
                  aria-selected={departmentFilter === 'electronics'}
                  onClick={() => handleSelectDepartment('electronics')}
                  className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                    departmentFilter === 'electronics'
                      ? 'bg-[#14221B] text-white shadow-xs'
                      : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#EFECE2] border border-[#0F3D28]/15'
                  }`}
                >
                  <Cpu className={`w-4 h-4 ${departmentFilter === 'electronics' ? 'text-[#E6A817]' : 'text-[#0F3D28]'}`} />
                  <span>{t.deptElectronics}</span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                      departmentFilter === 'electronics' ? 'bg-[#E6A817] text-[#14221B]' : 'bg-white text-[#0F3D28]'
                    }`}
                  >
                    {departmentCounts.electronics}
                  </span>
                </button>
              )}

              {departmentCounts.apparel > 0 && (
                <button
                  type="button"
                  role="tab"
                  aria-selected={departmentFilter === 'apparel'}
                  onClick={() => handleSelectDepartment('apparel')}
                  className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                    departmentFilter === 'apparel'
                      ? 'bg-[#0F3D28] text-white shadow-xs'
                      : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#EFECE2] border border-[#0F3D28]/15'
                  }`}
                >
                  <Shirt className={`w-4 h-4 ${departmentFilter === 'apparel' ? 'text-[#E6A817]' : 'text-[#0F3D28]'}`} />
                  <span>{t.deptApparel}</span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                      departmentFilter === 'apparel' ? 'bg-[#E6A817] text-[#14221B]' : 'bg-white text-[#0F3D28]'
                    }`}
                  >
                    {departmentCounts.apparel}
                  </span>
                </button>
              )}

              {departmentCounts.retail > 0 && (
                <button
                  type="button"
                  role="tab"
                  aria-selected={departmentFilter === 'retail'}
                  onClick={() => handleSelectDepartment('retail')}
                  className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap ${
                    departmentFilter === 'retail'
                      ? 'bg-[#0F3D28] text-white shadow-xs'
                      : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#EFECE2] border border-[#0F3D28]/15'
                  }`}
                >
                  <ShoppingBag className={`w-4 h-4 ${departmentFilter === 'retail' ? 'text-[#E6A817]' : 'text-[#0F3D28]'}`} />
                  <span>{t.deptRetail}</span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                      departmentFilter === 'retail' ? 'bg-[#E6A817] text-[#14221B]' : 'bg-white text-[#0F3D28]'
                    }`}
                  >
                    {departmentCounts.retail}
                  </span>
                </button>
              )}

              <button
                type="button"
                role="tab"
                aria-selected={showFavoritesOnly}
                onClick={() => setShowFavoritesOnly((prev) => !prev)}
                className={`px-3.5 py-2 rounded-lg text-xs sm:text-sm font-bold inline-flex items-center gap-2 transition-colors cursor-pointer whitespace-nowrap sm:ml-auto ${
                  showFavoritesOnly
                    ? 'bg-[#0F3D28] text-white ring-2 ring-[#E6A817] shadow-xs'
                    : 'bg-[#FFFBEB] text-[#14221B] hover:bg-[#FEF3C7] border border-[#E6A817]/50'
                }`}
              >
                <Heart
                  className={`w-4 h-4 ${
                    showFavoritesOnly ? 'fill-[#E6A817] text-[#E6A817]' : 'text-[#B45309]'
                  }`}
                />
                <span>{t.favoritesLabel}</span>
                <span
                  className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                    showFavoritesOnly ? 'bg-[#E6A817] text-[#14221B]' : 'bg-white text-[#0F3D28]'
                  }`}
                >
                  {departmentCounts.favorites}
                </span>
              </button>
            </div>

            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-2 text-xs sm:text-sm text-[#526058]">
                <SlidersHorizontal className="w-4 h-4 text-[#0F3D28]" />
                <span>
                  {t.showingLabel} <strong className="text-[#14221B] tabular-nums">{visibleProducts.length}</strong>{' '}
                  {t.ofLabel}{' '}
                  <strong className="text-[#14221B] tabular-nums">{filteredProducts.length}</strong>{' '}
                  {t.verifiedItemsLabel}
                </span>
                {(showFavoritesOnly || activeVertical !== 'store' || selectedAisle !== 'all' || departmentFilter !== 'all' || selectedCategory !== 'all' || searchQuery.trim() !== '') && (
                  <button
                    type="button"
                    onClick={() => {
                      setActiveVertical('store');
                      setShowFavoritesOnly(false);
                      setDepartmentFilter('all');
                      setSelectedAisle('all');
                      setSelectedCategory('all');
                      setSearchQuery('');
                    }}
                    className="ml-1 text-xs font-semibold text-[#0F3D28] underline cursor-pointer"
                  >
                    {t.resetFiltersLabel}
                  </button>
                )}
              </div>

              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 flex-1 md:justify-end">
                <div className="relative flex-1 sm:max-w-xs">
                  <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    type="search"
                    aria-label="Filter current product list by name or SKU"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder={t.catalogSearchPlaceholder}
                    className="w-full pl-9 pr-8 py-1.5 rounded-lg border border-[#0F3D28]/20 bg-[#F8F7F2] text-xs sm:text-sm text-[#14221B] placeholder:text-[#526058]/80 focus:outline-none focus:border-[#0F3D28] focus:bg-white transition-colors"
                  />
                  {searchQuery && (
                    <button
                      type="button"
                      aria-label="Clear search query"
                      onClick={() => setSearchQuery('')}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#526058] hover:text-[#14221B] cursor-pointer"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <ArrowUpDown className="w-4 h-4 text-[#526058]" />
                  <label htmlFor="sort-select" className="text-xs font-semibold text-[#526058]">
                    {t.sortLabel}
                  </label>
                  <select
                    id="sort-select"
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
                    className="px-3 py-1.5 rounded-lg border border-[#0F3D28]/20 bg-[#F8F7F2] text-xs font-semibold text-[#14221B] focus:outline-none"
                  >
                    <option value="popular">{t.sortPopular}</option>
                    <option value="price_asc">{t.sortPriceAsc}</option>
                    <option value="price_desc">{t.sortPriceDesc}</option>
                    <option value="savings">{t.sortSavings}</option>
                  </select>
                </div>
              </div>
            </div>
          </div>

          {/* Horizontal Category Filter Row Directly Above Product Grid */}
          {!loading && (categoryOptions.length > 1 || selectedCategory !== 'all') && (
            <div
              role="tablist"
              aria-label="Filter products by category"
              className="bg-white px-3.5 py-2.5 rounded-xl border border-[#0F3D28]/10 flex items-center gap-2 overflow-x-auto no-scrollbar"
            >
              <span className="text-xs font-bold text-[#526058] uppercase tracking-wider shrink-0 mr-1">
                {t.categoryFilterLabel}
              </span>
              {categoryOptions.map((cat) => {
                const active = selectedCategory === cat.id;
                const label =
                  cat.id === 'all'
                    ? t.allCategoriesLabel
                    : t.categoryLabels[cat.id] || cat.id;
                return (
                  <button
                    key={cat.id}
                    type="button"
                    role="tab"
                    aria-selected={active}
                    onClick={() => setSelectedCategory(cat.id)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold inline-flex items-center gap-1.5 transition-colors shrink-0 cursor-pointer whitespace-nowrap ${
                      active
                        ? 'bg-[#0F3D28] text-white shadow-xs'
                        : 'bg-[#F8F7F2] text-[#14221B] hover:bg-[#EFECE2] border border-[#0F3D28]/15'
                    }`}
                  >
                    <span>{label}</span>
                    <span
                      className={`px-1.5 py-0.5 rounded text-[11px] font-mono-num ${
                        active
                          ? 'bg-[#E6A817] text-[#14221B]'
                          : 'bg-white text-[#0F3D28]'
                      }`}
                    >
                      {cat.count}
                    </span>
                  </button>
                );
              })}
            </div>
          )}

          {loading ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div
                  key={i}
                  className="bg-white rounded-xl border border-[#0F3D28]/10 overflow-hidden p-3 space-y-3"
                >
                  <div className="aspect-square rounded-lg skeleton-shimmer" />
                  <div className="h-3 w-2/3 rounded skeleton-shimmer" />
                  <div className="h-4 w-full rounded skeleton-shimmer" />
                  <div className="h-8 w-full rounded skeleton-shimmer" />
                </div>
              ))}
            </div>
          ) : filteredProducts.length === 0 ? (
            <div className="bg-white rounded-2xl p-10 text-center border border-[#0F3D28]/12 space-y-3">
              <p className="font-display text-xl font-bold text-[#14221B]">
                {showFavoritesOnly && departmentCounts.favorites === 0
                  ? t.noFavoritesTitle
                  : t.noMatchTitle}
              </p>
              <p className="text-sm text-[#526058]">
                {showFavoritesOnly && departmentCounts.favorites === 0
                  ? t.noFavoritesDesc
                  : t.noMatchDesc}
              </p>
              <button
                type="button"
                onClick={() => {
                  setActiveVertical('store');
                  setShowFavoritesOnly(false);
                  setSearchQuery('');
                  setDepartmentFilter('all');
                  setSelectedAisle('all');
                  setSelectedCategory('all');
                }}
                className="px-5 py-2.5 rounded-xl bg-[#0F3D28] text-white text-sm font-semibold cursor-pointer"
              >
                {t.showAllBtn}
              </button>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
                {visibleProducts.map((product, idx) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    index={idx}
                    qtyInCart={cartQtyMap.get(product.id) || 0}
                    lang={lang}
                    isFavorite={favoriteSet.has(product.id)}
                    onToggleFavorite={onToggleFavorite}
                    onAdd={onAddToCart}
                    onUpdateQty={onUpdateCartQty}
                    onQuickView={setQuickViewProduct}
                  />
                ))}
              </div>

              <div ref={sentinelRef} className="py-4 text-center">
                {visibleCount < filteredProducts.length && (
                  <button
                    type="button"
                    onClick={() =>
                      setVisibleCount((prev) => Math.min(prev + BATCH_SIZE, filteredProducts.length))
                    }
                    className="px-6 py-3 rounded-xl bg-white border border-[#0F3D28]/25 text-sm font-semibold text-[#0F3D28] hover:bg-[#0F3D28] hover:text-white transition-colors cursor-pointer"
                  >
                    {t.loadMorePrefix} ({visibleCount} {t.ofLabel} {filteredProducts.length} {t.shownSuffix})
                  </button>
                )}
              </div>
            </>
          )}
        </section>

        {/* Authentic Sandhaur Store Showcase */}
        <section className="bg-white rounded-2xl border border-[#0F3D28]/12 p-6 sm:p-8 space-y-6">
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div className="space-y-1.5">
              <span className="text-xs font-bold uppercase tracking-wider text-[#0F3D28]">
                {t.showcaseEyebrow}
              </span>
              <h2 className="font-display text-2xl font-bold text-[#14221B]">
                {t.showcaseHeading}
              </h2>
              <p className="text-sm text-[#526058] max-w-2xl">
                {t.showcaseDesc}
              </p>
            </div>
            <div className="flex items-center gap-2 text-xs font-semibold text-[#0F3D28]">
              <ShieldCheck className="w-4 h-4 text-[#E6A817]" />
              <span>{t.showcasePolicy}</span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="rounded-xl overflow-hidden aspect-[4/3] bg-[#F8F7F2] border border-[#0F3D28]/10">
              <img
                src="/images/store/exterior.jpg"
                alt="Nav Grocery Store Main Bazaar Sandhaur Exterior"
                loading="lazy"
                className="w-full h-full object-cover"
              />
            </div>
            <div className="rounded-xl overflow-hidden aspect-[4/3] bg-[#F8F7F2] border border-[#0F3D28]/10">
              <img
                src="/images/store/aisle_personal.jpg"
                alt="Personal Care and Household Detergent Aisles"
                loading="lazy"
                className="w-full h-full object-cover"
              />
            </div>
            <div className="rounded-xl overflow-hidden aspect-[4/3] bg-[#F8F7F2] border border-[#0F3D28]/10">
              <img
                src="/images/store/aisle_beverages.jpg"
                alt="Beverages, Oils and Packaged Staples Shelves"
                loading="lazy"
                className="w-full h-full object-cover"
              />
            </div>
          </div>
        </section>

        {/* Official Interactive Digital Visiting Card */}
        <DigitalVisitingCard logoUrl={branding.logoUrl} lang={lang} />
      </main>

      {/* Footer */}
      <footer className="bg-[#0F3D28] text-[#F8F7F2] mt-12 border-t border-[#E6A817]/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-10 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <StoreLogo logoUrl={branding.logoUrl} darkSurface sizeClass="w-11 h-11" />
              <div>
                <h3 className="font-display font-bold text-lg text-white">{t.storeName}</h3>
                <p className="text-xs text-[#E6A817]">
                  {t.fullAddress}
                </p>
              </div>
            </div>
            <p className="text-xs text-white/80 leading-relaxed">
              {lang === 'en' ? branding.storeTagline : t.footerTagline}
            </p>
          </div>

          <div className="space-y-2 text-xs">
            <h4 className="font-bold text-sm text-[#E6A817]">{t.footerCareTitle}</h4>
            <p className="flex items-center gap-2">
              <Phone className="w-3.5 h-3.5 text-[#E6A817] shrink-0" />
              <span>
                <a href="tel:+918847370905" className="hover:underline">+91 8847370905</a>
                {' · '}
                <a href="tel:+919915752905" className="hover:underline">+91 9915752905</a>
              </span>
            </p>
            <p className="flex items-center gap-2">
              <Mail className="w-3.5 h-3.5 text-[#E6A817] shrink-0" />
              <a href="mailto:orders@navgrocerystore.com" className="hover:underline">
                orders@navgrocerystore.com
              </a>
            </p>
            <p className="flex items-center gap-2">
              <Globe className="w-3.5 h-3.5 text-[#E6A817] shrink-0" />
              <a href="https://www.navgrocerystore.com" className="hover:underline">
                www.navgrocerystore.com
              </a>
            </p>
            <div className="pt-1 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => {
                  setTrackerOrderId('');
                  setTrackerOpen(true);
                }}
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#E6A817] hover:underline cursor-pointer"
              >
                <PackageCheck className="w-3.5 h-3.5" />
                <span>{t.footerTrackOrder}</span>
              </button>
              <button
                type="button"
                onClick={onOpenAccount}
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-white hover:underline cursor-pointer"
              >
                <User className="w-3.5 h-3.5 text-[#E6A817]" />
                <span>{t.accountLabel}</span>
              </button>
            </div>
          </div>

          <div className="space-y-2.5 text-xs">
            <h4 className="font-bold text-sm text-[#E6A817]">{t.footerConnectTitle}</h4>
            <div className="flex flex-wrap gap-2">
              <a
                href="https://wa.me/918847370905"
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1.5 rounded-lg bg-[#E6A817] text-[#14221B] font-bold hover:bg-[#f0b72b]"
              >
                {t.footerWhatsAppHelp}
              </a>
              <a
                href="https://www.instagram.com/nav_grocery_store/"
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white inline-flex items-center gap-1.5"
              >
                <Instagram className="w-3.5 h-3.5 text-[#E6A817]" />
                <span>@nav_grocery_store</span>
              </a>
              <a
                href="https://www.youtube.com/@shopatnav"
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white inline-flex items-center gap-1.5"
              >
                <Youtube className="w-3.5 h-3.5 text-[#E6A817]" />
                <span>@shopatnav</span>
              </a>
            </div>
            <p className="text-[11px] text-white/65 pt-1">
              {t.footerDeliveryNote}
            </p>
          </div>
        </div>
      </footer>

      {/* Shopper Modals & Drawers */}
      <QuickViewModal
        product={quickViewProduct}
        qtyInCart={quickViewProduct ? cartQtyMap.get(quickViewProduct.id) || 0 : 0}
        lang={lang}
        isFavorite={quickViewProduct ? favoriteSet.has(quickViewProduct.id) : false}
        onToggleFavorite={onToggleFavorite}
        onClose={() => setQuickViewProduct(null)}
        onAdd={onAddToCart}
        onUpdateQty={onUpdateCartQty}
      />

      <BarcodeScannerModal
        isOpen={scannerOpen}
        products={products}
        onClose={() => setScannerOpen(false)}
        onSelectProduct={(prod) => setQuickViewProduct(prod)}
      />

      <CartCheckoutDrawer
        isOpen={cartOpen}
        logoUrl={branding.logoUrl}
        items={cart}
        fulfillment={fulfillment}
        shippingZone={shippingZone}
        paymentSettings={paymentSettings}
        customerSession={customerSession}
        authLoading={authLoading}
        authError={authError}
        onSignInWithGoogle={onSignInWithGoogle}
        onUpdateSession={(updated) => onUpdateSession(updated)}
        onClose={() => setCartOpen(false)}
        onUpdateQty={onUpdateCartQty}
        onRemoveItem={onRemoveCartItem}
        onClearCart={onClearCart}
        onChangeFulfillment={setFulfillment}
        onTrackOrder={(orderId) => {
          setCartOpen(false);
          setTrackerOrderId(orderId);
          setTrackerOpen(true);
        }}
      />

      <OrderTrackerModal
        isOpen={trackerOpen}
        initialOrderId={trackerOrderId}
        lang={lang}
        onClose={() => setTrackerOpen(false)}
      />

      <StoreAICopilot
        products={products}
        lang={lang}
        onChangeLang={onChangeLang}
        onAddToCart={onAddToCart}
        onSelectAisle={(aisleId) => {
          handleSelectAisle(aisleId);
          catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }}
      />
    </div>
  );
};
