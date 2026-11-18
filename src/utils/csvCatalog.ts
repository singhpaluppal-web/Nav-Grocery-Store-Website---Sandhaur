import { Product, PublishingChannels } from '../types';
import {
  getCanonicalSkuImagePath,
  isVerifiedSkuFormat,
  resolveProductImage,
} from './brandPhotos';

export interface CsvImportRow {
  rowNumber: number;
  sku: string;
  name: string;
  mrp: number;
  sale: number;
  cost?: number;
  brand?: string;
  department?: string;
  category?: string;
  stock?: number;
  gst?: string;
  barcode?: string;
  image?: string;
  action: 'update' | 'create' | 'invalid';
  previousName?: string;
  previousMrp?: number;
  error?: string;
}

export interface CsvParseSummary {
  rows: CsvImportRow[];
  validCount: number;
  updateCount: number;
  createCount: number;
  invalidCount: number;
  detectedColumns: string[];
  fatalError?: string;
}

function detectDelimiter(cleanText: string): string {
  const firstLine = cleanText.split(/\r?\n/, 1)[0] || '';
  const commaCount = (firstLine.match(/,/g) || []).length;
  const tabCount = (firstLine.match(/\t/g) || []).length;
  const semiCount = (firstLine.match(/;/g) || []).length;
  if (commaCount === 0 && tabCount > 0) return '\t';
  if (commaCount === 0 && semiCount > 0) return ';';
  return ',';
}

function parseRawCsvMatrix(text: string): string[][] {
  const clean = text.replace(/^\uFEFF/, '');
  const delimiter = detectDelimiter(clean);
  const matrix: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let inQuotes = false;

  for (let i = 0; i < clean.length; i++) {
    const ch = clean[i];
    if (inQuotes) {
      if (ch === '"') {
        if (clean[i + 1] === '"') {
          cell += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        cell += ch;
      }
    } else if (ch === '"') {
      inQuotes = true;
    } else if (ch === delimiter) {
      row.push(cell.trim());
      cell = '';
    } else if (ch === '\r' || ch === '\n') {
      if (ch === '\r' && clean[i + 1] === '\n') i++;
      row.push(cell.trim());
      if (row.some((c) => c.length > 0)) {
        matrix.push(row);
      }
      row = [];
      cell = '';
    } else {
      cell += ch;
    }
  }

  row.push(cell.trim());
  if (row.some((c) => c.length > 0)) {
    matrix.push(row);
  }
  return matrix;
}

function normalizeHeader(h: string): string {
  return String(h || '')
    .replace(/^\uFEFF/, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
}

function normalizeCsvSku(raw: string): string {
  const trimmed = String(raw || '').trim().toUpperCase().replace(/\s+/g, '');
  if (!trimmed) return '';
  if (/^\d{1,3}$/.test(trimmed)) {
    const num = parseInt(trimmed, 10);
    if (num >= 1 && num <= 308) {
      return `NAV${String(num).padStart(3, '0')}`;
    }
  }
  const navMatch = /^NAV[-_\s]?0*(\d{1,4})$/i.exec(trimmed);
  if (navMatch) {
    const num = parseInt(navMatch[1], 10);
    return num <= 999 ? `NAV${String(num).padStart(3, '0')}` : `NAV${num}`;
  }
  return trimmed;
}

function isSyntheticPlaceholderName(val: string): boolean {
  const clean = String(val || '').trim();
  if (!clean || clean === '—' || clean === '-') return true;
  return /^(?:grocery\s*item|sample\s*item|test\s*item|item|product|sku)\s*#?\s*\d*$/i.test(
    clean
  );
}

function isPurelyNumericOrBool(val: string): boolean {
  const clean = String(val || '').trim();
  if (!clean || clean === '—' || clean === '-') return true;
  if (/^[₹Rs.\s]*-?\d+(?:[.,]\d+)?\s*%?$/i.test(clean)) return true;
  if (/^(?:true|false|yes|no|in stock|out of stock)$/i.test(clean)) return true;
  return false;
}

function parseNumeric(raw: string): number {
  const cleaned = String(raw || '')
    .replace(/[₹,\s]/g, '')
    .replace(/^rs\.?/i, '')
    .replace(/%/g, '');
  if (!cleaned) return NaN;
  const num = Number(cleaned);
  return Number.isFinite(num) ? num : NaN;
}

function normalizeGst(raw: string): string {
  const clean = String(raw || '').trim();
  if (!clean) return '';
  if (clean.endsWith('%')) return clean;
  const num = parseNumeric(clean);
  if (Number.isFinite(num) && num >= 0 && num <= 100) {
    return `${num}%`;
  }
  return '';
}

function sanitizeCsvImage(raw: string): string | undefined {
  const clean = String(raw || '').trim();
  if (!clean || clean === '—' || clean === '-') return undefined;
  const lower = clean.toLowerCase();
  if (
    lower.includes('/images/studio/') ||
    lower.includes('/images/verified/') ||
    lower.includes('placeholder') ||
    lower.includes('batch_')
  ) {
    return undefined;
  }
  if (
    clean.startsWith('data:image/') ||
    clean.startsWith('https://') ||
    clean.startsWith('http://') ||
    clean.startsWith('/products/') ||
    clean.startsWith('/images/')
  ) {
    return clean;
  }
  if (/^(?:NAV)?0*\d{1,3}\.(?:jpg|jpeg|png|webp)$/i.test(clean)) {
    return `/products/${clean}`;
  }
  return undefined;
}

export function parseCatalogCsv(
  csvText: string,
  existingProducts: Product[],
  verified308: Product[]
): CsvParseSummary {
  const matrix = parseRawCsvMatrix(csvText);
  if (matrix.length === 0) {
    return {
      rows: [],
      validCount: 0,
      updateCount: 0,
      createCount: 0,
      invalidCount: 0,
      detectedColumns: [],
      fatalError: 'CSV file is empty.',
    };
  }

  const existingMap = new Map<string, Product>();
  for (const p of verified308) existingMap.set(p.id.toUpperCase(), p);
  for (const p of existingProducts) existingMap.set(p.id.toUpperCase(), p);

  const firstRow = matrix[0];
  // Build strict header-name -> column-index lookup map (zero positional fallback)
  const headerIndexMap = new Map<string, number>();
  firstRow.forEach((rawHeader, idx) => {
    const key = normalizeHeader(rawHeader);
    if (key && !headerIndexMap.has(key)) {
      headerIndexMap.set(key, idx);
    }
  });

  const findHeaderIdx = (aliases: string[]): number => {
    for (const alias of aliases) {
      const found = headerIndexMap.get(alias);
      if (found !== undefined) return found;
    }
    return -1;
  };

  const skuIdx = findHeaderIdx([
    'id',
    'sku',
    'skuid',
    'skucode',
    'productid',
    'productcode',
    'itemcode',
    'itemid',
    'code',
  ]);
  const nameIdx = findHeaderIdx([
    'name',
    'productname',
    'title',
    'itemname',
    'producttitle',
  ]);
  const mrpIdx = findHeaderIdx([
    'mrp',
    'mrpinr',
    'mrprs',
    'mrpprice',
    'listprice',
    'maxretailprice',
    'unitmrp',
    'rate',
  ]);
  const saleIdx = findHeaderIdx([
    'sale',
    'saleprice',
    'saleinr',
    'sale12off',
    '12offwholesalepriceinr',
    'wholesalepriceinr',
    'wholesaleprice',
    'sellingprice',
    'discountedprice',
    'offerprice',
    'price',
  ]);
  const costIdx = findHeaderIdx(['cost', 'costprice', 'costinr', 'purchaseprice']);
  const brandIdx = findHeaderIdx(['brand', 'brandname', 'manufacturer']);
  const deptIdx = findHeaderIdx(['department', 'dept', 'section']);
  const catIdx = findHeaderIdx(['category', 'categoryname', 'aisle', 'subcategory']);
  const stockIdx = findHeaderIdx([
    'stock',
    'stockqty',
    'qty',
    'quantity',
    'inventory',
    'stockquantity',
  ]);
  const stockStatusIdx = findHeaderIdx(['stockstatus', 'status', 'availability']);
  const gstIdx = findHeaderIdx(['gst', 'gstrate', 'gstpercent', 'gstpct', 'tax', 'taxrate']);
  const barcodeIdx = findHeaderIdx(['barcode', 'ean', 'upc', 'gtin']);
  const imageIdx = findHeaderIdx([
    'image',
    'imageurl',
    'imagepath',
    'img',
    'imgurl',
    'photo',
    'photourl',
    'picture',
  ]);

  if (skuIdx === -1 || (mrpIdx === -1 && saleIdx === -1)) {
    return {
      rows: [],
      validCount: 0,
      updateCount: 0,
      createCount: 0,
      invalidCount: 0,
      detectedColumns: firstRow,
      fatalError:
        'Missing required CSV headers. Include named columns for id / sku, name, brand, category, mrp, sale, gst, stock, and image.',
    };
  }

  const seenSkus = new Set<string>();
  const parsedRows: CsvImportRow[] = [];

  for (let r = 1; r < matrix.length; r++) {
    const cols = matrix[r];
    const rowNumber = r + 1;

    const getCell = (colIdx: number): string =>
      colIdx >= 0 && colIdx < cols.length ? String(cols[colIdx] ?? '').trim() : '';

    const sku = normalizeCsvSku(getCell(skuIdx));
    const rawName = getCell(nameIdx);
    const parsedMrp = mrpIdx >= 0 ? parseNumeric(getCell(mrpIdx)) : NaN;
    const parsedSale = saleIdx >= 0 ? parseNumeric(getCell(saleIdx)) : NaN;

    if (!sku) {
      parsedRows.push({
        rowNumber,
        sku: '—',
        name: rawName || '—',
        mrp: 0,
        sale: 0,
        action: 'invalid',
        error: 'Missing SKU ID',
      });
      continue;
    }

    if (seenSkus.has(sku)) {
      parsedRows.push({
        rowNumber,
        sku,
        name: rawName || '—',
        mrp: Number.isFinite(parsedMrp) ? Math.round(parsedMrp) : 0,
        sale: Number.isFinite(parsedSale) ? Math.round(parsedSale) : 0,
        action: 'invalid',
        error: `Duplicate SKU (${sku}) in CSV`,
      });
      continue;
    }
    seenSkus.add(sku);

    const existing = existingMap.get(sku);
    const hasValidMrp = Number.isFinite(parsedMrp) && parsedMrp > 0;
    const rawSalePositive = Number.isFinite(parsedSale) && parsedSale > 0;

    if (!hasValidMrp && !rawSalePositive && !existing?.mrp) {
      parsedRows.push({
        rowNumber,
        sku,
        name: rawName || existing?.name || '—',
        mrp: 0,
        sale: 0,
        action: 'invalid',
        error: `Invalid MRP "${getCell(mrpIdx)}"`,
      });
      continue;
    }

    const mrp = hasValidMrp
      ? Math.max(1, Math.round(parsedMrp))
      : rawSalePositive
      ? Math.max(1, Math.round(parsedSale / 0.88))
      : Math.max(1, Math.round(existing?.mrp || 1));

    // Guard against missing/invalid or shifted values (e.g. GST 5% or cost/qty landing in sale column)
    const isValidSalePrice =
      rawSalePositive &&
      parsedSale <= mrp &&
      (mrp < 20 || parsedSale >= Math.round(mrp * 0.7));

    const sale = isValidSalePrice
      ? Math.max(1, Math.round(parsedSale))
      : Math.round(mrp * 0.88);

    const safeCsvName =
      !isSyntheticPlaceholderName(rawName) && !isPurelyNumericOrBool(rawName)
        ? rawName
        : '';
    const finalName = safeCsvName || existing?.name || '';

    if (!finalName) {
      parsedRows.push({
        rowNumber,
        sku,
        name: '—',
        mrp,
        sale,
        action: 'invalid',
        error: 'Product Name is required for new SKU',
      });
      continue;
    }

    const costRaw = getCell(costIdx);
    const parsedCost = costRaw ? parseNumeric(costRaw) : NaN;
    const stockRaw = getCell(stockIdx);
    const parsedStock = stockRaw ? parseNumeric(stockRaw) : NaN;
    const stockStatusRaw = getCell(stockStatusIdx).toLowerCase();
    const resolvedStock =
      Number.isFinite(parsedStock) && parsedStock >= 0
        ? Math.round(parsedStock)
        : stockStatusRaw.includes('out')
        ? 0
        : stockStatusRaw.includes('in stock')
        ? 25
        : undefined;

    const rawBrandCell = getCell(brandIdx);
    const rawBrand = !isPurelyNumericOrBool(rawBrandCell) ? rawBrandCell : '';

    const rawDeptCell = getCell(deptIdx);
    const rawDept = !isPurelyNumericOrBool(rawDeptCell) ? rawDeptCell : '';

    // Prevent numeric values like "66" or "5" from ever shifting into Category
    const rawCatCell = getCell(catIdx);
    const rawCat = !isPurelyNumericOrBool(rawCatCell) ? rawCatCell : '';

    const rawGst = gstIdx >= 0 ? normalizeGst(getCell(gstIdx)) : '';
    const rawBarcode = getCell(barcodeIdx);
    const rawImage = imageIdx >= 0 ? sanitizeCsvImage(getCell(imageIdx)) : undefined;

    parsedRows.push({
      rowNumber,
      sku,
      name: finalName,
      mrp,
      sale,
      cost:
        Number.isFinite(parsedCost) && parsedCost >= 0 ? Math.round(parsedCost) : undefined,
      brand: rawBrand ? rawBrand.toUpperCase() : undefined,
      department: rawDept || undefined,
      category: rawCat || undefined,
      stock: resolvedStock,
      gst: rawGst || undefined,
      barcode: rawBarcode || undefined,
      image: rawImage,
      action: existing ? 'update' : 'create',
      previousName: existing?.name,
      previousMrp: existing?.mrp,
    });
  }

  const updateCount = parsedRows.filter((x) => x.action === 'update').length;
  const createCount = parsedRows.filter((x) => x.action === 'create').length;
  const invalidCount = parsedRows.filter((x) => x.action === 'invalid').length;

  return {
    rows: parsedRows,
    validCount: updateCount + createCount,
    updateCount,
    createCount,
    invalidCount,
    detectedColumns: firstRow,
  };
}

export function buildUpdatedProductFromCsvRow(
  row: CsvImportRow,
  existing: Product | undefined,
  defaultChannels: PublishingChannels,
  preserveStockAndCustomPhotos: boolean
): Product {
  const now = new Date().toISOString();
  const safeName =
    row.name && !isSyntheticPlaceholderName(row.name)
      ? row.name
      : existing?.name || row.sku;

  if (existing) {
    const csvProvidedCustomImage =
      row.image &&
      (row.image.startsWith('data:image/') ||
        row.image.startsWith('https://') ||
        row.image.startsWith('http://'));

    const nextCustomImage = preserveStockAndCustomPhotos
      ? existing.customImage || (csvProvidedCustomImage ? row.image : undefined)
      : csvProvidedCustomImage
      ? row.image
      : row.image && row.image.startsWith('/products/')
      ? ''
      : existing.customImage;

    const merged: Product = {
      ...existing,
      id: row.sku,
      name: safeName,
      mrp: row.mrp,
      sale: row.sale,
      price: row.sale,
      brand: row.brand || existing.brand,
      department: row.department || existing.department,
      category: row.category || existing.category,
      cost: row.cost !== undefined ? row.cost : existing.cost,
      gst: row.gst || existing.gst,
      barcode: row.barcode || existing.barcode,
      stock:
        preserveStockAndCustomPhotos
          ? existing.stock
          : row.stock !== undefined
          ? row.stock
          : existing.stock,
      customImage: nextCustomImage,
      image: row.image || existing.image,
      channels: existing.channels || defaultChannels,
      updatedAt: now,
    };

    return {
      ...merged,
      image: resolveProductImage(merged),
    };
  }

  const isCustomUrl =
    row.image &&
    (row.image.startsWith('data:image/') ||
      row.image.startsWith('https://') ||
      row.image.startsWith('http://'));

  const fresh: Product = {
    id: row.sku,
    name: safeName,
    brand: row.brand || 'NAV STORE',
    department:
      row.department ||
      ((row.category || '').toLowerCase().includes('frozen')
        ? 'Cold Chain Frozen Foods'
        : 'Packaged Groceries'),
    category: row.category || 'Grocery Essentials',
    cost: row.cost,
    mrp: row.mrp,
    sale: row.sale,
    price: row.sale,
    stock: row.stock !== undefined ? row.stock : 25,
    gst: row.gst || '5%',
    barcode: row.barcode,
    customImage: isCustomUrl ? row.image : undefined,
    image: row.image || (isVerifiedSkuFormat(row.sku) ? getCanonicalSkuImagePath(row.sku) : ''),
    channels: { ...defaultChannels },
    updatedAt: now,
  };
  return {
    ...fresh,
    image: resolveProductImage(fresh),
  };
}

function escapeCsvCell(val: unknown): string {
  const s = String(val ?? '');
  if (/[",\r\n]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

export function buildSampleCsvTemplate(): string {
  const lines = [
    ['id', 'name', 'brand', 'category', 'cost', 'mrp', 'sale', 'gst', 'stock', 'image'].join(','),
    ['NAV001', 'Aashirvaad Superior MP Sharbati Atta 10kg', 'AASHIRVAAD', 'Atta & Dals', '412', '520', '458', '5%', '25', '/products/001.jpg'].join(','),
    ['NAV002', 'Fortune Chakki Fresh Atta 10kg', 'FORTUNE', 'Atta & Dals', '364', '460', '405', '5%', '25', '/products/002.jpg'].join(','),
    ['NAV309', 'Verka Pure Desi Ghee Tin 2L (Wholesale Pack)', 'VERKA', 'Desi Ghee & Oils', '1100', '1390', '1223', '5%', '15', '/products/309.jpg'].join(','),
  ];
  return lines.join('\n') + '\n';
}

export function exportCatalogToCsv(products: Product[]): string {
  const header = [
    'sku_id',
    'product_name',
    'mrp',
    'sale_12_off',
    'stock_qty',
    'stock_status',
    'brand',
    'department',
    'category',
    'cost',
    'gst',
    'barcode',
    'image',
    'nav_grocery_web',
    'navkart_web',
    'android_apps',
  ].join(',');

  const sorted = [...products].sort((a, b) =>
    a.id.localeCompare(b.id, undefined, { numeric: true })
  );

  const lines = sorted.map((p) => {
    const stockQty = Math.max(0, Number(p.stock ?? 0));
    const stockStatus = stockQty > 0 ? 'In Stock' : 'Out of Stock';
    const ch = p.channels || {
      navGroceryWeb: true,
      navKartWeb: true,
      androidApps: true,
    };
    const canonicalPath = getCanonicalSkuImagePath(p.id);
    const imgPath =
      isVerifiedSkuFormat(p.id) && (!p.customImage || p.customImage.startsWith('/products/'))
        ? canonicalPath
        : p.image && !p.image.startsWith('data:')
        ? p.image
        : canonicalPath;

    return [
      p.id,
      p.name,
      p.mrp,
      p.sale || Math.round(p.mrp * 0.88),
      stockQty,
      stockStatus,
      p.brand,
      p.department,
      p.category,
      p.cost ?? '',
      p.gst || '5%',
      p.barcode || '',
      imgPath,
      ch.navGroceryWeb ? 'Yes' : 'No',
      ch.navKartWeb ? 'Yes' : 'No',
      ch.androidApps ? 'Yes' : 'No',
    ]
      .map(escapeCsvCell)
      .join(',');
  });
  return '\uFEFF' + [header, ...lines].join('\n') + '\n';
}
