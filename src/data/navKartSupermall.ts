import { Product } from '../types';

export interface SkuRatingSummary {
  rating: number;
  reviewCount: number;
}

export function getProductRating(product: Pick<Product, 'id' | 'rating' | 'reviewCount'>): SkuRatingSummary {
  if (typeof product.rating === 'number' && typeof product.reviewCount === 'number') {
    return {
      rating: Number(product.rating.toFixed(1)),
      reviewCount: product.reviewCount,
    };
  }
  const clean = String(product.id || 'NAV001');
  let hash = 0;
  for (let i = 0; i < clean.length; i++) {
    hash = (hash * 31 + clean.charCodeAt(i)) % 10000;
  }
  const tenths = 5 + (hash % 5); // 4.5 .. 4.9
  const rating = Number((4 + tenths / 10).toFixed(1));
  const reviewCount = 42 + (hash % 240);
  return { rating, reviewCount };
}

function makeSupermallSku(
  id: string,
  name: string,
  brand: string,
  department: string,
  category: string,
  mrp: number,
  barcode: string,
  gst = '18%',
  stock = 18
): Product {
  const sale = Math.round(mrp * 0.88);
  return {
    id,
    name,
    brand,
    department,
    category,
    mrp,
    sale,
    price: sale,
    stock,
    gst,
    barcode,
    image: '',
    channels: {
      navGroceryWeb: false,
      navKartWeb: true,
      androidApps: true,
    },
  };
}

/**
 * Curated Nav Kart Supermall SKUs across Electronics, Kitchen Appliances,
 * Home Utilities & Apparel — strictly sealed retail goods (zero fresh produce).
 */
export const NAVKART_SUPERMALL_SKUS: Product[] = [
  makeSupermallSku(
    'NK309',
    'boAt Rockerz 255 Pro+ Bluetooth Neckband',
    'BOAT',
    'Electronics & Mobiles',
    'Audio & Headphones',
    2990,
    '8904130893091',
    '18%'
  ),
  makeSupermallSku(
    'NK310',
    'Mi Power Bank 3i 20000mAh 18W Fast Charge',
    'XIAOMI',
    'Electronics & Mobiles',
    'Power & Charging',
    2499,
    '8904130893107',
    '18%'
  ),
  makeSupermallSku(
    'NK311',
    'Philips 12W B22 LED Bulb Pack of 4 (Cool Day Light)',
    'PHILIPS',
    'Electronics & Mobiles',
    'Home Lighting',
    680,
    '8904130893114',
    '18%'
  ),
  makeSupermallSku(
    'NK312',
    'Prestige PKOSS 1.5L Electric Stainless Kettle',
    'PRESTIGE',
    'Home & Kitchen',
    'Kitchen Appliances',
    1295,
    '8904130893121',
    '18%'
  ),
  makeSupermallSku(
    'NK313',
    'Bajaj Majesty DX-6 1000W Dry Iron',
    'BAJAJ',
    'Home & Kitchen',
    'Home Appliances',
    1050,
    '8904130893138',
    '18%'
  ),
  makeSupermallSku(
    'NK314',
    'Milton Thermosteel Flip Lid Flask 1000 ml',
    'MILTON',
    'Home & Kitchen',
    'Kitchen Storage',
    1140,
    '8904130893145',
    '12%'
  ),
  makeSupermallSku(
    'NK315',
    'Cello Checkers Plastic Airtight Containers Set of 18',
    'CELLO',
    'Home & Kitchen',
    'Kitchen Storage',
    1499,
    '8904130893152',
    '18%'
  ),
  makeSupermallSku(
    'NK316',
    'Noise ColorFit Pulse Grand Smartwatch (Jet Black)',
    'NOISE',
    'Electronics & Mobiles',
    'Wearables',
    2999,
    '8904130893169',
    '18%'
  ),
  makeSupermallSku(
    'NK317',
    'Monte Carlo Pure Cotton Straight Fit Kurta (Forest Green)',
    'MONTE CARLO',
    'Clothing & Apparel',
    'Menswear & Kurtas',
    1699,
    '8904130893176',
    '12%'
  ),
  makeSupermallSku(
    'NK318',
    'Fabindia Handloom Cotton Formal Kurta (Warm Cream)',
    'FABINDIA',
    'Clothing & Apparel',
    'Menswear & Kurtas',
    1890,
    '8904130893183',
    '12%'
  ),
  makeSupermallSku(
    'NK319',
    'Allen Solly Men Solid Polo T-Shirt Pack (Navy/Amber)',
    'ALLEN SOLLY',
    'Clothing & Apparel',
    'Casual Apparel',
    1399,
    '8904130893190',
    '12%'
  ),
  makeSupermallSku(
    'NK320',
    'Jockey 100% Super Combed Cotton Trackpants (Charcoal)',
    'JOCKEY',
    'Clothing & Apparel',
    'Casual Apparel',
    1199,
    '8904130893206',
    '12%'
  ),
  makeSupermallSku(
    'NK321',
    'Safari 30L Water-Resistant Everyday Travel Backpack (Black)',
    'SAFARI',
    'General Retail Goods',
    'General Retail & Travel',
    1499,
    '8904130893213',
    '18%'
  ),
  makeSupermallSku(
    'NK322',
    'Eveready DigiLED Rechargeable Torch & Emergency Lantern',
    'EVEREADY',
    'General Retail Goods',
    'General Retail & Home Utility',
    599,
    '8904130893220',
    '18%'
  ),
  makeSupermallSku(
    'NK323',
    'Classmate Longbook Ruled Notebooks Pack of 6 (172 Pages)',
    'CLASSMATE',
    'General Retail Goods',
    'General Retail & Stationery',
    420,
    '8904130893237',
    '12%'
  ),
];
