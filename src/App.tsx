import React, { Suspense, useCallback, useEffect, useState } from 'react';
import {
  BrandingSettings,
  CartItem,
  CustomerSession,
  PaymentSettings,
  Product,
} from './types';
import {
  DEFAULT_BRANDING,
  DEFAULT_PAYMENTS,
  fetchBrandingSettings,
  fetchPaymentSettings,
  fetchUnifiedCatalogProducts,
} from './utils/firebase';
import { StoreLang } from './utils/i18n';
import { StorefrontView } from './components/StorefrontView';
import { AccountPageView } from './components/AccountPageView';
import { WhatsAppAuthModal } from './components/WhatsAppAuthModal';

const LazyAdminControlCenter = React.lazy(() =>
  import('./components/admin/AdminControlCenter').then((m) => ({
    default: m.AdminControlCenter,
  }))
);

type AppRoute = 'storefront' | 'account' | 'admin';

function detectRoute(): AppRoute {
  if (typeof window === 'undefined') return 'storefront';
  const path = window.location.pathname.toLowerCase();
  const hash = window.location.hash.toLowerCase();
  if (
    path === '/admin' ||
    path.startsWith('/admin/') ||
    path === '/admin.html' ||
    hash.startsWith('#/admin')
  ) {
    return 'admin';
  }
  if (path === '/account' || path.startsWith('/account/') || hash.startsWith('#/account')) {
    return 'account';
  }
  return 'storefront';
}

export default function App() {
  const [route, setRoute] = useState<AppRoute>(detectRoute);
  const [products, setProducts] = useState<Product[]>([]);
  const [branding, setBranding] = useState<BrandingSettings>(DEFAULT_BRANDING);
  const [paymentSettings, setPaymentSettings] = useState<PaymentSettings>(DEFAULT_PAYMENTS);
  const [loading, setLoading] = useState<boolean>(true);
  const [authModalOpen, setAuthModalOpen] = useState<boolean>(false);

  const [lang, setLang] = useState<StoreLang>(() => {
    try {
      return localStorage.getItem('nav_public_lang_v1') === 'pa' ? 'pa' : 'en';
    } catch {
      return 'en';
    }
  });

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

  const [customerSession, setCustomerSession] = useState<CustomerSession | null>(() => {
    try {
      const raw = localStorage.getItem('nav_customer_session_v2');
      return raw ? (JSON.parse(raw) as CustomerSession) : null;
    } catch {
      return null;
    }
  });

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

  useEffect(() => {
    try {
      localStorage.setItem('nav_public_cart_v3', JSON.stringify(cart));
    } catch {
      // ignore
    }
  }, [cart]);

  useEffect(() => {
    try {
      localStorage.setItem('nav_public_favorites_v1', JSON.stringify(favoriteIds));
    } catch {
      // ignore
    }
  }, [favoriteIds]);

  useEffect(() => {
    const syncRoute = () => setRoute(detectRoute());
    window.addEventListener('popstate', syncRoute);
    window.addEventListener('hashchange', syncRoute);
    return () => {
      window.removeEventListener('popstate', syncRoute);
      window.removeEventListener('hashchange', syncRoute);
    };
  }, []);

  const navigateTo = useCallback((nextPath: '/' | '/account') => {
    if (typeof window !== 'undefined' && window.location.pathname !== nextPath) {
      window.history.pushState({}, '', nextPath);
    }
    setRoute(nextPath === '/account' ? 'account' : 'storefront');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const loadStorefront = useCallback(async () => {
    setLoading(true);
    const [items, bCfg, pCfg] = await Promise.all([
      fetchUnifiedCatalogProducts(),
      fetchBrandingSettings(),
      fetchPaymentSettings(),
    ]);
    setProducts(items);
    setBranding(bCfg);
    setPaymentSettings(pCfg);
    setLoading(false);
  }, []);

  useEffect(() => {
    if (route !== 'admin') {
      loadStorefront();
    }
  }, [route, loadStorefront]);

  const toggleFavorite = (productId: string) => {
    setFavoriteIds((prev) =>
      prev.includes(productId) ? prev.filter((id) => id !== productId) : [...prev, productId]
    );
  };

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

  if (route === 'admin') {
    return (
      <Suspense
        fallback={
          <div className="min-h-screen bg-[#FAF8F3] flex items-center justify-center text-sm font-semibold text-[#0B3B24]">
            Loading Unified Admin Control Center...
          </div>
        }
      >
        <LazyAdminControlCenter />
      </Suspense>
    );
  }

  if (route === 'account') {
    return (
      <>
        <AccountPageView
          logoUrl={branding.logoUrl}
          products={products}
          favoriteIds={favoriteIds}
          customerSession={customerSession}
          paymentSettings={paymentSettings}
          lang={lang}
          onChangeLang={setLang}
          onUpdateSession={setCustomerSession}
          onToggleFavorite={toggleFavorite}
          onAddToCart={(p) => addToCart(p, 1)}
          onNavigateHome={() => navigateTo('/')}
          onOpenWhatsAppAuth={() => setAuthModalOpen(true)}
        />
        <WhatsAppAuthModal
          isOpen={authModalOpen}
          existingSession={customerSession}
          onClose={() => setAuthModalOpen(false)}
          onVerified={(session) => {
            localStorage.setItem('nav_customer_session_v2', JSON.stringify(session));
            setCustomerSession(session);
          }}
          onLogout={() => {
            localStorage.removeItem('nav_customer_session_v2');
            setCustomerSession(null);
          }}
        />
      </>
    );
  }

  return (
    <StorefrontView
      products={products}
      loading={loading}
      branding={branding}
      paymentSettings={paymentSettings}
      lang={lang}
      onChangeLang={setLang}
      favoriteIds={favoriteIds}
      onToggleFavorite={toggleFavorite}
      cart={cart}
      onAddToCart={addToCart}
      onUpdateCartQty={updateCartQty}
      onRemoveCartItem={(id) => setCart((prev) => prev.filter((i) => i.product.id !== id))}
      onClearCart={() => setCart([])}
      customerSession={customerSession}
      onUpdateSession={setCustomerSession}
      onOpenAccount={() => navigateTo('/account')}
    />
  );
}
