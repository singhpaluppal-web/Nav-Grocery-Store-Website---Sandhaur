import { useCallback, useEffect, useState } from 'react';
import { BrandingSettings, PaymentSettings, Product } from './types';
import {
  DEFAULT_BRANDING,
  DEFAULT_PAYMENTS,
  fetchBrandingSettings,
  fetchGroceryProducts,
  fetchPaymentSettings,
} from './utils/firebase';
import { StorefrontView } from './components/StorefrontView';

export default function App() {
  const [products, setProducts] = useState<Product[]>([]);
  const [branding, setBranding] = useState<BrandingSettings>(DEFAULT_BRANDING);
  const [paymentSettings, setPaymentSettings] = useState<PaymentSettings>(DEFAULT_PAYMENTS);
  const [loading, setLoading] = useState<boolean>(true);

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
    loadStorefront();
  }, [loadStorefront]);

  return (
    <StorefrontView
      products={products}
      loading={loading}
      branding={branding}
      paymentSettings={paymentSettings}
    />
  );
}
