export type StoreLang = 'en' | 'pa' | 'hi';

export interface StoreTranslations {
  storeName: string;
  headerLocation: string;
  fullAddress: string;
  announcementFallback: string;
  helplineLabel: string;
  searchPlaceholder: string;
  clearLabel: string;
  deliveryMode: string;
  pickupMode: string;
  favoritesLabel: string;
  trackOrderLabel: string;
  whatsappLoginLabel: string;
  basketLabel: string;
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
  showingLabel: string;
  ofLabel: string;
  verifiedItemsLabel: string;
  resetFiltersLabel: string;
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
    storeName: 'Nav Grocery Store',
    headerLocation: 'Main Bazaar, Sandhaur · 148020',
    fullAddress: 'Main Bazaar, Sandhaur, Distt. Malerkotla, Punjab 148020',
    announcementFallback:
      'Flat 12% Off MRP on all 308 Grocery Items · 2-Hr Delivery in Sandhaur 148020 · 30-Min Main Bazaar Pickup',
    helplineLabel: 'Helpline:',
    searchPlaceholder: 'Search 308 groceries: Aashirvaad, India Gate, Verka, Amul, McCain...',
    clearLabel: 'Clear',
    deliveryMode: '2-Hr Delivery',
    pickupMode: '30-Min Pickup',
    favoritesLabel: 'Favorites',
    trackOrderLabel: 'Track Order',
    whatsappLoginLabel: 'WhatsApp Login',
    basketLabel: 'Basket',
    heroEyebrow: 'Nav Grocery Store · Main Bazaar, Sandhaur 148020',
    heroTitle: 'Flat 12% Wholesale Discount on All 308 Verified Groceries',
    heroSubtitle:
      '100% factory-sealed pantry staples, pure cow desi ghee, masala spices & -18°C cold-chain frozen essentials.',
    heroDeliveryBadge: '2-Hour Local Delivery in Sandhaur 148020',
    heroPickupBadge: '30-Min Express Store Pickup',
    heroShopAllCta: 'Shop All 308 Items',
    heroWhatsAppCta: 'Order on WhatsApp',
    aislesHeading: 'Shop by Grocery Aisle',
    aislesSubnote: 'Strictly 308 Sealed & -18°C Frozen Items',
    deptAll: 'All Store Items',
    deptPackaged: 'Packaged Groceries',
    deptFrozen: '-18°C Frozen Foods',
    showingLabel: 'Showing',
    ofLabel: 'of',
    verifiedItemsLabel: 'verified grocery items',
    resetFiltersLabel: 'Reset Filters',
    catalogSearchPlaceholder: 'Filter by product name or SKU (e.g. NAV081)...',
    sortLabel: 'Sort:',
    sortPopular: 'Popular (SKU Order)',
    sortPriceAsc: 'Price: Low to High',
    sortPriceDesc: 'Price: High to Low',
    sortSavings: 'Max Savings (₹)',
    noFavoritesTitle: 'No favorite items saved yet',
    noFavoritesDesc: 'Tap the heart button on any product card to bookmark items for quick re-ordering.',
    noMatchTitle: 'No matching grocery items found',
    noMatchDesc: 'Try clearing your search or switching back to All Store Items.',
    showAllBtn: 'Show All 308 Products',
    loadMorePrefix: 'Load Next 24 Items',
    shownSuffix: 'shown',
    addBtn: 'Add',
    outOfStock: 'Out of Stock',
    savePrefix: 'Save',
    quickViewLabel: 'Quick View',
    showcaseEyebrow: 'Main Bazaar · Sandhaur · Distt. Malerkotla · Punjab 148020',
    showcaseHeading: 'Trusted Local Supermarket & -18°C Cold-Chain Store',
    showcaseDesc:
      'Every product is factory-sealed and stocked cleanly on our Main Bazaar shelves—offered at flat 12% wholesale savings off MRP with 2-hour local delivery and 30-minute store pickup.',
    showcasePolicy: 'Zero Fresh Produce Policy · 100% Sealed & Frozen',
    vcardEyebrow: 'Official Digital Visiting Card',
    vcardTagline: '100% Sealed Branded Groceries & Cold-Chain Frozen Foods · Flat 12% Off MRP',
    vcardActionsTitle: 'Instant Customer Actions',
    vcardCallNow: 'Call Now',
    vcardWhatsAppChat: 'WhatsApp Chat',
    vcardSaveContact: 'Save Contact (vCard)',
    vcardShareCard: 'Share Store Card',
    vcardCopied: 'Details Copied',
    vcardHoursNote:
      'Open daily at Main Bazaar, Sandhaur · Free 2-hour local delivery on orders above ₹499 or 30-min express pickup.',
    footerTagline: 'Wholesale Kirana & Cold-Chain Frozen Store · Main Bazaar, Sandhaur 148020',
    footerCareTitle: 'Helpline & Customer Care',
    footerTrackOrder: 'Track Order Status',
    footerConnectTitle: 'Connect With Nav Grocery Store',
    footerWhatsAppHelp: 'WhatsApp Order Help',
    footerDeliveryNote: '2-Hour Local Delivery in Sandhaur 148020 · 30-Min Main Bazaar Store Pickup',
    trackerTitle: 'Order Status Tracker',
    trackerSubtitle: 'Live delivery & 30-minute pickup tracking · Nav Grocery Store',
    trackerInputLabel: 'Enter Your Order ID',
    trackerSubmit: 'Track Order',
    trackerChecking: 'Checking...',
    trackerRecent: 'Recent orders placed on this device:',
    trackerEmptyTitle: 'Check Your Delivery or Store Pickup Status',
    trackerEmptyDesc:
      'Enter your NGS-... Order ID from your digital bill above to inspect live preparation, dispatch, and pickup readiness.',
    quickViewAddBtn: 'Add to Basket',
    quickViewIngredients: 'Ingredients',
    quickViewComposition: 'Key Composition',
    quickViewShelfLife: 'Shelf Life & Storage',
    quickViewAllergens: 'Allergen & Safety Warnings',
    quickViewShare: 'Share',
    quickViewLinkCopied: 'Link Copied!',
    bulkCalcTitle: 'Bulk Price & Savings Calculator',
    bulkQtyLabel: 'Qty',
    bulkTotalLabel: 'Wholesale Total',
    bulkMrpLabel: 'Total MRP',
    bulkSavingsLabel: 'Your Total Savings',
    bulkAddCta: 'Add',
    bulkPacksUnit: 'packs',
    bulkFreeDelivery: 'Free 2-Hr Sandhaur Delivery Unlocked',
    aisles: {
      all: { label: 'All Store Items', shortLabel: 'All 308', description: 'Complete sealed grocery catalog' },
      deals: { label: '12% Off Deals', shortLabel: '12% Deals', description: 'Wholesale savings off MRP' },
      frozen: { label: 'Frozen Veg & Snacks (-18°C)', shortLabel: 'Frozen -18°C', description: 'Safal, McCain & Verka cold chain' },
      atta: { label: 'Atta, Rice & Dal', shortLabel: 'Atta & Dal', description: 'Aashirvaad, India Gate & Fortune' },
      ghee: { label: 'Desi Ghee & Oils', shortLabel: 'Ghee & Oils', description: 'Verka, Amul, Fortune & Mustard Oil' },
      spices: { label: 'Spices & Masale', shortLabel: 'Spices', description: 'MDH, Catch, Everest & Tata Salt' },
      beverages: { label: 'Tea, Beverages & Biscuits', shortLabel: 'Tea & Snacks', description: 'Tata Tea, Red Label, Maggi & Biscuits' },
      household: { label: 'Household & Personal Care', shortLabel: 'Household', description: 'Surf Excel, Ariel, Vim, Dettol & Dove' },
    },
  },
  pa: {
    storeName: 'ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ',
    headerLocation: 'ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ · 148020',
    fullAddress: 'ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ, ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ, ਪੰਜਾਬ 148020',
    announcementFallback:
      'ਸਾਰੀਆਂ 308 ਕਰਿਆਨਾ ਵਸਤੂਆਂ ’ਤੇ MRP ਤੋਂ 12% ਫਲੈਟ ਛੋਟ · ਸੰਧੌੜ 148020 ਵਿੱਚ 2 ਘੰਟੇ ਡਿਲੀਵਰੀ · 30 ਮਿੰਟ ਮੇਨ ਬਾਜ਼ਾਰ ਪਿਕਅੱਪ',
    helplineLabel: 'ਹੈਲਪਲਾਈਨ:',
    searchPlaceholder: '308 ਕਰਿਆਨਾ ਚੀਜ਼ਾਂ ਖੋਜੋ: Aashirvaad, India Gate, Verka, Amul...',
    clearLabel: 'ਸਾਫ਼ ਕਰੋ',
    deliveryMode: '2-ਘੰਟੇ ਡਿਲੀਵਰੀ',
    pickupMode: '30-ਮਿੰਟ ਪਿਕਅੱਪ',
    favoritesLabel: 'ਪਸੰਦੀਦਾ',
    trackOrderLabel: 'ਆਰਡਰ ਟ੍ਰੈਕ',
    whatsappLoginLabel: 'ਵਟਸਐਪ ਲਾਗਇਨ',
    basketLabel: 'ਟੋਕਰੀ',
    heroEyebrow: 'ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ · ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ 148020',
    heroTitle: 'ਸਾਰੀਆਂ 308 ਪ੍ਰਮਾਣਿਤ ਕਰਿਆਨਾ ਚੀਜ਼ਾਂ ’ਤੇ 12% ਥੋਕ ਛੋਟ',
    heroSubtitle:
      '100% ਫੈਕਟਰੀ-ਸੀਲਬੰਦ ਰਾਸ਼ਨ, ਸ਼ੁੱਧ ਗਾਂ ਦਾ ਦੇਸੀ ਘਿਓ, ਮਸਾਲੇ ਅਤੇ -18°C ਕੋਲਡ-ਚੇਨ ਫਰੋਜ਼ਨ ਸਮਾਨ।',
    heroDeliveryBadge: 'ਸੰਧੌੜ 148020 ਵਿੱਚ 2-ਘੰਟੇ ਲੋਕਲ ਡਿਲੀਵਰੀ',
    heroPickupBadge: '30-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ ਸਟੋਰ ਪਿਕਅੱਪ',
    heroShopAllCta: 'ਸਾਰੀਆਂ 308 ਚੀਜ਼ਾਂ ਵੇਖੋ',
    heroWhatsAppCta: 'ਵਟਸਐਪ ’ਤੇ ਆਰਡਰ ਕਰੋ',
    aislesHeading: 'ਕਰਿਆਨਾ ਸ਼੍ਰੇਣੀ ਅਨੁਸਾਰ ਖਰੀਦੋ',
    aislesSubnote: 'ਕੇਵਲ 308 ਸੀਲਬੰਦ ਅਤੇ -18°C ਫਰੋਜ਼ਨ ਵਸਤੂਆਂ',
    deptAll: 'ਸਾਰਾ ਸਟੋਰ',
    deptPackaged: 'ਪੈਕਡ ਕਰਿਆਨਾ',
    deptFrozen: '-18°C ਫਰੋਜ਼ਨ ਫੂਡ',
    showingLabel: 'ਦਿਖਾ ਰਹੇ ਹਾਂ',
    ofLabel: 'ਵਿੱਚੋਂ',
    verifiedItemsLabel: 'ਪ੍ਰਮਾਣਿਤ ਕਰਿਆਨਾ ਚੀਜ਼ਾਂ',
    resetFiltersLabel: 'ਫਿਲਟਰ ਹਟਾਓ',
    catalogSearchPlaceholder: 'ਨਾਮ ਜਾਂ SKU (ਜਿਵੇਂ NAV081) ਨਾਲ ਖੋਜੋ...',
    sortLabel: 'ਕ੍ਰਮ:',
    sortPopular: 'ਪ੍ਰਸਿੱਧ (SKU ਅਨੁਸਾਰ)',
    sortPriceAsc: 'ਕੀਮਤ: ਘੱਟ ਤੋਂ ਵੱਧ',
    sortPriceDesc: 'ਕੀਮਤ: ਵੱਧ ਤੋਂ ਘੱਟ',
    sortSavings: 'ਵੱਧ ਤੋਂ ਵੱਧ ਬਚਤ (₹)',
    noFavoritesTitle: 'ਹਾਲੇ ਕੋਈ ਪਸੰਦੀਦਾ ਚੀਜ਼ ਸੇਵ ਨਹੀਂ ਹੈ',
    noFavoritesDesc: 'ਜਲਦੀ ਦੁਬਾਰਾ ਆਰਡਰ ਕਰਨ ਲਈ ਕਿਸੇ ਵੀ ਪ੍ਰੋਡਕਟ ’ਤੇ ਦਿਲ ਵਾਲੇ ਬਟਨ ਨੂੰ ਦਬਾਓ।',
    noMatchTitle: 'ਕੋਈ ਮਿਲਦੀ-ਜੁਲਦੀ ਕਰਿਆਨਾ ਵਸਤੂ ਨਹੀਂ ਮਿਲੀ',
    noMatchDesc: 'ਕਿਰਪਾ ਕਰਕੇ ਖੋਜ ਸਾਫ਼ ਕਰੋ ਜਾਂ ਸਾਰਾ ਸਟੋਰ ਚੁਣੋ।',
    showAllBtn: 'ਸਾਰੀਆਂ 308 ਚੀਜ਼ਾਂ ਵੇਖੋ',
    loadMorePrefix: 'ਅਗਲੀਆਂ 24 ਚੀਜ਼ਾਂ ਲੋਡ ਕਰੋ',
    shownSuffix: 'ਦਿਖਾਈਆਂ ਗਈਆਂ',
    addBtn: 'ਸ਼ਾਮਲ ਕਰੋ',
    outOfStock: 'ਸਟਾਕ ਖਤਮ',
    savePrefix: 'ਬਚਤ',
    quickViewLabel: 'ਵੇਰਵਾ ਵੇਖੋ',
    showcaseEyebrow: 'ਮੇਨ ਬਾਜ਼ਾਰ · ਸੰਧੌੜ · ਜ਼ਿਲ੍ਹਾ ਮਲੇਰਕੋਟਲਾ · ਪੰਜਾਬ 148020',
    showcaseHeading: 'ਭਰੋਸੇਮੰਦ ਲੋਕਲ ਸੁਪਰਮਾਰਕੀਟ ਅਤੇ -18°C ਕੋਲਡ-ਚੇਨ ਸਟੋਰ',
    showcaseDesc:
      'ਹਰ ਉਤਪਾਦ ਫੈਕਟਰੀ ਤੋਂ ਸੀਲਬੰਦ ਅਤੇ ਮੇਨ ਬਾਜ਼ਾਰ ਵਿੱਚ ਸਾਫ਼-ਸੁਥਰੇ ਢੰਗ ਨਾਲ ਰੱਖਿਆ ਜਾਂਦਾ ਹੈ — MRP ਤੋਂ 12% ਬਚਤ, 2-ਘੰਟੇ ਡਿਲੀਵਰੀ ਅਤੇ 30-ਮਿੰਟ ਸਟੋਰ ਪਿਕਅੱਪ ਸਮੇਤ।',
    showcasePolicy: 'ਤੁਹਾਡੀ ਸੁਰੱਖਿਆ: 100% ਸੀਲਬੰਦ ਅਤੇ ਫਰੋਜ਼ਨ',
    vcardEyebrow: 'ਅਧਿਕਾਰਤ ਡਿਜੀਟਲ ਵਿਜ਼ਿਟਿੰਗ ਕਾਰਡ',
    vcardTagline: '100% ਸੀਲਬੰਦ ਬ੍ਰਾਂਡਡ ਕਰਿਆਨਾ ਅਤੇ ਕੋਲਡ-ਚੇਨ ਫਰੋਜ਼ਨ ਫੂਡ · MRP ’ਤੇ 12% ਛੋਟ',
    vcardActionsTitle: 'ਤੁਰੰਤ ਗਾਹਕ ਸੇਵਾਵਾਂ',
    vcardCallNow: 'ਹੁਣੇ ਕਾਲ ਕਰੋ',
    vcardWhatsAppChat: 'ਵਟਸਐਪ ਚੈਟ',
    vcardSaveContact: 'ਸੰਪਰਕ ਸੇਵ (vCard)',
    vcardShareCard: 'ਕਾਰਡ ਸਾਂਝਾ ਕਰੋ',
    vcardCopied: 'ਵੇਰਵੇ ਕਾਪੀ ਹੋਏ',
    vcardHoursNote:
      'ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ ਵਿਖੇ ਰੋਜ਼ਾਨਾ ਖੁੱਲ੍ਹਾ · ₹499 ਤੋਂ ਵੱਧ ਆਰਡਰ ’ਤੇ ਮੁਫ਼ਤ 2-ਘੰਟੇ ਡਿਲੀਵਰੀ ਜਾਂ 30-ਮਿੰਟ ਪਿਕਅੱਪ।',
    footerTagline: 'ਥੋਕ ਕਰਿਆਨਾ ਅਤੇ ਕੋਲਡ-ਚੇਨ ਫਰੋਜ਼ਨ ਸਟੋਰ · ਮੇਨ ਬਾਜ਼ਾਰ, ਸੰਧੌੜ 148020',
    footerCareTitle: 'ਹੈਲਪਲਾਈਨ ਅਤੇ ਗਾਹਕ ਸੇਵਾ',
    footerTrackOrder: 'ਆਰਡਰ ਸਟੇਟਸ ਟ੍ਰੈਕ ਕਰੋ',
    footerConnectTitle: 'ਨਵ ਗ੍ਰੋਸਰੀ ਸਟੋਰ ਨਾਲ ਜੁੜੋ',
    footerWhatsAppHelp: 'ਵਟਸਐਪ ਆਰਡਰ ਮਦਦ',
    footerDeliveryNote: 'ਸੰਧੌੜ 148020 ਵਿੱਚ 2-ਘੰਟੇ ਲੋਕਲ ਡਿਲੀਵਰੀ · 30-ਮਿੰਟ ਮੇਨ ਬਾਜ਼ਾਰ ਪਿਕਅੱਪ',
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
    quickViewIngredients: 'ਸਮੱਗਰੀ (Ingredients)',
    quickViewComposition: 'ਮੁੱਖ ਤੱਤ (Composition)',
    quickViewShelfLife: 'ਸ਼ੈਲਫ ਲਾਈਫ ਅਤੇ ਸਟੋਰੇਜ',
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
    bulkFreeDelivery: 'ਸੰਧੌੜ ਵਿੱਚ ਮੁਫ਼ਤ 2-ਘੰਟੇ ਡਿਲੀਵਰੀ',
    aisles: {
      all: { label: 'ਸਾਰੀਆਂ ਵਸਤੂਆਂ', shortLabel: 'ਸਭ 308', description: 'ਪੂਰਾ ਸੀਲਬੰਦ ਕਰਿਆਨਾ ਕੈਟਾਲਾਗ' },
      deals: { label: '12% ਛੋਟ ਵਾਲੇ ਸੌਦੇ', shortLabel: '12% ਛੋਟ', description: 'MRP ’ਤੇ ਥੋਕ ਬਚਤ' },
      frozen: { label: 'ਫਰੋਜ਼ਨ ਸਬਜ਼ੀਆਂ ਤੇ ਸਨੈਕਸ (-18°C)', shortLabel: 'ਫਰੋਜ਼ਨ -18°C', description: 'Safal, McCain ਅਤੇ Verka' },
      atta: { label: 'ਆਟਾ, ਚਾਵਲ ਅਤੇ ਦਾਲਾਂ', shortLabel: 'ਆਟਾ ਤੇ ਦਾਲ', description: 'Aashirvaad, India Gate ਤੇ Fortune' },
      ghee: { label: 'ਦੇਸੀ ਘਿਓ ਅਤੇ ਤੇਲ', shortLabel: 'ਘਿਓ ਤੇ ਤੇਲ', description: 'Verka, Amul, Fortune ਤੇ ਸਰ੍ਹੋਂ ਦਾ ਤੇਲ' },
      spices: { label: 'ਮਸਾਲੇ ਅਤੇ ਨਮਕ', shortLabel: 'ਮਸਾਲੇ', description: 'MDH, Catch, Everest ਤੇ Tata Salt' },
      beverages: { label: 'ਚਾਹ, ਪੀਣ ਵਾਲੇ ਪਦਾਰਥ ਤੇ ਬਿਸਕੁਟ', shortLabel: 'ਚਾਹ ਤੇ ਸਨੈਕਸ', description: 'Tata Tea, Red Label, Maggi ਤੇ ਬਿਸਕੁਟ' },
      household: { label: 'ਘਰੇਲੂ ਅਤੇ ਨਿੱਜੀ ਸੰਭਾਲ', shortLabel: 'ਘਰੇਲੂ ਸਮਾਨ', description: 'Surf Excel, Ariel, Vim, Dettol ਤੇ Dove' },
    },
  },
  hi: {
    storeName: 'नव ग्रोसरी स्टोर',
    headerLocation: 'मेन बाजार, संधौर · 148020',
    fullAddress: 'मेन बाजार, संधौर, जिला मलेरकोटला, पंजाब 148020',
    announcementFallback:
      'सभी 308 किराना वस्तुओं पर MRP से 12% फ्लैट छूट · संधौर 148020 में 2 घंटे डिलीवरी · 30 मिनट मेन बाजार पिकअप',
    helplineLabel: 'हेल्पलाइन:',
    searchPlaceholder: '308 किराना आइटम खोजें: Aashirvaad, India Gate, Verka, Amul...',
    clearLabel: 'साफ करें',
    deliveryMode: '2-घंटे डिलीवरी',
    pickupMode: '30-मिनट पिकअप',
    favoritesLabel: 'पसंदीदा',
    trackOrderLabel: 'ऑर्डर ट्रैक',
    whatsappLoginLabel: 'व्हाट्सएप लॉगिन',
    basketLabel: 'बास्केट',
    heroEyebrow: 'नव ग्रोसरी स्टोर · मेन बाजार, संधौर 148020',
    heroTitle: 'सभी 308 प्रमाणित किराना उत्पादों पर 12% थोक छूट',
    heroSubtitle:
      '100% फैक्ट्री-सीलबंद राशन, शुद्ध गाय का देसी घी, मसाले और -18°C कोल्ड-चेन फ्रोजन उत्पाद।',
    heroDeliveryBadge: 'संधौर 148020 में 2 घंटे लोकल डिलीवरी',
    heroPickupBadge: '30 मिनट एक्सप्रेस स्टोर पिकअप',
    heroShopAllCta: 'सभी 308 आइटम देखें',
    heroWhatsAppCta: 'व्हाट्सएप पर ऑर्डर करें',
    aislesHeading: 'किराना सेक्शन के अनुसार खरीदें',
    aislesSubnote: 'केवल 308 सीलबंद एवं -18°C फ्रोजन आइटम',
    deptAll: 'सभी आइटम',
    deptPackaged: 'पैक्ड किराना',
    deptFrozen: '-18°C फ्रोजन फूड',
    showingLabel: 'दिखा रहे हैं',
    ofLabel: 'में से',
    verifiedItemsLabel: 'प्रमाणित किराना आइटम',
    resetFiltersLabel: 'फ़िल्टर रीसेट करें',
    catalogSearchPlaceholder: 'उत्पाद का नाम या SKU (जैसे NAV081) से फ़िल्टर करें...',
    sortLabel: 'क्रम:',
    sortPopular: 'लोकप्रिय (SKU क्रम)',
    sortPriceAsc: 'कीमत: कम से अधिक',
    sortPriceDesc: 'कीमत: अधिक से कम',
    sortSavings: 'अधिकतम बचत (₹)',
    noFavoritesTitle: 'अभी कोई पसंदीदा आइटम सेव नहीं है',
    noFavoritesDesc: 'जल्दी री-ऑर्डर करने के लिए किसी भी प्रोडक्ट कार्ड पर हार्ट बटन दबाएं।',
    noMatchTitle: 'कोई मिलता-जुलता किराना आइटम नहीं मिला',
    noMatchDesc: 'कृपया अपनी खोज साफ़ करें या सभी आइटम पर वापस जाएं।',
    showAllBtn: 'सभी 308 उत्पाद देखें',
    loadMorePrefix: 'अगले 24 आइटम लोड करें',
    shownSuffix: 'दिखाए गए',
    addBtn: 'जोड़ें',
    outOfStock: 'स्टॉक समाप्त',
    savePrefix: 'बचत',
    quickViewLabel: 'त्वरित विवरण',
    showcaseEyebrow: 'मेन बाजार · संधौर · जिला मलेरकोटला · पंजाब 148020',
    showcaseHeading: 'विश्वसनीय लोकल सुपरमार्केट और -18°C कोल्ड-चेन स्टोर',
    showcaseDesc:
      'हर उत्पाद फैक्ट्री से सीलबंद और हमारे मेन बाजार स्टोर में स्वच्छता से उपलब्ध है — MRP पर 12% थोक बचत, 2 घंटे डिलीवरी और 30 मिनट पिकअप के साथ।',
    showcasePolicy: 'जीरो फ्रेश प्रोड्यूस नीति · 100% सीलबंद व फ्रोजन',
    vcardEyebrow: 'आधिकारिक डिजिटल विजिटिंग कार्ड',
    vcardTagline: '100% सीलबंद ब्रांडेड किराना और कोल्ड-चेन फ्रोजन फूड · MRP पर 12% छूट',
    vcardActionsTitle: 'त्वरित ग्राहक सेवाएं',
    vcardCallNow: 'अभी कॉल करें',
    vcardWhatsAppChat: 'व्हाट्सएप चैट',
    vcardSaveContact: 'संपर्क सेव (vCard)',
    vcardShareCard: 'स्टोर कार्ड साझा करें',
    vcardCopied: 'विवरण कॉपी हो गया',
    vcardHoursNote:
      'मेन बाजार, संधौर में प्रतिदिन खुला · ₹499 से अधिक के ऑर्डर पर मुफ्त 2 घंटे डिलीवरी या 30 मिनट पिकअप।',
    footerTagline: 'थोक किराना एवं कोल्ड-चेन फ्रोजन स्टोर · मेन बाजार, संधौर 148020',
    footerCareTitle: 'हेल्पलाइन और ग्राहक सहायता',
    footerTrackOrder: 'ऑर्डर स्टेटस ट्रैक करें',
    footerConnectTitle: 'नव ग्रोसरी स्टोर से जुड़ें',
    footerWhatsAppHelp: 'व्हाट्सएप ऑर्डर सहायता',
    footerDeliveryNote: 'संधौर 148020 में 2 घंटे लोकल डिलीवरी · 30 मिनट मेन बाजार पिकअप',
    trackerTitle: 'ऑर्डर स्टेटस ट्रैकर',
    trackerSubtitle: 'लाइव डिलीवरी और 30 मिनट पिकअप ट्रैकिंग · नव ग्रोसरी स्टोर',
    trackerInputLabel: 'अपना ऑर्डर ID दर्ज करें',
    trackerSubmit: 'ट्रैक करें',
    trackerChecking: 'जांच हो रही है...',
    trackerRecent: 'इस डिवाइस पर हाल के ऑर्डर:',
    trackerEmptyTitle: 'अपनी डिलीवरी या स्टोर पिकअप की स्थिति जांचें',
    trackerEmptyDesc:
      'लाइव तैयारी, डिस्पैच और पिकअप की स्थिति देखने के लिए ऊपर अपने डिजिटल बिल से NGS-... ऑर्डर ID दर्ज करें।',
    quickViewAddBtn: 'बास्केट में जोड़ें',
    quickViewIngredients: 'सामग्री (Ingredients)',
    quickViewComposition: 'मुख्य घटक (Composition)',
    quickViewShelfLife: 'शेल्फ लाइफ और स्टोरेज',
    quickViewAllergens: 'एलर्जी और सुरक्षा चेतावनी',
    quickViewShare: 'शेयर करें',
    quickViewLinkCopied: 'लिंक कॉपी हो गया!',
    bulkCalcTitle: 'थोक मूल्य और बचत कैलकुलेटर',
    bulkQtyLabel: 'मात्रा',
    bulkTotalLabel: 'थोक कुल',
    bulkMrpLabel: 'कुल MRP',
    bulkSavingsLabel: 'आपकी कुल बचत',
    bulkAddCta: 'जोड़ें',
    bulkPacksUnit: 'पैक',
    bulkFreeDelivery: 'संधौर में मुफ्त 2-घंटे डिलीवरी',
    aisles: {
      all: { label: 'सभी स्टोर आइटम', shortLabel: 'सभी 308', description: 'संपूर्ण सीलबंद किराना कैटलॉग' },
      deals: { label: '12% छूट ऑफ़र', shortLabel: '12% छूट', description: 'MRP पर थोक बचत' },
      frozen: { label: 'फ्रोजन सब्ज़ियां व स्नैक्स (-18°C)', shortLabel: 'फ्रोजन -18°C', description: 'Safal, McCain व Verka' },
      atta: { label: 'आटा, चावल और दालें', shortLabel: 'आटा व दाल', description: 'Aashirvaad, India Gate व Fortune' },
      ghee: { label: 'देसी घी और तेल', shortLabel: 'घी व तेल', description: 'Verka, Amul, Fortune व सरसों तेल' },
      spices: { label: 'मसाले और नमक', shortLabel: 'मसाले', description: 'MDH, Catch, Everest व Tata Salt' },
      beverages: { label: 'चाय, पेय और बिस्कुट', shortLabel: 'चाय व स्नैक्स', description: 'Tata Tea, Red Label, Maggi व बिस्कुट' },
      household: { label: 'घरेलू और पर्सनल केयर', shortLabel: 'घरेलू उत्पाद', description: 'Surf Excel, Ariel, Vim, Dettol व Dove' },
    },
  },
};

export function getStoreTranslations(lang: StoreLang): StoreTranslations {
  return TRANSLATIONS[lang] || TRANSLATIONS.en;
}
