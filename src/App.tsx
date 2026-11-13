import React, { Suspense, useCallback, useEffect, useState } from 'react';
import { BrandingSettings, PaymentSettings, Product } from './types';
import {
  DEFAULT_BRANDING,
  DEFAULT_PAYMENTS,
  fetchBrandingSettings,
  fetchGroceryProducts,
  fetchPaymentSettings,
} from './utils/firebase';
import { StorefrontView } from './components/StorefrontView';

const LazyAdminControlCenter = React.lazy(() =>
  import('./components/admin/AdminControlCenter').then((m) => ({
    default: m.AdminControlCenter,
  }))
);

function detectAdminRoute(): boolean {
  if (typeof window === 'undefined') return false;
  const path = window.location.pathname.toLowerCase();
  const hash = window.location.hash.toLowerCase();
  return (
    path === '/admin' ||
    path.startsWith('/admin/') ||
    path === '/admin.html' ||
    hash.startsWith('#/admin')
  );
}

export default function App() {
  const [isAdmin, setIsAdmin] = useState<boolean>(detectAdminRoute);
  const [products, setProducts] = useState<Product[]>([]);
  const [branding, setBranding] = useState<BrandingSettings>(DEFAULT_BRANDING);
  const [paymentSettings, setPaymentSettings] = useState<PaymentSettings>(DEFAULT_PAYMENTS);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const syncRoute = () => setIsAdmin(detectAdminRoute());
    window.addEventListener('popstate', syncRoute);
    window.addEventListener('hashchange', syncRoute);
    return () => {
      window.removeEventListener('popstate', syncRoute);
      window.removeEventListener('hashchange', syncRoute);
    };
  }, []);

  const loadStorefront = useCallback(async () => {
    setLoading(true);
    const [items, bCfg, pCfg] = await Promise.all([
      fetchGroceryProducts(),
      fetchBrandingSettings(),
      fetchPaymentSettings(),
    ]);
    setProducts(items);
    setBranding(bCfg);
    setPaymentSettings(pCfg);
    setLoading(false);
  }, []);

  useEffect(() => {
    if (!isAdmin) {
      loadStorefront();
    }
  }, [isAdmin, loadStorefront]);

  if (isAdmin) {
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

  return (
    <StorefrontView
      products={products}
      loading={loading}
      branding={branding}
      paymentSettings={paymentSettings}
    />
  );
}
