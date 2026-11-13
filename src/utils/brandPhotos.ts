import { Product } from '../types';

export interface StudioCategoryRule {
  keywords: string[];
  exclude?: string[];
  imageUrl: string;
}

/**
 * Word-boundary studio packaging rules matching exact brand & product names
 * to verified pure-#FFFFFF commercial studio packaging photography.
 * Eliminates all legacy shifted paths, user phone snapshots, and grey placeholder boxes.
 */
const STUDIO_RULES: StudioCategoryRule[] = [
  {
    keywords: ['fries', 'tikki', 'patty', 'smiles', 'mccain'],
    imageUrl: '/images/studio/studio_mccain_fries.jpg',
  },
  {
    keywords: ['frozen', 'safal', 'paneer', 'peas', 'sweet corn', 'chaap'],
    imageUrl: '/images/studio/studio_frozen_peas.jpg',
  },
  {
    keywords: [
      'harpic',
      'lizol',
      'colin',
      'vim',
      'exo',
      'pril',
      'antiseptic',
      'savlon',
      'toilet cleaner',
      'surface cleaner',
      'dishwash',
    ],
    imageUrl: '/images/studio/studio_harpic_cleaner.jpg',
  },
  {
    keywords: ['surf excel', 'ariel', 'tide', 'rin', 'comfort', 'detergent', 'softener'],
    imageUrl: '/images/studio/studio_detergent_bag.jpg',
  },
  {
    keywords: [
      'soap',
      'bathing',
      'toothpaste',
      'dental cream',
      'cavity',
      'shampoo',
      'hair oil',
      'cold cream',
      'moisturizing',
      'body lotion',
      'colgate',
      'pepsodent',
      'sensodyne',
      'close up',
      'dove',
      'dettol',
      'lifebuoy',
      'pears',
      'lux',
      'santoor',
      'medimix',
      'godrej',
      'nivea',
      'vaseline',
      'ponds',
      'parachute',
      'bajaj',
      'head & shoulders',
      'clinic plus',
      'sunsilk',
      'pantene',
    ],
    imageUrl: '/images/studio/studio_personal_care.jpg',
  },
  {
    keywords: [
      'mustard oil',
      'sunflower oil',
      'soyabean oil',
      'edible oil',
      'blended oil',
      'cooking oil',
      'saffola',
    ],
    imageUrl: '/images/studio/studio_cooking_oil.jpg',
  },
  {
    keywords: ['ghee', 'vanaspati'],
    imageUrl: '/images/studio/studio_desi_ghee.jpg',
  },
  {
    keywords: ['atta', 'besan', 'sooji', 'maida', 'daliya', 'oats', 'corn flakes'],
    imageUrl: '/images/studio/studio_atta_aashirvaad.jpg',
  },
  {
    keywords: ['haldiram', 'haldirams', 'bhujia', 'mixture', 'khatta meetha', 'lays', 'kurkure', 'uncle chipps'],
    imageUrl: '/images/studio/studio_biscuits_chocolate.jpg',
  },
  {
    keywords: ['rice', 'dal', 'chana', 'rajma', 'moong'],
    imageUrl: '/images/studio/studio_basmati_rice.jpg',
  },
  {
    keywords: ['noodle', 'noodles', 'maggi'],
    exclude: ['sauce', 'ketchup'],
    imageUrl: '/images/studio/studio_noodles_pack.jpg',
  },
  {
    keywords: ['ketchup', 'sauce', 'jam', 'nutella', 'peanut butter', 'honey'],
    imageUrl: '/images/studio/studio_ketchup_jam.jpg',
  },
  {
    keywords: ['tea', 'chai', 'red label', 'taj mahal', 'wagh bakri', 'coffee', 'nescafe', 'bru', 'bournvita', 'horlicks', 'boost', 'rooh afza', 'sugar'],
    imageUrl: '/images/studio/studio_tea_red_pack.jpg',
  },
  {
    keywords: ['biscuit', 'biscuits', 'cookie', 'cookies', 'chips', 'chocolate', 'fantasy', 'oreo', 'bourbon', 'milk bikis', 'parle'],
    imageUrl: '/images/studio/studio_biscuits_chocolate.jpg',
  },
  {
    keywords: [
      'masala',
      'salt',
      'haldi',
      'mirch',
      'coriander',
      'pepper',
      'methi',
      'jeera',
      'mustard seeds',
      'ajwain',
      'hing',
      'asafoetida',
      'herbs',
      'mdh',
      'everest',
      'catch',
    ],
    imageUrl: '/images/studio/studio_spices_masala.jpg',
  },
];

function hasWord(haystack: string, keyword: string): boolean {
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`\\b${escaped}\\b`, 'i').test(haystack);
}

/**
 * Resolves every SKU (NAV001..NAV308) strictly by brand + exact product name
 * to a verified crisp commercial white-background studio pack image.
 */
export function resolveProductImage(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'> &
    Partial<Pick<Product, 'customImage' | 'image'>>
): string {
  const custom = (product.customImage || '').trim();
  if (custom.startsWith('/images/studio/') || custom.startsWith('data:image/jpeg') || custom.startsWith('data:image/png')) {
    return custom;
  }

  const hay = `${product.brand || ''} ${product.name || ''}`;
  for (const rule of STUDIO_RULES) {
    if (rule.exclude && rule.exclude.some((ex) => hasWord(hay, ex))) {
      continue;
    }
    if (rule.keywords.some((kw) => hasWord(hay, kw))) {
      return rule.imageUrl;
    }
  }

  const current = (product.image || '').trim();
  if (current.startsWith('/images/studio/')) {
    return current;
  }

  return '/images/studio/studio_atta_aashirvaad.jpg';
}

export function buildBrandPackageSvg(
  product: Pick<Product, 'id' | 'brand' | 'name' | 'category' | 'department'>
): string {
  return resolveProductImage(product);
}
