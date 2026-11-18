import { Product, ServiceVertical } from '../types';

export interface AisleDefinition {
  id: string;
  label: string;
  shortLabel: string;
  description: string;
  accentBg: string;
}

export const GROCERY_AISLES: AisleDefinition[] = [
  { id: 'all', label: 'All Store Items', shortLabel: 'All 308', description: 'Complete sealed wholesale catalog', accentBg: '#0F3D28' },
  { id: 'deals', label: '12% Off Deals', shortLabel: '12% Deals', description: 'Flat wholesale savings off MRP', accentBg: '#B45309' },
  { id: 'frozen', label: 'Frozen Veg & Snacks (-18°C)', shortLabel: 'Frozen -18°C', description: 'Safal, McCain & Verka cold chain', accentBg: '#0369A1' },
  { id: 'atta', label: 'Atta, Rice & Dal', shortLabel: 'Atta & Dal', description: 'Aashirvaad, India Gate & Fortune', accentBg: '#92400E' },
  { id: 'ghee', label: 'Desi Ghee & Oils', shortLabel: 'Ghee & Oils', description: 'Verka, Amul, Fortune & Mustard Oil', accentBg: '#A16207' },
  { id: 'spices', label: 'Spices & Masale', shortLabel: 'Spices', description: 'MDH, Catch, Everest & Tata Salt', accentBg: '#991B1B' },
  { id: 'beverages', label: 'Tea, Beverages & Biscuits', shortLabel: 'Tea & Snacks', description: 'Tata Tea, Red Label, Maggi & Biscuits', accentBg: '#7C2D12' },
  { id: 'household', label: 'Household & Personal Care', shortLabel: 'Household', description: 'Surf Excel, Ariel, Vim, Dettol & Dove', accentBg: '#1E40AF' },
  { id: 'electronics', label: 'Electronics & Appliances', shortLabel: 'Electronics', description: 'Audio, LED lighting, kitchen appliances', accentBg: '#0F3D28' },
  { id: 'apparel', label: 'Apparel & Fashion', shortLabel: 'Apparel', description: 'Cotton kurtas, casuals & trackwear', accentBg: '#14221B' },
  { id: 'retail', label: 'General Retail Goods', shortLabel: 'General Retail', description: 'Backpacks, torches, flasks & stationery', accentBg: '#0A2A1B' },
];

const HOUSEHOLD_KEYWORDS = [
  'surf', 'ariel', 'tide', 'rin', 'comfort', 'vim', 'exo', 'pril', 'harpic',
  'lizol', 'colin', 'dettol', 'savlon', 'lifebuoy', 'dove', 'lux', 'pears',
  'colgate', 'pepsodent', 'sensodyne', 'close up', 'clinic', 'pantene',
  'head & shoulders', 'sunsilk', 'parachute', 'nivea', 'vaseline', 'odonil',
  'good knight', 'mortein', 'hit', 'all out', 'whisper', 'pampers', 'huggies',
  'soap', 'shampoo', 'detergent', 'cleaner', 'dishwash', 'toothpaste', 'brush',
  'santoor', 'medimix', 'godrej', 'bajaj', 'ponds',
];

const BEVERAGES_SNACKS_KEYWORDS = [
  'tea', 'chai', 'red label', 'taj mahal', 'wagh bakri', 'coffee', 'nescafe',
  'bru', 'bournvita', 'horlicks', 'boost', 'rooh afza', 'kissan', 'maggi',
  'yippee', 'biscuit', 'parle', 'britannia', 'good day', 'marie', 'oreo',
  'hide & seek', 'haldiram', 'bikano', 'lays', 'kurkure', 'bingo', 'namkeen',
  'bhujia', 'sugar', 'madhur', 'jam', 'sauce', 'ketchup', 'chocolate', 'cadbury',
  'cornflakes', 'oats', 'honey', 'dabur', 'noodles', 'nutella', 'pintola',
  'uncle chipps', 'sunfeast', 'chings',
];

export type DepartmentFilter = 'all' | 'packaged' | 'frozen' | 'electronics' | 'apparel' | 'retail';

export function isFrozenProduct(product: Product): boolean {
  const cat = (product.category || '').toLowerCase();
  const dept = (product.department || '').toLowerCase();
  return dept.includes('frozen') || cat.includes('frozen');
}

export function isElectronicsOrHomeProduct(product: Product): boolean {
  const dept = (product.department || '').toLowerCase();
  const cat = (product.category || '').toLowerCase();
  return (
    dept.includes('electronic') ||
    dept.includes('mobile') ||
    dept.includes('home & kitchen') ||
    cat.includes('audio') ||
    cat.includes('appliance') ||
    cat.includes('lighting') ||
    cat.includes('power') ||
    cat.includes('wearable') ||
    cat.includes('kitchen storage')
  );
}

export function isApparelProduct(product: Product): boolean {
  const dept = (product.department || '').toLowerCase();
  const cat = (product.category || '').toLowerCase();
  return (
    dept.includes('clothing') ||
    dept.includes('apparel') ||
    dept.includes('footwear') ||
    cat.includes('menswear') ||
    cat.includes('kurta') ||
    cat.includes('apparel')
  );
}

export function isGeneralRetailProduct(product: Product): boolean {
  const dept = (product.department || '').toLowerCase();
  const cat = (product.category || '').toLowerCase();
  return (
    dept.includes('general retail') ||
    dept.includes('luggage') ||
    dept.includes('stationery') ||
    cat.includes('general retail') ||
    cat.includes('travel') ||
    cat.includes('stationery') ||
    cat.includes('home utility')
  );
}

export function isGroceryCoreProduct(product: Product): boolean {
  return (
    !isElectronicsOrHomeProduct(product) &&
    !isApparelProduct(product) &&
    !isGeneralRetailProduct(product)
  );
}

export function matchesServiceVertical(product: Product, vertical: ServiceVertical): boolean {
  if (!hasZeroFreshProduce(product)) return false;

  if (vertical === 'store') {
    // Store (Grocery) strictly shows the 308 verified packaged grocery & -18C frozen food SKUs
    return isGroceryCoreProduct(product) && product.channels?.navGroceryWeb !== false;
  }
  if (vertical === 'supermall') {
    // Nav Kart Supermall strictly opens electronics, apparel & general retail goods
    return (
      !isGroceryCoreProduct(product) &&
      (isElectronicsOrHomeProduct(product) ||
        isApparelProduct(product) ||
        isGeneralRetailProduct(product)) &&
      product.channels?.navKartWeb !== false
    );
  }
  if (vertical === 'express') {
    // Express 15-Min delivery covers in-stock fast-moving staples, dairy, frozen & snacks
    return Number(product.stock) > 0 && isGroceryCoreProduct(product);
  }
  if (vertical === 'essentials') {
    return isGroceryCoreProduct(product);
  }
  return true;
}

export function matchesDepartmentCategory(
  product: Product,
  departmentFilter: DepartmentFilter
): boolean {
  if (departmentFilter === 'all') return true;
  if (departmentFilter === 'frozen') return isFrozenProduct(product);
  if (departmentFilter === 'electronics') return isElectronicsOrHomeProduct(product);
  if (departmentFilter === 'apparel') return isApparelProduct(product);
  if (departmentFilter === 'retail') return isGeneralRetailProduct(product);
  return isGroceryCoreProduct(product) && !isFrozenProduct(product);
}

export function matchesGroceryAisle(product: Product, aisleId: string): boolean {
  if (aisleId === 'all') return true;
  if (aisleId === 'deals') {
    const mrp = Number(product.mrp) || 0;
    const sale = Number(product.sale || product.price) || 0;
    return mrp > sale;
  }

  const cat = (product.category || '').toLowerCase();
  const text = `${product.brand || ''} ${product.name || ''} ${cat} ${product.department || ''}`.toLowerCase();

  if (aisleId === 'electronics') {
    return isElectronicsOrHomeProduct(product);
  }
  if (aisleId === 'apparel') {
    return isApparelProduct(product);
  }
  if (aisleId === 'retail') {
    return isGeneralRetailProduct(product) || cat.includes('kitchen storage');
  }
  if (aisleId === 'frozen') {
    return isFrozenProduct(product);
  }
  if (aisleId === 'atta') {
    return cat.includes('atta') || cat.includes('dal') || text.includes('rice') || text.includes('besan');
  }
  if (aisleId === 'ghee') {
    return cat.includes('ghee') || cat.includes('oil');
  }
  if (aisleId === 'spices') {
    return cat.includes('spice') || text.includes('masala') || text.includes('salt');
  }
  if (aisleId === 'household') {
    return HOUSEHOLD_KEYWORDS.some((kw) => text.includes(kw));
  }
  if (aisleId === 'beverages') {
    if (HOUSEHOLD_KEYWORDS.some((kw) => text.includes(kw))) return false;
    return BEVERAGES_SNACKS_KEYWORDS.some((kw) => text.includes(kw)) || cat.includes('staples');
  }
  return true;
}

const PACKAGED_CONDIMENT_SAFE =
  /\b(ketchup|sauce|puree|paste|chutney|jam|pickle|juice|soup|masala|powder|chips|crisps|noodles|biscuit|candy|toffee|squash|syrup)\b/i;

const BANNED_FRESH_PATTERNS: RegExp[] = [
  /\bfresh\s+fruits?\b/i,
  /\bfresh\s+vegetables?\b/i,
  /\bfresh\s+produce\b/i,
  /\bfresh\s+(?:tomato|tomatoes|onion|onions|potato|potatoes|apple|apples|banana|bananas|mango|mangoes|spinach|coriander|okra|bhindi|cauliflower|cabbage|carrot|carrots|peas)\b/i,
  /\bsabzi\b/i,
];

export function hasZeroFreshProduce(product: Product): boolean {
  const deptCat = `${product.category || ''} ${product.department || ''}`.toLowerCase();
  if (/\bfresh\s+(?:fruits?|vegetables?|produce)\b|\bsabzi\b/i.test(deptCat)) {
    return false;
  }

  const hay = `${product.name || ''} ${deptCat}`.toLowerCase();
  if (PACKAGED_CONDIMENT_SAFE.test(hay)) {
    return true;
  }

  return !BANNED_FRESH_PATTERNS.some((pattern) => pattern.test(hay));
}

export function isStrictlyGrocery308(product: Product): boolean {
  if (!product || !product.id) return false;
  if (product.channels && product.channels.navGroceryWeb === false) return false;

  const idStr = String(product.id).trim();
  const match = /^NAV(\d+)$/i.exec(idStr);
  const isCore308 = Boolean(match && parseInt(match[1], 10) >= 1 && parseInt(match[1], 10) <= 308);
  const isAdminPublishedCustom = Boolean(product.channels?.navGroceryWeb === true);

  if (!isCore308 && !isAdminPublishedCustom) return false;

  const dept = (product.department || '').toLowerCase();
  const allowedDept = dept.includes('grocery') || dept.includes('groceries') || dept.includes('frozen');
  if (!allowedDept) return false;

  return hasZeroFreshProduce(product);
}

export function isAllowedRetailProduct(product: Product): boolean {
  if (!product || !product.id) return false;
  if (!hasZeroFreshProduce(product)) return false;

  if (product.channels) {
    const visibleAnywhere =
      product.channels.navGroceryWeb !== false || product.channels.navKartWeb !== false;
    if (!visibleAnywhere) return false;
  }

  const dept = (product.department || '').toLowerCase();
  return (
    dept.includes('grocery') ||
    dept.includes('groceries') ||
    dept.includes('frozen') ||
    dept.includes('electronic') ||
    dept.includes('mobile') ||
    dept.includes('clothing') ||
    dept.includes('apparel') ||
    dept.includes('footwear') ||
    dept.includes('home') ||
    dept.includes('kitchen') ||
    dept.includes('general retail') ||
    dept.includes('stationery') ||
    dept.includes('luggage')
  );
}

export function normalizeWholesalePricing(product: Product): Product {
  const mrp = Math.max(1, Math.round(Number(product.mrp) || 0));
  const sale = Math.round(mrp * 0.88);
  return {
    ...product,
    id: String(product.id).trim().toUpperCase(),
    mrp,
    sale,
    price: sale,
    stock: Number.isFinite(Number(product.stock)) ? Number(product.stock) : 25,
    gst: product.gst || '5%',
  };
}
