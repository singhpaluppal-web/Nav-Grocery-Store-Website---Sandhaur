import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Search,
  MapPin,
  ShoppingBasket,
  Truck,
  Store,
  UserCheck,
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
} from 'lucide-react';
import {
  BrandingSettings,
  CartItem,
  CustomerSession,
  FulfillmentMode,
  HeroBanner,
  PaymentSettings,
  Product,
} from '../types';
import {
  DepartmentFilter,
  GROCERY_AISLES,
  isFrozenProduct,
  matchesDepartmentCategory,
  matchesGroceryAisle,
} from '../utils/aisles';
import { buildBrandPackageSvg, resolveProductImage } from '../utils/brandPhotos';
import { StoreLogo } from './StoreLogo';
import { HeroCarousel } from './HeroCarousel';
import { ProductCard } from './ProductCard';
import { QuickViewModal } from './QuickViewModal';
import { CartCheckoutDrawer } from './CartCheckoutDrawer';
import { WhatsAppAuthModal } from './WhatsAppAuthModal';
import { StoreAICopilot } from './StoreAICopilot';
import { DigitalVisitingCard } from './DigitalVisitingCard';
import { OrderTrackerModal } from './OrderTrackerModal';
import { LanguageSwitcher } from './LanguageSwitcher';
import { getStoreTranslations, StoreLang } from '../utils/i18n';

interface StorefrontViewProps {
  products: Product[];
  loading: boolean;
  branding: BrandingSettings;
  paymentSettings: PaymentSettings;
}

const BATCH_SIZE = 24;

export const StorefrontView: React.FC<StorefrontViewProps> = ({
  products,
  loading,
  branding,
  paymentSettings,
}) => {
  const [departmentFilter, setDepartmentFilter] = useState<DepartmentFilter>('all');
  const [selectedAisle, setSelectedAisle] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchOpen, setSearchOpen] = useState<boolean>(false);
  const [sortBy, setSortBy] = useState<'popular' | 'price_asc' | 'price_desc' | 'savings'>('popular');
  const [visibleCount, setVisibleCount] = useState<number>(BATCH_SIZE);

  const [fulfillment, setFulfillment] = useState<FulfillmentMode>('delivery');
  const [cartOpen, setCartOpen] = useState<boolean>(false);
  const [authOpen, setAuthOpen] = useState<boolean>(false);
  const [trackerOpen, setTrackerOpen] = useState<boolean>(false);
  const [trackerOrderId, setTrackerOrderId] = useState<string>('');
  const [quickViewProduct, setQuickViewProduct] = useState<Product | null>(null);

  const [cart, setCart] = useState<CartItem[]>(() => {
    try {
      const saved = localStorage.getItem('nav_public_cart_v3');
      return saved ? (JSON.parse(saved) as CartItem[]) : [];
    } catch {
      return [];
    }
  });

  const [favoriteIds, setFavoriteIds] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('nav_public_favorites_v1');
      return saved ? (JSON.parse(saved) as string[]) : [];
    } catch {
      return [];
    }
  });

  const [showFavoritesOnly, setShowFavoritesOnly] = useState<boolean>(false);

  const [lang, setLang] = useState<StoreLang>(() => {
    try {
      const saved = localStorage.getItem('nav_public_lang_v1');
      return saved === 'pa' || saved === 'hi' ? saved : 'en';
    } catch {
      return 'en';
    }
  });

  const t = useMemo(() => getStoreTranslations(lang), [lang]);

  useEffect(() => {
    try {
      localStorage.setItem('nav_public_lang_v1', lang);
    } catch {
      // ignore storage error
    }
    if (typeof document !== 'undefined') {
      document.documentElement.lang = lang;
    }
  }, [lang]);

  const [customerSession, setCustomerSession] = useState<CustomerSession | null>(() => {
    try {
      const raw = localStorage.getItem('nav_customer_session_v2');
      return raw ? (JSON.parse(raw) as CustomerSession) : null;
    } catch {
      return null;
    }
  });

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

  useEffect(() => {
    localStorage.setItem('nav_public_cart_v3', JSON.stringify(cart));
  }, [cart]);

  useEffect(() => {
    localStorage.setItem('nav_public_favorites_v1', JSON.stringify(favoriteIds));
  }, [favoriteIds]);

  const favoriteSet = useMemo(() => new Set(favoriteIds), [favoriteIds]);

  const toggleFavorite = (productId: string) => {
    setFavoriteIds((prev) =>
      prev.includes(productId) ? prev.filter((id) => id !== productId) : [...prev, productId]
    );
  };

  const departmentCounts = useMemo(() => {
    let frozen = 0;
    let favorites = 0;
    for (const p of products) {
      if (isFrozenProduct(p)) frozen++;
      if (favoriteSet.has(p.id)) favorites++;
    }
    return {
      all: products.length,
      packaged: products.length - frozen,
      frozen,
      favorites,
    };
  }, [products, favoriteSet]);

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    const list = products.filter((p) => {
      if (showFavoritesOnly && !favoriteSet.has(p.id)) return false;
      if (!matchesDepartmentCategory(p, departmentFilter)) return false;
      if (!matchesGroceryAisle(p, selectedAisle)) return false;
      if (!q) return true;
      const hay = `${p.id} ${p.brand} ${p.name} ${p.category} ${p.department}`.toLowerCase();
      return q.split(/\s+/).every((token) => hay.includes(token));
    });

    if (sortBy === 'price_asc') return [...list].sort((a, b) => a.sale - b.sale);
    if (sortBy === 'price_desc') return [...list].sort((a, b) => b.sale - a.sale);
    if (sortBy === 'savings') return [...list].sort((a, b) => b.mrp - b.sale - (a.mrp - a.sale));
    return list;
  }, [products, showFavoritesOnly, favoriteSet, departmentFilter, selectedAisle, searchQuery, sortBy]);

  const predictiveSuggestions = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (q.length < 2) return [];
    return filteredProducts.slice(0, 6);
  }, [filteredProducts, searchQuery]);

  useEffect(() => {
    setVisibleCount(BATCH_SIZE);
  }, [showFavoritesOnly, departmentFilter, selectedAisle, searchQuery, sortBy]);

  const handleSelectDepartment = (dept: DepartmentFilter) => {
    setDepartmentFilter(dept);
    if (dept === 'frozen' && selectedAisle !== 'all' && selectedAisle !== 'deals') {
      setSelectedAisle('all');
    } else if (dept === 'packaged' && selectedAisle === 'frozen') {
      setSelectedAisle('all');
    }
  };

  const handleSelectAisle = (aisleId: string) => {
    setSelectedAisle(aisleId);
    if (aisleId === 'frozen' && departmentFilter === 'packaged') {
      setDepartmentFilter('all');
    } else if (
      aisleId !== 'all' &&
      aisleId !== 'deals' &&
      aisleId !== 'frozen' &&
      departmentFilter === 'frozen'
    ) {
      setDepartmentFilter('all');
    }
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

  const addToCart = (product: Product, qtyToAdd = 1) => {
    const increment = Math.max(1, Math.floor(qtyToAdd));
    setCart((prev) => {
      const idx = prev.findIndex((i) => i.product.id === product.id);
      if (idx >= 0) {
        return prev.map((item, i) =>
          i === idx ? { ...item, quantity: item.quantity + increment } : item
        );
      }
      return [...prev, { product, quantity: increment }];
    });
  };

  const updateCartQty = (productId: string, qty: number) => {
    if (qty <= 0) {
      setCart((prev) => prev.filter((i) => i.product.id !== productId));
      return;
    }
    setCart((prev) =>
      prev.map((item) => (item.product.id === productId ? { ...item, quantity: qty } : item))
    );
  };

  const handleBannerAction = (banner: HeroBanner) => {
    handleSelectAisle(banner.targetAisle || 'all');
    catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#F8F7F2] text-[#14221B]">
      {/* Announcement Strip */}
      <div className="bg-[#0A2A1B] text-[#F8F7F2] text-[11px] sm:text-xs py-1.5 px-4 text-center font-medium">
        <span>{lang === 'en' ? branding.announcementText : t.announcementFallback}</span>
        <span className="hidden md:inline text-[#E6A817] ml-3">
          · {t.helplineLabel} +91 8847370905 / +91 9915752905
        </span>
      </div>

      {/* Pure Public Sticky Header — Zero Admin or External Marketplace Links */}
      <header className="sticky top-0 z-30 bg-[#0F3D28] text-white border-b border-[#E6A817]/25 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-3">
          {/* Logo & Sandhaur Location */}
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

          {/* Predictive Search Bar */}
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
                  className="px-2.5 text-xs font-semibold text-[#526058] hover:text-[#14221B] cursor-pointer"
                >
                  {t.clearLabel}
                </button>
              )}
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
                          if (!el.dataset.svgFallback) {
                            el.dataset.svgFallback = '1';
                            el.src = buildBrandPackageSvg(item);
                          }
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

          {/* Right Shopper Controls ONLY */}
          <div className="flex items-center gap-2 shrink-0">
            <LanguageSwitcher lang={lang} onChange={setLang} />

            <div className="hidden xl:flex items-center bg-[#0A2A1B] rounded-xl p-1 text-xs font-semibold">
              <button
                type="button"
                onClick={() => setFulfillment('delivery')}
                className={`px-2.5 py-1.5 rounded-lg inline-flex items-center gap-1 whitespace-nowrap cursor-pointer ${
                  fulfillment === 'delivery' ? 'bg-[#E6A817] text-[#14221B]' : 'text-white/80 hover:text-white'
                }`}
              >
                <Truck className="w-3.5 h-3.5" />
                <span>{t.deliveryMode}</span>
              </button>
              <button
                type="button"
                onClick={() => setFulfillment('pickup')}
                className={`px-2.5 py-1.5 rounded-lg inline-flex items-center gap-1 whitespace-nowrap cursor-pointer ${
                  fulfillment === 'pickup' ? 'bg-[#E6A817] text-[#14221B]' : 'text-white/80 hover:text-white'
                }`}
              >
                <Store className="w-3.5 h-3.5" />
                <span>{t.pickupMode}</span>
              </button>
            </div>

            <button
              type="button"
              onClick={() => {
                setShowFavoritesOnly((prev) => !prev);
                catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
              }}
              aria-pressed={showFavoritesOnly}
              className={`px-3 py-2 rounded-xl text-xs font-semibold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer transition-colors ${
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
              className="px-3 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
            >
              <PackageCheck className="w-3.5 h-3.5 text-[#E6A817]" />
              <span className="hidden md:inline">{t.trackOrderLabel}</span>
            </button>

            <button
              type="button"
              onClick={() => setAuthOpen(true)}
              className="px-3 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
            >
              <UserCheck className="w-3.5 h-3.5 text-[#E6A817]" />
              <span className="hidden xl:inline">
                {customerSession ? customerSession.name.split(' ')[0] : t.whatsappLoginLabel}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setCartOpen(true)}
              className="px-3.5 py-2 rounded-xl bg-[#E6A817] text-[#14221B] hover:bg-[#f0b72b] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
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
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-8">
        {/* Auto-Sliding Promotional Shopping Hero Banner */}
        <HeroCarousel
          banners={branding.banners}
          lang={lang}
          onAction={handleBannerAction}
          onShopAll={() => {
            setDepartmentFilter('all');
            setSelectedAisle('all');
            catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }}
        />

        {/* Shop by Grocery Aisle */}
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
            {GROCERY_AISLES.map((aisle) => {
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

        {/* Department Toggle & Chunked 24-Batch Product Grid */}
        <section ref={catalogRef} aria-label="Product Catalog" className="space-y-4">
          <div className="bg-white p-3.5 rounded-xl border border-[#0F3D28]/10 space-y-3">
            {/* Packaged Groceries vs -18°C Frozen Foods Toggle */}
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
                {(showFavoritesOnly || selectedAisle !== 'all' || departmentFilter !== 'all' || searchQuery.trim() !== '') && (
                  <button
                    type="button"
                    onClick={() => {
                      setShowFavoritesOnly(false);
                      setDepartmentFilter('all');
                      setSelectedAisle('all');
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
                  setShowFavoritesOnly(false);
                  setSearchQuery('');
                  setDepartmentFilter('all');
                  setSelectedAisle('all');
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
                    onToggleFavorite={toggleFavorite}
                    onAdd={addToCart}
                    onUpdateQty={updateCartQty}
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

      {/* Official Public Footer (Strict Privacy Shield: Zero Owner PII, Zero GSTIN, Zero External Portal Links) */}
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
            <div className="pt-1">
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
        onToggleFavorite={toggleFavorite}
        onClose={() => setQuickViewProduct(null)}
        onAdd={addToCart}
        onUpdateQty={updateCartQty}
      />

      <CartCheckoutDrawer
        isOpen={cartOpen}
        logoUrl={branding.logoUrl}
        items={cart}
        fulfillment={fulfillment}
        paymentSettings={paymentSettings}
        customerSession={customerSession}
        onClose={() => setCartOpen(false)}
        onUpdateQty={updateCartQty}
        onRemoveItem={(id) => setCart((prev) => prev.filter((i) => i.product.id !== id))}
        onClearCart={() => setCart([])}
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

      <WhatsAppAuthModal
        isOpen={authOpen}
        existingSession={customerSession}
        onClose={() => setAuthOpen(false)}
        onVerified={(session) => {
          localStorage.setItem('nav_customer_session_v2', JSON.stringify(session));
          setCustomerSession(session);
        }}
        onLogout={() => {
          localStorage.removeItem('nav_customer_session_v2');
          setCustomerSession(null);
        }}
      />

      <StoreAICopilot
        products={products}
        lang={lang}
        onChangeLang={setLang}
        onAddToCart={addToCart}
        onSelectAisle={(aisleId) => {
          setSelectedAisle(aisleId);
          catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }}
      />
    </div>
  );
};
