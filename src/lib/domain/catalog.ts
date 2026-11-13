import { z } from 'zod';
import importedData from './catalog_data.json';

export const CATEGORIES = [
  'Packaged Grocery',
  'Frozen Veg & Snacks',
  'Dairy & Desi Ghee',
  'Spices & Masalas',
  'Beverages & Tea',
  'Personal & Home Care',
] as const;

const BANNED_FRESH_KEYWORDS = [
  'fresh fruit',
  'fresh vegetable',
  'loose vegetable',
  'loose fruit',
];

export const ProductSchema = z
  .object({
    id: z.string().min(1),
    name: z.string().min(2),
    brand: z.string().min(1),
    category: z.string().min(1),
    packagingSize: z.string().min(1),
    barcode: z.string().min(4),
    mrp: z.number().positive(),
    salePrice: z.number().positive(),
    imageUrl: z.string().min(1),
    isFrozen: z.boolean().default(false),
    isFreshProduce: z.literal(false),
    inStock: z.boolean().default(true),
  })
  .superRefine((data, ctx) => {
    const lowerName = data.name.toLowerCase();
    if (BANNED_FRESH_KEYWORDS.some((kw) => lowerName.includes(kw))) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Fresh fruits and loose vegetables are strictly prohibited.',
        path: ['name'],
      });
    }
  });

export type Product = z.infer<typeof ProductSchema>;

export function calculateWholesalePrice(mrp: number): number {
  return Math.round(mrp * 0.88);
}

const FALLBACK_CATALOG: Product[] = [
  {
    id: 'sku-001',
    name: 'Safal Select IQF Frozen Green Peas',
    brand: 'Safal',
    category: 'Frozen Veg & Snacks',
    packagingSize: '1 kg',
    barcode: '8901725123456',
    mrp: 120,
    salePrice: calculateWholesalePrice(120),
    imageUrl: 'https://images.navgrocerystore.com/peas.png',
    isFrozen: true,
    isFreshProduce: false,
    inStock: true,
  },
  {
    id: 'sku-002',
    name: 'Aashirvaad Shudh Chakki Atta',
    brand: 'Aashirvaad',
    category: 'Packaged Grocery',
    packagingSize: '10 kg',
    barcode: '8901725111222',
    mrp: 480,
    salePrice: calculateWholesalePrice(480),
    imageUrl: 'https://images.navgrocerystore.com/atta.png',
    isFrozen: false,
    isFreshProduce: false,
    inStock: true,
  },
  {
    id: 'sku-003',
    name: 'Verka Pure Punjabi Desi Ghee Tin',
    brand: 'Verka',
    category: 'Dairy & Desi Ghee',
    packagingSize: '1 L',
    barcode: '8901725333444',
    mrp: 650,
    salePrice: calculateWholesalePrice(650),
    imageUrl: 'https://images.navgrocerystore.com/ghee.png',
    isFrozen: false,
    isFreshProduce: false,
    inStock: true,
  },
];

export const INITIAL_VERIFIED_CATALOG: Product[] =
  Array.isArray(importedData) && importedData.length > 0
    ? (importedData as Product[])
    : FALLBACK_CATALOG;
