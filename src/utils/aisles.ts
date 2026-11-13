import { Product } from '../types';

export interface AisleDefinition {
  id: string;
  label: string;
  shortLabel: string;
  description: string;
  accentBg: string;
}

export const GROCERY_AISLES: AisleDefinition[] = [
  { id: 'all', label: 'All Store Items', shortLabel: 'All 308', description: 'Complete sealed grocery catalog', accentBg: '#0F3D28' },
  { id: 'deals', label: '12% Off Deals', shortLabel: '12% Deals', description: 'Wholesale savings off MRP', accentBg: '#B45309' },
  { id: 'frozen', label: 'Frozen Veg & Snacks (-18°C)', shortLabel: 'Frozen -18°C', description: 'Safal, McCain & Verka cold chain', accentBg: '#0369A1' },
  { id: 'atta', label: 'Atta, Rice & Dal', shortLabel: 'Atta & Dal', description: 'Aashirvaad, India Gate & Fortune', accentBg: '#92400E' },
  { id: 'ghee', label: 'Desi Ghee & Oils', shortLabel: 'Ghee & Oils', description: 'Verka, Amul, Fortune & Mustard Oil', accentBg: '#A16207' },
  { id: 'spices', label: 'Spices & Masale', shortLabel: 'Spices', description: 'MDH, Catch, Everest & Tata Salt', accentBg: '#991B1B' },
  { id: 'beverages', label: 'Tea, Beverages & Biscuits', shortLabel: 'Tea & Snacks', description: 'Tata Tea, Red Label, Maggi & Biscuits', accentBg: '#7C2D12' },
  { id: 'household', label: 'Household & Personal Care', shortLabel: 'Household', description: 'Surf Excel, Ariel, Vim, Dettol & Dove', accentBg: '#1E40AF' },
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

export type DepartmentFilter = 'all' | 'packaged' | 'frozen';

export function isFrozenProduct(product: Product): boolean {
  const cat = (product.category || '').toLowerCase();
  const dept = (product.department || '').toLowerCase();
  return dept.includes('frozen') || cat.includes('frozen');
}

export function matchesDepartmentCategory(
  product: Product,
  departmentFilter: DepartmentFilter
): boolean {
  if (departmentFilter === 'all') return true;
  const frozen = isFrozenProduct(product);
  return departmentFilter === 'frozen' ? frozen : !frozen;
}

export function matchesGroceryAisle(product: Product, aisleId: string): boolean {
  if (aisleId === 'all') return true;
  if (aisleId === 'deals') {
    const mrp = Number(product.mrp) || 0;
    const sale = Number(product.sale || product.price) || 0;
    return mrp > sale;
  }

  const cat = (product.category || '').toLowerCase();
  const text = `${product.brand || ''} ${product.name || ''} ${cat}`.toLowerCase();

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

const BANNED_FRESH_TERMS = [
  'fresh fruit', 'fresh vegetable', 'fresh tomato', 'fresh onion', 'fresh potato',
  'fresh apple', 'fresh banana', 'fresh mango', 'sabzi', 'fresh produce',
];

export function isStrictlyGrocery308(product: Product): boolean {
  if (!product || !product.id) return false;
  const match = /^NAV(\d+)$/i.exec(String(product.id).trim());
  if (!match) return false;
  const num = parseInt(match[1], 10);
  if (num < 1 || num > 308) return false;

  const dept = (product.department || '').toLowerCase();
  const allowedDept = dept.includes('grocery') || dept.includes('groceries') || dept.includes('frozen');
  if (!allowedDept) return false;

  const nameLower = `${product.name || ''} ${product.category || ''}`.toLowerCase();
  if (BANNED_FRESH_TERMS.some((term) => nameLower.includes(term))) return false;
  return true;
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
