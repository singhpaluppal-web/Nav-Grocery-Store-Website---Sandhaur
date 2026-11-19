export type StoreLang = 'en' | 'pa';

export interface StoreTranslations {
  storeName: string;
  headerLocation: string;
  fullAddress: string;
  announcementFallback: string;
  helplineLabel: string;
  searchPlaceholder: string;
  clearLabel: string;
  scanBarcodeLabel: string;
  deliveryMode: string;
  pickupMode: string;
  favoritesLabel: string;
  trackOrderLabel: string;
  accountLabel: string;
  googleSignInLabel: string;
  signOutLabel: string;
  basketLabel: string;
  verticals: {
    store: { label: string; badge: string };
    supermall: { label: string; badge: string };
    express: { label: string; badge: string };
    essentials: { label: string; badge: string };
  };
  heroEyebrow: string;
  heroTitle: string;
  heroSubtitle: string;
  heroDeliveryBadge: string;
  heroPickupBadge: string;
  heroShopAllCta: string;
  heroWhatsAppCta: string;
  aislesHeading: string;
  aislesSubnote: string;
  deptAll: string;
  deptPackaged: string;
  deptFrozen: string;
  deptElectronics: string;
  deptApparel: string;
  deptRetail: string;
  showingLabel: string;
  ofLabel: string;
  verifiedItemsLabel: string;
  resetFiltersLabel: string;
  allCategoriesLabel: string;
  categoryFilterLabel: string;
  categoryLabels: Record<string, string>;
  catalogSearchPlaceholder: string;
  sortLabel: string;
  sortPopular: string;
  sortPriceAsc: string;
  sortPriceDesc: string;
  sortSavings: string;
  noFavoritesTitle: string;
  noFavoritesDesc: string;
  noMatchTitle: string;
  noMatchDesc: string;
  showAllBtn: string;
  loadMorePrefix: string;
  shownSuffix: string;
  addBtn: string;
  outOfStock: string;
  savePrefix: string;
  quickViewLabel: string;
  showcaseEyebrow: string;
  showcaseHeading: string;
  showcaseDesc: string;
  showcasePolicy: string;
  vcardEyebrow: string;
  vcardTagline: string;
  vcardActionsTitle: string;
  vcardCallNow: string;
  vcardWhatsAppChat: string;
  vcardSaveContact: string;
  vcardShareCard: string;
  vcardCopied: string;
  vcardHoursNote: string;
  footerTagline: string;
  footerCareTitle: string;
  footerTrackOrder: string;
  footerConnectTitle: string;
  footerWhatsAppHelp: string;
  footerDeliveryNote: string;
  trackerTitle: string;
  trackerSubtitle: string;
  trackerInputLabel: string;
  trackerSubmit: string;
  trackerChecking: string;
  trackerRecent: string;
  trackerEmptyTitle: string;
  trackerEmptyDesc: string;
  quickViewAddBtn: string;
  quickViewIngredients: string;
  quickViewComposition: string;
  quickViewShelfLife: string;
  quickViewAllergens: string;
  quickViewShare: string;
  quickViewLinkCopied: string;
  bulkCalcTitle: string;
  bulkQtyLabel: string;
  bulkTotalLabel: string;
  bulkMrpLabel: string;
  bulkSavingsLabel: string;
  bulkAddCta: string;
  bulkPacksUnit: string;
  bulkFreeDelivery: string;
  aisles: Record<string, { label: string; shortLabel: string; description: string }>;
}

const TRANSLATIONS: Record<StoreLang, StoreTranslations> = {
  en: {
    storeName: 'Nav Grocery & Nav Kart',
    headerLocation: 'Main Bazaar, Sandhaur 148020',
    fullAddress: 'Main Bazaar, Sandhaur, District Malerkotla, Punjab 148020',
    announcementFallback:
      'Flat 12% Off MRP on all 308+ Items · Open 8:00 AM – 9:30 PM · 15-Min Express & 2-Hr Delivery in Sandhaur 148020',
    helplineLabel: 'Helpline:',
    searchPlaceholder: 'Search 320+ SKUs: Aashirvaad, Verka, boAt, Philips, Monte Carlo...',
    clearLabel: 'Clear',
    scanBarcodeLabel: 'Scan',
    deliveryMode: 'Express / 2-Hr',
    pickupMode: '30-Min Pickup',
    favoritesLabel: 'Wishlist',
    trackOrderLabel: 'Orders',
    accountLabel: 'Account',
    googleSignInLabel: 'Continue with Google',
    signOutLabel: 'Sign Out',
    basketLabel: 'Cart',
    verticals: {
      store: { label: 'Store (Grocery)', badge: '308 SKUs' },
      supermall: { label: 'Nav Kart Supermall', badge: 'Tech, Apparel & Retail' },
      express: { label: 'Express (15-Min)', badge: 'Sandhaur 148020' },
      essentials: { label: 'Food & Essentials', badge: '-18°C Frozen' },
    },
    heroEyebrow: 'Nav Grocery Store & Nav Kart · Main Bazaar, Sandhaur 148020',
    heroTitle: 'Yellow Friday Wholesale Sale: Flat 12% Off MRP',
    heroSubtitle:
      '100% factory-sealed pantry staples, pure cow desi ghee, -18°C frozen foods, electronics & apparel — zero fresh produce.',
    heroDeliveryBadge: '15-Min Express & 2-Hr Local Delivery (148020)',
    heroPickupBadge: '30-Min Main Bazaar Store Pickup',
    heroShopAllCta: 'Shop All Deals',
    heroWhatsAppCta: 'Order on WhatsApp',
    aislesHeading: 'Shop by Department & Aisle',
    aislesSubnote: 'Strictly Sealed Retail & -18°C Cold-Chain Goods',
    deptAll: 'All Store Items',
    deptPackaged: 'Packaged Groceries',
    deptFrozen: '-18°C Frozen Foods',
    deptElectronics: 'Electronics & Home',
    deptApparel: 'Apparel & Fashion',
    deptRetail: 'General Retail Goods',
    showingLabel: 'Showing',
    ofLabel: 'of',
    verifiedItemsLabel: 'verified items',
    resetFiltersLabel: 'Reset Filters',
    allCategoriesLabel: 'All Categories',
    categoryFilterLabel: 'Categories:',
    categoryLabels: {
      'Atta & Dals': 'Atta & Dals',
      'Desi Ghee & Oils': 'Desi Ghee & Oils',
      Spices: 'Spices',
      'Frozen Foods': 'Frozen Foods',
      Frozen: 'Frozen',
      'Grocery & Staples': 'Grocery & Staples',
      Dairy: 'Dairy',
      Snacks: 'Snacks',
      Beverages: 'Beverages',
      'Personal Care': 'Personal Care',
      'Household Cleaning': 'Household Cleaning',
      'Audio & Headphones': 'Audio & Headphones',
      'Power & Charging': 'Power & Charging',
      'Home Lighting': 'Home Lighting',
      'Kitchen Appliances': 'Kitchen Appliances',
      'Home Appliances': 'Home Appliances',
      'Kitchen Storage': 'Kitchen Storage',
      Wearables: 'Wearables',
      'Menswear & Kurtas': 'Menswear & Kurtas',
      'Casual Apparel': 'Casual Apparel',
      'General Retail & Travel': 'General Retail & Travel',
      'General Retail & Home Utility': 'General Retail & Home Utility',
      'General Retail & Stationery': 'General Retail & Stationery',
    },
    catalogSearchPlaceholder: 'Filter by product name, brand, barcode or SKU (e.g. NAV081)...',
    sortLabel: 'Sort:',
    sortPopular: 'Popular (SKU Order)',
    sortPriceAsc: 'Price: Low to High',
    sortPriceDesc: 'Price: High to Low',
    sortSavings: 'Max Savings (₹)',
    noFavoritesTitle: 'Your wishlist is empty',
    noFavoritesDesc: 'Tap the heart button on any product card to save items for quick re-ordering.',
    noMatchTitle: 'No matching items found',
    noMatchDesc: 'Try clearing your search or switching back to All Store Items.',
    showAllBtn: 'Show All Store Items',
    loadMorePrefix: 'Load Next 24 Items',
    shownSuffix: 'shown',
    addBtn: 'Add',
    outOfStock: 'Out of Stock',
    savePrefix: 'Save',
    quickViewLabel: 'Quick View',
    showcaseEyebrow: 'Main Bazaar · Sandhaur · District Malerkotla · Punjab 148020 · 8:00 AM – 9:30 PM',
    showcaseHeading: 'Trusted Local Supermarket & Nav Kart Multi-Category Hub',
    showcaseDesc:
      'Every product is factory-sealed and stocked cleanly on our Main Bazaar shelves—offered at flat 12% wholesale savings off MRP with 15-min express dispatch, 2-hour local delivery, and 30-minute store pickup.',
    showcasePolicy: 'Zero Fresh Produce Policy · 100% Sealed & -18°C Frozen',
    vcardEyebrow: 'Official Digital Visiting Card',
    vcardTagline: '100% Sealed Branded Groceries, Frozen Foods, Electronics & Apparel · Flat 12% Off MRP',
    vcardActionsTitle: 'Instant Customer Actions',
    vcardCallNow: 'Call Now',
    vcardWhatsAppChat: 'WhatsApp Chat',
    vcardSaveContact: 'Save Contact (vCard)',
    vcardShareCard: 'Share Store Card',
    vcardCopied: 'Details Copied',
    vcardHoursNote:
      'Hours: 8:00 AM – 9:30 PM daily at Main Bazaar, Sandhaur · Free local delivery on orders above ₹499 or 30-min express pickup.',
    footerTagline: 'Nav Grocery Store & Nav Kart · Main Bazaar, Sandhaur 148020',
    footerCareTitle: 'Helpline & Customer Care',
    footerTrackOrder: 'Track Order Status',
    footerConnectTitle: 'Connect With Nav Grocery & Nav Kart',
    footerWhatsAppHelp: 'WhatsApp Order Help',
    footerDeliveryNote:
      'Hours: 8:00 AM – 9:30 PM · 15-Min Express & 2-Hr Delivery in Sandhaur 148020 · 30-Min Main Bazaar Pickup',
    trackerTitle: 'Order Status Tracker',
    trackerSubtitle: 'Live delivery & 30-minute pickup tracking · Nav Grocery Store & Nav Kart',
    trackerInputLabel: 'Enter Your Order ID',
    trackerSubmit: 'Track Order',
    trackerChecking: 'Checking...',
    trackerRecent: 'Recent orders placed on this device:',
    trackerEmptyTitle: 'Check Your Delivery or Store Pickup Status',
    trackerEmptyDesc:
      'Enter your NGS-... Order ID from your digital bill above to inspect live preparation, dispatch, and pickup readiness.',
    quickViewAddBtn: 'Add to Basket',
    quickViewIngredients: 'Ingredients / Specs',
    quickViewComposition: 'Key Specifications',
    quickViewShelfLife: 'Shelf Life / Warranty & Storage',
    quickViewAllergens: 'Allergen & Safety Notes',
    quickViewShare: 'Share',
    quickViewLinkCopied: 'Link Copied!',
    bulkCalcTitle: 'Bulk Price & Savings Calculator',
    bulkQtyLabel: 'Qty',
    bulkTotalLabel: 'Wholesale Total',
    bulkMrpLabel: 'Total MRP',
    bulkSavingsLabel: 'Your Total Savings',
    bulkAddCta: 'Add',
    bulkPacksUnit: 'packs',
    bulkFreeDelivery: 'Free Sandhaur Delivery Unlocked',
    aisles: {
      all: { label: 'All Store Items', shortLabel: 'All Items', description: 'Complete sealed catalog' },
      deals: { label: '12% Off Deals', shortLabel: '12% Deals', description: 'Wholesale savings off MRP' },
      frozen: { label: 'Frozen Veg & Snacks (-18°C)', shortLabel: 'Frozen -18°C', description: 'Safal, McCain & Verka cold chain' },
      atta: { label: 'Atta, Rice & Dal', shortLabel: 'Atta & Dal', description: 'Aashirvaad, India Gate & Fortune' },
      ghee: { label: 'Desi Ghee & Oils', shortLabel: 'Ghee & Oils', description: 'Verka, Amul, Fortune & Mustard Oil' },
      spices: { label: 'Spices & Masale', shortLabel: 'Spices', description: 'MDH, Catch, Everest & Tata Salt' },
      beverages: { label: 'Tea, Beverages & Biscuits', shortLabel: 'Tea & Snacks', description: 'Tata Tea, Red Label, Maggi & Biscuits' },
      household: { label: 'Household & Personal Care', shortLabel: 'Household', description: 'Surf Excel, Ariel, Vim, Dettol & Dove' },
      electronics: { label: 'Electronics & Appliances', shortLabel: 'Electronics', description: 'Audio, LED bulbs, kettles & irons' },
      apparel: { label: 'Apparel & Fashion', shortLabel: 'Apparel', description: 'Kurtas, polos & casual trackwear' },
      retail: { label: 'General Retail Goods', shortLabel: 'General Retail', description: 'Backpacks, torches & stationery' },
    },
  },
  pa: {
    storeName: 'ਨਵ ਗ੍ਰੋਸਰੀ ਤੇ ਨਵ ਕਾਰਟ',
    headerLocation: 'ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ 148020',
    fullAddress: 'ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ, ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ, ਪੰਜਾਬ 148020',
    announcementFallback:
      'ਸਾਰੀਆਂ 308+ ਵਸਤੂਆਂ ’ਤੇ MRP ਤੋਂ 12% ਫਲੈਟ ਛੋਟ · ਸੰਧੌੜ 148020 ਵਿੱਚ 15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ ਤੇ 2-ਘੰਟੇ ਡਿਲੀਵਰੀ',
    helplineLabel: 'ਹੈਲਪਲਾਈਨ:',
    searchPlaceholder: '320+ ਚੀਜ਼ਾਂ ਖੋਜੋ: Aashirvaad, Verka, boAt, Philips...',
    clearLabel: 'ਸਾਫ਼ ਕਰੋ',
    scanBarcodeLabel: 'ਸਕੈਨ',
    deliveryMode: 'ਐਕਸਪ੍ਰੈੱਸ / 2-ਘੰਟੇ',
    pickupMode: '30-ਮਿੰਟ ਪਿਕਅੱਪ',
    favoritesLabel: 'ਪਸੰਦੀਦਾ',
    trackOrderLabel: 'ਆਰਡਰ',
    accountLabel: 'ਖਾਤਾ',
    googleSignInLabel: 'ਗੂਗਲ ਨਾਲ ਜਾਰੀ ਰੱਖੋ',
    signOutLabel: 'ਸਾਈਨ ਆਊਟ',
    basketLabel: 'ਟੋਕਰੀ',
    verticals: {
      store: { label: 'ਸਟੋਰ (ਕਰਿਆਨਾ)', badge: '308 ਚੀਜ਼ਾਂ' },
      supermall: { label: 'ਨਵ ਕਾਰਟ ਸੁਪਰਮਾਲ', badge: 'ਟੈੱਕ, ਕੱਪੜੇ ਤੇ ਰਿਟੇਲ' },
      express: { label: 'ਐਕਸਪ੍ਰੈੱਸ (15-ਮਿੰਟ)', badge: 'ਸੰਧੌੜ 148020' },
      essentials: { label: 'ਫੂਡ ਤੇ ਜ਼ਰੂਰੀ ਸਮਾਨ', badge: '-18°C ਫਰੋਜ਼ਨ' },
    },
    heroEyebrow: 'ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਤੇ ਨਵ ਕਾਰਟ · ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ 148020',
    heroTitle: 'ਯੈਲੋ ਫ੍ਰਾਈਡੇ ਥੋਕ ਸੇਲ: ਸਾਰੀਆਂ ਚੀਜ਼ਾਂ ’ਤੇ 12% ਫਲੈਟ ਛੋਟ',
    heroSubtitle:
      '100% ਫੈਕਟਰੀ-ਸੀਲਬੰਦ ਰਾਸ਼ਨ, ਸ਼ੁੱਧ ਦੇਸੀ ਘਿਓ, -18°C ਫਰੋਜ਼ਨ ਫੂਡ, ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਅਤੇ ਕੱਪੜੇ।',
    heroDeliveryBadge: 'ਸੰਧੌੜ 148020 ਵਿੱਚ 15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ ਤੇ 2-ਘੰਟੇ ਡਿਲੀਵਰੀ',
    heroPickupBadge: '30-ਮਿੰਟ ਮੇਨ ਬਾਜ਼ਾਰ ਸਟੋਰ ਪਿਕਅੱਪ',
    heroShopAllCta: 'ਸਾਰੀਆਂ ਪੇਸ਼ਕਸ਼ਾਂ ਵੇਖੋ',
    heroWhatsAppCta: 'ਵਟਸਐਪ ’ਤੇ ਆਰਡਰ ਕਰੋ',
    aislesHeading: 'ਸ਼੍ਰੇਣੀ ਅਨੁਸਾਰ ਖਰੀਦਦਾਰੀ ਕਰੋ',
    aislesSubnote: 'ਕੇਵਲ ਸੀਲਬੰਦ ਸਮਾਨ ਅਤੇ -18°C ਫਰੋਜ਼ਨ ਵਸਤੂਆਂ',
    deptAll: 'ਸਾਰਾ ਸਟੋਰ',
    deptPackaged: 'ਪੈਕਡ ਕਰਿਆਨਾ',
    deptFrozen: '-18°C ਫਰੋਜ਼ਨ ਫੂਡ',
    deptElectronics: 'ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਤੇ ਘਰੇਲੂ',
    deptApparel: 'ਕੱਪੜੇ ਤੇ ਫੈਸ਼ਨ',
    deptRetail: 'ਜਨਰਲ ਰਿਟੇਲ ਸਮਾਨ',
    showingLabel: 'ਦਿਖਾ ਰਹੇ ਹਾਂ',
    ofLabel: 'ਵਿੱਚੋਂ',
    verifiedItemsLabel: 'ਪ੍ਰਮਾਣਿਤ ਵਸਤੂਆਂ',
    resetFiltersLabel: 'ਫਿਲਟਰ ਹਟਾਓ',
    allCategoriesLabel: 'ਸਾਰੀਆਂ ਸ਼੍ਰੇਣੀਆਂ',
    categoryFilterLabel: 'ਸ਼੍ਰੇਣੀਆਂ:',
    categoryLabels: {
      'Atta & Dals': 'ਆਟਾ ਤੇ ਦਾਲਾਂ',
      'Desi Ghee & Oils': 'ਦੇਸੀ ਘਿਓ ਤੇ ਤੇਲ',
      Spices: 'ਮਸਾਲੇ',
      'Frozen Foods': 'ਫਰੋਜ਼ਨ ਫੂਡ',
      Frozen: 'ਫਰੋਜ਼ਨ',
      'Grocery & Staples': 'ਕਰਿਆਨਾ ਤੇ ਰਾਸ਼ਨ',
      Dairy: 'ਡੇਅਰੀ',
      Snacks: 'ਸਨੈਕਸ',
      Beverages: 'ਪੀਣ ਵਾਲੇ ਪਦਾਰਥ',
      'Personal Care': 'ਨਿੱਜੀ ਸੰਭਾਲ',
      'Household Cleaning': 'ਘਰੇਲੂ ਸਫ਼ਾਈ',
      'Audio & Headphones': 'ਆਡੀਓ ਤੇ ਹੈੱਡਫੋਨ',
      'Power & Charging': 'ਪਾਵਰ ਬੈਂਕ ਤੇ ਚਾਰਜਿੰਗ',
      'Home Lighting': 'LED ਲਾਈਟਿੰਗ',
      'Kitchen Appliances': 'ਰਸੋਈ ਉਪਕਰਣ',
      'Home Appliances': 'ਘਰੇਲੂ ਉਪਕਰਣ',
      'Kitchen Storage': 'ਰਸੋਈ ਸਟੋਰੇਜ',
      Wearables: 'ਸਮਾਰਟਵਾਚ',
      'Menswear & Kurtas': 'ਕੁਰਤੇ ਤੇ ਪੁਰਸ਼ ਕੱਪੜੇ',
      'Casual Apparel': 'ਕੈਜ਼ੂਅਲ ਕੱਪੜੇ',
      'General Retail & Travel': 'ਬੈਗ ਤੇ ਯਾਤਰਾ ਸਮਾਨ',
      'General Retail & Home Utility': 'ਟਾਰਚ ਤੇ ਘਰੇਲੂ ਸਮਾਨ',
      'General Retail & Stationery': 'ਕਾਪੀਆਂ ਤੇ ਸਟੇਸ਼ਨਰੀ',
    },
    catalogSearchPlaceholder: 'ਨਾਮ, ਬ੍ਰਾਂਡ, ਬਾਰਕੋਡ ਜਾਂ SKU (ਜਿਵੇਂ NAV081) ਨਾਲ ਖੋਜੋ...',
    sortLabel: 'ਕ੍ਰਮ:',
    sortPopular: 'ਪ੍ਰਸਿੱਧ (SKU ਅਨੁਸਾਰ)',
    sortPriceAsc: 'ਕੀਮਤ: ਘੱਟ ਤੋਂ ਵੱਧ',
    sortPriceDesc: 'ਕੀਮਤ: ਵੱਧ ਤੋਂ ਘੱਟ',
    sortSavings: 'ਵੱਧ ਤੋਂ ਵੱਧ ਬਚਤ (₹)',
    noFavoritesTitle: 'ਤੁਹਾਡੀ ਵਿਸ਼ਲਿਸਟ ਖਾਲੀ ਹੈ',
    noFavoritesDesc: 'ਜਲਦੀ ਦੁਬਾਰਾ ਆਰਡਰ ਕਰਨ ਲਈ ਕਿਸੇ ਵੀ ਪ੍ਰੋਡਕਟ ’ਤੇ ਦਿਲ ਵਾਲੇ ਬਟਨ ਨੂੰ ਦਬਾਓ।',
    noMatchTitle: 'ਕੋਈ ਮਿਲਦੀ-ਜੁਲਦੀ ਵਸਤੂ ਨਹੀਂ ਮਿਲੀ',
    noMatchDesc: 'ਕਿਰਪਾ ਕਰਕੇ ਖੋਜ ਸਾਫ਼ ਕਰੋ ਜਾਂ ਸਾਰਾ ਸਟੋਰ ਚੁਣੋ।',
    showAllBtn: 'ਸਾਰੀਆਂ ਵਸਤੂਆਂ ਵੇਖੋ',
    loadMorePrefix: 'ਅਗਲੀਆਂ 24 ਚੀਜ਼ਾਂ ਲੋਡ ਕਰੋ',
    shownSuffix: 'ਦਿਖਾਈਆਂ ਗਈਆਂ',
    addBtn: 'ਸ਼ਾਮਲ ਕਰੋ',
    outOfStock: 'ਸਟਾਕ ਖਤਮ',
    savePrefix: 'ਬਚਤ',
    quickViewLabel: 'ਵੇਰਵਾ ਵੇਖੋ',
    showcaseEyebrow: 'ਮੇਨ ਬਾਜ਼ਾਰ · ਸੰਧੌੜ · ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ · ਪੰਜਾਬ 148020',
    showcaseHeading: 'ਭਰੋਸੇਮੰਦ ਲੋਕਲ ਸੁਪਰਮਾਰਕੀਟ ਅਤੇ ਨਵ ਕਾਰਟ ਹੱਬ',
    showcaseDesc:
      'ਹਰ ਉਤਪਾਦ ਫੈਕਟਰੀ ਤੋਂ ਸੀਲਬੰਦ ਅਤੇ ਮੇਨ ਬਾਜ਼ਾਰ ਵਿੱਚ ਸਾਫ਼-ਸੁਥਰੇ ਢੰਗ ਨਾਲ ਰੱਖਿਆ ਜਾਂਦਾ ਹੈ — MRP ਤੋਂ 12% ਬਚਤ, 15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ, 2-ਘੰਟੇ ਡਿਲੀਵਰੀ ਅਤੇ 30-ਮਿੰਟ ਸਟੋਰ ਪਿਕਅੱਪ ਸਮੇਤ।',
    showcasePolicy: 'ਤੁਹਾਡੀ ਸੁਰੱਖਿਆ: 100% ਸੀਲਬੰਦ ਅਤੇ -18°C ਫਰੋਜ਼ਨ',
    vcardEyebrow: 'ਅਧਿਕਾਰਤ ਡਿਜੀਟਲ ਵਿਜ਼ਿਟਿੰਗ ਕਾਰਡ',
    vcardTagline: '100% ਸੀਲਬੰਦ ਕਰਿਆਨਾ, ਫਰੋਜ਼ਨ ਫੂਡ, ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਅਤੇ ਕੱਪੜੇ · MRP ’ਤੇ 12% ਛੋਟ',
    vcardActionsTitle: 'ਤੁਰੰਤ ਗਾਹਕ ਸੇਵਾਵਾਂ',
    vcardCallNow: 'ਹੁਣੇ ਕਾਲ ਕਰੋ',
    vcardWhatsAppChat: 'ਵਟਸਐਪ ਚੈਟ',
    vcardSaveContact: 'ਸੰਪਰਕ ਸੇਵ (vCard)',
    vcardShareCard: 'ਕਾਰਡ ਸਾਂਝਾ ਕਰੋ',
    vcardCopied: 'ਵੇਰਵੇ ਕਾਪੀ ਹੋਏ',
    vcardHoursNote:
      'ਸਮਾਂ: ਰੋਜ਼ਾਨਾ ਸਵੇਰੇ 8:00 – ਰਾਤ 9:30 ਵਜੇ · ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ · ₹499 ਤੋਂ ਵੱਧ ਆਰਡਰ ’ਤੇ ਮੁਫ਼ਤ ਡਿਲੀਵਰੀ ਜਾਂ 30-ਮਿੰਟ ਪਿਕਅੱਪ।',
    footerTagline: 'ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਤੇ ਨਵ ਕਾਰਟ · ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ 148020',
    footerCareTitle: 'ਹੈਲਪਲਾਈਨ ਅਤੇ ਗਾਹਕ ਸੇਵਾ',
    footerTrackOrder: 'ਆਰਡਰ ਸਟੇਟਸ ਟ੍ਰੈਕ ਕਰੋ',
    footerConnectTitle: 'ਨਵ ਗ੍ਰੋਸਰੀ ਅਤੇ ਨਵ ਕਾਰਟ ਨਾਲ ਜੁੜੋ',
    footerWhatsAppHelp: 'ਵਟਸਐਪ ਆਰਡਰ ਮਦਦ',
    footerDeliveryNote:
      'ਸਮਾਂ: 8:00 AM – 9:30 PM · ਸੰਧੌੜ 148020 ਵਿੱਚ 15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ ਤੇ 2-ਘੰਟੇ ਡਿਲੀਵਰੀ · 30-ਮਿੰਟ ਪਿਕਅੱਪ',
    trackerTitle: 'ਆਰਡਰ ਸਟੇਟਸ ਟ੍ਰੈਕਰ',
    trackerSubtitle: 'ਲਾਈਵ ਡਿਲੀਵਰੀ ਅਤੇ 30-ਮਿੰਟ ਪਿਕਅੱਪ ਜਾਣਕਾਰੀ · ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ',
    trackerInputLabel: 'ਆਪਣਾ ਆਰਡਰ ID ਦਰਜ ਕਰੋ',
    trackerSubmit: 'ਟ੍ਰੈਕ ਕਰੋ',
    trackerChecking: 'ਜਾਂਚ ਜਾਰੀ...',
    trackerRecent: 'ਇਸ ਡਿਵਾਈਸ ’ਤੇ ਹਾਲੀਆ ਆਰਡਰ:',
    trackerEmptyTitle: 'ਆਪਣੀ ਡਿਲੀਵਰੀ ਜਾਂ ਸਟੋਰ ਪਿਕਅੱਪ ਦੀ ਸਥਿਤੀ ਵੇਖੋ',
    trackerEmptyDesc:
      'ਲਾਈਵ ਤਿਆਰੀ, ਡਿਸਪੈਚ ਅਤੇ ਪਿਕਅੱਪ ਦੀ ਜਾਣਕਾਰੀ ਵੇਖਣ ਲਈ ਉੱਪਰ ਆਪਣੇ ਡਿਜੀਟਲ ਬਿੱਲ ਤੋਂ NGS-... ਆਰਡਰ ID ਦਰਜ ਕਰੋ।',
    quickViewAddBtn: 'ਟੋਕਰੀ ਵਿੱਚ ਪਾਓ',
    quickViewIngredients: 'ਸਮੱਗਰੀ / ਵੇਰਵਾ',
    quickViewComposition: 'ਮੁੱਖ ਵੇਰਵਾ',
    quickViewShelfLife: 'ਸ਼ੈਲਫ ਲਾਈਫ / ਵਾਰੰਟੀ ਤੇ ਸਟੋਰੇਜ',
    quickViewAllergens: 'ਐਲਰਜੀ ਅਤੇ ਸੁਰੱਖਿਆ ਚੇਤਾਵਨੀ',
    quickViewShare: 'ਸਾਂਝਾ ਕਰੋ',
    quickViewLinkCopied: 'ਲਿੰਕ ਕਾਪੀ ਹੋਇਆ!',
    bulkCalcTitle: 'ਥੋਕ ਕੀਮਤ ਅਤੇ ਬਚਤ ਕੈਲਕੁਲੇਟਰ',
    bulkQtyLabel: 'ਮਾਤਰਾ',
    bulkTotalLabel: 'ਥੋਕ ਕੁੱਲ',
    bulkMrpLabel: 'ਕੁੱਲ MRP',
    bulkSavingsLabel: 'ਤੁਹਾਡੀ ਕੁੱਲ ਬਚਤ',
    bulkAddCta: 'ਸ਼ਾਮਲ ਕਰੋ',
    bulkPacksUnit: 'ਪੈਕ',
    bulkFreeDelivery: 'ਸੰਧੌੜ ਵਿੱਚ ਮੁਫ਼ਤ ਡਿਲੀਵਰੀ',
    aisles: {
      all: { label: 'ਸਾਰੀਆਂ ਵਸਤੂਆਂ', shortLabel: 'ਸਭ ਚੀਜ਼ਾਂ', description: 'ਪੂਰਾ ਸੀਲਬੰਦ ਕੈਟਾਲਾਗ' },
      deals: { label: '12% ਛੋਟ ਵਾਲੇ ਸੌਦੇ', shortLabel: '12% ਛੋਟ', description: 'MRP ’ਤੇ ਥੋਕ ਬਚਤ' },
      frozen: { label: 'ਫਰੋਜ਼ਨ ਸਬਜ਼ੀਆਂ ਤੇ ਸਨੈਕਸ (-18°C)', shortLabel: 'ਫਰੋਜ਼ਨ -18°C', description: 'Safal, McCain ਅਤੇ Verka' },
      atta: { label: 'ਆਟਾ, ਚਾਵਲ ਅਤੇ ਦਾਲਾਂ', shortLabel: 'ਆਟਾ ਤੇ ਦਾਲ', description: 'Aashirvaad, India Gate ਤੇ Fortune' },
      ghee: { label: 'ਦੇਸੀ ਘਿਓ ਅਤੇ ਤੇਲ', shortLabel: 'ਘਿਓ ਤੇ ਤੇਲ', description: 'Verka, Amul, Fortune ਤੇ ਸਰ੍ਹੋਂ ਦਾ ਤੇਲ' },
      spices: { label: 'ਮਸਾਲੇ ਅਤੇ ਨਮਕ', shortLabel: 'ਮਸਾਲੇ', description: 'MDH, Catch, Everest ਤੇ Tata Salt' },
      beverages: { label: 'ਚਾਹ, ਪੀਣ ਵਾਲੇ ਪਦਾਰਥ ਤੇ ਬਿਸਕੁਟ', shortLabel: 'ਚਾਹ ਤੇ ਸਨੈਕਸ', description: 'Tata Tea, Red Label, Maggi ਤੇ ਬਿਸਕੁਟ' },
      household: { label: 'ਘਰੇਲੂ ਅਤੇ ਨਿੱਜੀ ਸੰਭਾਲ', shortLabel: 'ਘਰੇਲੂ ਸਮਾਨ', description: 'Surf Excel, Ariel, Vim, Dettol ਤੇ Dove' },
      electronics: { label: 'ਇਲੈਕਟ੍ਰਾਨਿਕਸ ਤੇ ਉਪਕਰਣ', shortLabel: 'ਇਲੈਕਟ੍ਰਾਨਿਕਸ', description: 'ਹੈੱਡਫੋਨ, LED ਬਲਬ, ਕੇਤਲੀ ਤੇ ਪ੍ਰੈੱਸ' },
      apparel: { label: 'ਕੱਪੜੇ ਤੇ ਫੈਸ਼ਨ', shortLabel: 'ਕੱਪੜੇ', description: 'ਕੁਰਤੇ, ਟੀ-ਸ਼ਰਟਾਂ ਤੇ ਟ੍ਰੈਕਪੈਂਟ' },
      retail: { label: 'ਜਨਰਲ ਰਿਟੇਲ ਸਮਾਨ', shortLabel: 'ਜਨਰਲ ਰਿਟੇਲ', description: 'ਬੈਗ, ਟਾਰਚ ਤੇ ਸਟੇਸ਼ਨਰੀ' },
    },
  },
};

export function getStoreTranslations(lang: StoreLang): StoreTranslations {
  return TRANSLATIONS[lang] || TRANSLATIONS.en;
}
