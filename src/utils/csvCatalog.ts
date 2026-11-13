import { Product, PublishingChannels } from '../types';
import { isVerifiedSkuFormat, resolveProductImage } from './brandPhotos';

export interface CsvImportRow {
  rowNumber: number;
  sku: string;
  name: string;
  mrp: number;
  sale: number;
  brand?: string;
  department?: string;
  category?: string;
  stock?: number;
  gst?: string;
  barcode?: string;
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

function parseRawCsvMatrix(text: string): string[][] {
  const clean = text.replace(/^\uFEFF/, '');
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
    } else if (ch === ',') {
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
  return h.toLowerCase().replace(/[^a-z0-9]/g, '');
}

function parseNumeric(raw: string): number {
  const cleaned = String(raw || '').replace(/[₹,\s]/g, '').replace(/^rs\.?/i, '');
  const num = Number(cleaned);
  return Number.isFinite(num) ? num : NaN;
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
  const normHeaders = firstRow.map(normalizeHeader);

  const findCol = (aliases: string[]): number =>
    normHeaders.findIndex((h) => aliases.includes(h));

  let skuIdx = findCol(['sku', 'skuid', 'id', 'productid', 'itemcode', 'code']);
  let nameIdx = findCol(['name', 'productname', 'title', 'itemname', 'producttitle']);
  let mrpIdx = findCol(['mrp', 'price', 'rate', 'mrpinr']);
  const brandIdx = findCol(['brand', 'brandname']);
  const deptIdx = findCol(['department', 'dept']);
  const catIdx = findCol(['category', 'aisle']);
  const stockIdx = findCol(['stock', 'qty', 'quantity']);
  const gstIdx = findCol(['gst', 'tax']);
  const barcodeIdx = findCol(['barcode', 'ean']);

  let startRowIndex = 1;

  // Support headerless 3-column CSVs if first row already starts with NAVxxx and has numeric MRP
  if (skuIdx === -1 && mrpIdx === -1 && firstRow.length >= 3) {
    const looksLikeSku = /^(NAV\d+|[A-Z]{2,}\d+)$/i.test(firstRow[0]);
    const col2Num = parseNumeric(firstRow[1]);
    const col3Num = parseNumeric(firstRow[2]);
    if (looksLikeSku && (Number.isFinite(col2Num) || Number.isFinite(col3Num))) {
      startRowIndex = 0;
      skuIdx = 0;
      if (Number.isFinite(col2Num) && !Number.isFinite(col3Num)) {
        mrpIdx = 1;
        nameIdx = 2;
      } else {
        nameIdx = 1;
        mrpIdx = 2;
      }
    }
  }

  if (skuIdx === -1 || mrpIdx === -1 || nameIdx === -1) {
    return {
      rows: [],
      validCount: 0,
      updateCount: 0,
      createCount: 0,
      invalidCount: 0,
      detectedColumns: firstRow,
      fatalError:
        'Missing required CSV columns. Include headers: sku_id (or sku), product_name (or name), and mrp.',
    };
  }

  const seenSkus = new Set<string>();
  const parsedRows: CsvImportRow[] = [];

  for (let r = startRowIndex; r < matrix.length; r++) {
    const cols = matrix[r];
    const rowNumber = r + 1;
    const sku = String(cols[skuIdx] || '')
      .trim()
      .toUpperCase()
      .replace(/\s+/g, '');
    const rawName = String(cols[nameIdx] || '').trim();
    const parsedMrp = parseNumeric(cols[mrpIdx] || '');

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
        sale: 0,
        action: 'invalid',
        error: `Duplicate SKU (${sku}) in CSV`,
      });
      continue;
    }
    seenSkus.add(sku);

    if (!Number.isFinite(parsedMrp) || parsedMrp <= 0) {
      parsedRows.push({
        rowNumber,
        sku,
        name: rawName || '—',
        mrp: 0,
        sale: 0,
        action: 'invalid',
        error: `Invalid MRP "${cols[mrpIdx] || ''}"`,
      });
      continue;
    }

    const mrp = Math.max(1, Math.round(parsedMrp));
    const sale = Math.round(mrp * 0.88);
    const existing = existingMap.get(sku);
    const finalName = rawName || existing?.name || '';

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

    const parsedStock = stockIdx >= 0 && cols[stockIdx] !== '' ? parseNumeric(cols[stockIdx]) : NaN;

    parsedRows.push({
      rowNumber,
      sku,
      name: finalName,
      mrp,
      sale,
      brand: brandIdx >= 0 && cols[brandIdx] ? cols[brandIdx].toUpperCase() : undefined,
      department: deptIdx >= 0 && cols[deptIdx] ? cols[deptIdx] : undefined,
      category: catIdx >= 0 && cols[catIdx] ? cols[catIdx] : undefined,
      stock: Number.isFinite(parsedStock) && parsedStock >= 0 ? Math.round(parsedStock) : undefined,
      gst: gstIdx >= 0 && cols[gstIdx] ? cols[gstIdx] : undefined,
      barcode: barcodeIdx >= 0 && cols[barcodeIdx] ? cols[barcodeIdx] : undefined,
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
  nameAndMrpOnly: boolean
): Product {
  const now = new Date().toISOString();
  if (existing) {
    const merged: Product = {
      ...existing,
      id: row.sku,
      name: row.name,
      mrp: row.mrp,
      sale: row.sale,
      price: row.sale,
      ...(nameAndMrpOnly
        ? {}
        : {
            brand: row.brand || existing.brand,
            department: row.department || existing.department,
            category: row.category || existing.category,
            stock: row.stock !== undefined ? row.stock : existing.stock,
            gst: row.gst || existing.gst,
            barcode: row.barcode || existing.barcode,
          }),
      channels: existing.channels || defaultChannels,
      updatedAt: now,
    };
    return {
      ...merged,
      image: resolveProductImage(merged),
    };
  }

  const fresh: Product = {
    id: row.sku,
    name: row.name,
    brand: row.brand || 'NAV STORE',
    department: row.department || 'Packaged Groceries',
    category: row.category || 'Grocery Essentials',
    mrp: row.mrp,
    sale: row.sale,
    price: row.sale,
    stock: row.stock !== undefined ? row.stock : 25,
    gst: row.gst || '5%',
    barcode: row.barcode,
    image: isVerifiedSkuFormat(row.sku) ? `/products/${row.sku}.jpg` : '',
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
    ['sku_id', 'product_name', 'mrp', 'brand', 'department', 'category', 'stock'].join(','),
    ['NAV001', 'Aashirvaad Superior MP Sharbati Atta 10kg', '520', 'AASHIRVAAD', 'Packaged Groceries', 'Atta & Dals', '25'].join(','),
    ['NAV002', 'Fortune Chakki Fresh Atta 10kg', '460', 'FORTUNE', 'Packaged Groceries', 'Atta & Dals', '25'].join(','),
    ['NAV309', 'Verka Pure Desi Ghee Tin 2L (Wholesale Pack)', '1390', 'VERKA', 'Packaged Groceries', 'Desi Ghee & Oils', '15'].join(','),
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
    'gst',
    'barcode',
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
    return [
      p.id,
      p.name,
      p.mrp,
      Math.round(p.mrp * 0.88),
      stockQty,
      stockStatus,
      p.brand,
      p.department,
      p.category,
      p.gst || '5%',
      p.barcode || '',
      ch.navGroceryWeb ? 'Yes' : 'No',
      ch.navKartWeb ? 'Yes' : 'No',
      ch.androidApps ? 'Yes' : 'No',
    ]
      .map(escapeCsvCell)
      .join(',');
  });
  return '\uFEFF' + [header, ...lines].join('\n') + '\n';
}
