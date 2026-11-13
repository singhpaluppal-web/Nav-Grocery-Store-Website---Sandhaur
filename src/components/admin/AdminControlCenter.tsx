import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Check,
  Copy,
  Database,
  Download,
  ExternalLink,
  FileSpreadsheet,
  History,
  ImagePlus,
  Layers,
  Lock,
  Pencil,
  Plus,
  QrCode,
  RefreshCw,
  RotateCcw,
  Search,
  Smartphone,
  Store,
  Trash2,
  Upload,
  Globe,
  X,
} from 'lucide-react';
import { Product, PublishingChannels } from '../../types';
import {
  buildBrandPackageSvg,
  hasCustomAdminPhoto,
  isVerifiedSkuFormat,
  resolveProductImage,
} from '../../utils/brandPhotos';
import {
  deleteProductFromFirebase,
  fetchAllFirebaseProductsRaw,
  fetchVerified308Catalog,
  FIREBASE_RTDB_BASE,
  patchProductsInFirebase,
  replaceAllProductsInFirebase,
  syncAndroidFeedSnapshot,
  upsertProductInFirebase,
} from '../../utils/firebase';
import {
  compressImageFileToDataUrl,
  extractSkuFromFilename,
} from '../../utils/imageCompression';
import { AdminPinGate } from './AdminPinGate';
import { BatchCsvImportPanel } from './BatchCsvImportPanel';
import { ActivityLogPanel } from './ActivityLogPanel';
import { SkuQrLabelsPanel } from './SkuQrLabelsPanel';
import { SkuQrModal } from './SkuQrModal';
import { exportCatalogToCsv } from '../../utils/csvCatalog';
import { readDeepLinkSkuFromLocation } from '../../utils/skuQr';
import {
  ActivityActionType,
  AdminActivityEntry,
  fetchActivityLogs,
  getSavedOperatorId,
  recordActivityEntry,
  saveOperatorId,
} from '../../utils/activityLog';

const ADMIN_PIN = '148020';
const ADMIN_SESSION_KEY = 'nav_admin_unlocked_v2';

type AdminTab =
  | 'catalog'
  | 'add'
  | 'bulkPhotos'
  | 'batchImport'
  | 'qrLabels'
  | 'sync'
  | 'activityLog';
type CatalogFilter = 'all' | 'verified308' | 'synthetic' | 'customPhoto' | 'outOfStock';
type ChannelFilter = 'all' | 'navGroceryWeb' | 'navKartWeb' | 'androidApps';

interface BulkPhotoQueueItem {
  id: string;
  fileName: string;
  targetSku: string;
  dataUrl: string;
  beforeBytes: number;
  afterBytes: number;
  status: 'ready' | 'syncing' | 'synced' | 'error';
  errorMsg?: string;
}

const DEPARTMENTS = [
  'Packaged Groceries',
  'Cold Chain Frozen Foods',
  'Clothing & Apparel',
  'Electronics & Mobiles',
  'Footwear & Shoes',
  'Home & Kitchen',
];

const DEFAULT_CHANNELS: PublishingChannels = {
  navGroceryWeb: true,
  navKartWeb: true,
  androidApps: true,
};

function formatKb(bytes: number): string {
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

function matchesSkuFilter(productId: string, rawQuery: string): boolean {
  const cleaned = rawQuery.trim().toUpperCase();
  if (!cleaned) return true;

  const skuUpper = String(productId || '').trim().toUpperCase();
  const numMatch = /^NAV(\d+)$/.exec(skuUpper);
  const skuNum = numMatch ? parseInt(numMatch[1], 10) : NaN;

  const parts = cleaned.split(/[\s,;]+/).filter(Boolean);
  return parts.some((part) => {
    const rangeMatch = /^(?:NAV)?(\d+)\s*[-:]\s*(?:NAV)?(\d+)$/.exec(part);
    if (rangeMatch && Number.isFinite(skuNum)) {
      const start = parseInt(rangeMatch[1], 10);
      const end = parseInt(rangeMatch[2], 10);
      const lo = Math.min(start, end);
      const hi = Math.max(start, end);
      return skuNum >= lo && skuNum <= hi;
    }

    if (/^\d+$/.test(part) && Number.isFinite(skuNum)) {
      const targetNum = parseInt(part, 10);
      const padded = `NAV${String(targetNum).padStart(3, '0')}`;
      return skuUpper === padded || skuUpper.includes(part);
    }

    return skuUpper.includes(part);
  });
}

export const AdminControlCenter: React.FC = () => {
  const [unlocked, setUnlocked] = useState<boolean>(() => {
    try {
      return sessionStorage.getItem(ADMIN_SESSION_KEY) === '1';
    } catch {
      return false;
    }
  });

  const [activeTab, setActiveTab] = useState<AdminTab>('catalog');
  const [products, setProducts] = useState<Product[]>([]);
  const [verified308, setVerified308] = useState<Product[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [firebaseConnected, setFirebaseConnected] = useState<boolean>(true);
  const [busyAction, setBusyAction] = useState<string>('');
  const [statusBanner, setStatusBanner] = useState<{
    type: 'ok' | 'err';
    text: string;
  } | null>(null);

  // Catalog Filters & Pagination
  const [skuSearchQuery, setSkuSearchQuery] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [catalogFilter, setCatalogFilter] = useState<CatalogFilter>('all');
  const [channelFilter, setChannelFilter] = useState<ChannelFilter>('all');
  const [selectedSkus, setSelectedSkus] = useState<Set<string>>(new Set());
  const [bulkStockQty, setBulkStockQty] = useState<string>('25');
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 30;

  // Inline Delete Confirm, Edit Modal & QR Code State
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [qrProduct, setQrProduct] = useState<Product | null>(null);
  const [pendingDeepLinkSku, setPendingDeepLinkSku] = useState<string | null>(() =>
    readDeepLinkSkuFromLocation()
  );
  const [singlePhotoSku, setSinglePhotoSku] = useState<string | null>(null);
  const singlePhotoInputRef = useRef<HTMLInputElement | null>(null);

  // Add Custom SKU Form State
  const [newSku, setNewSku] = useState('');
  const [newName, setNewName] = useState('');
  const [newBrand, setNewBrand] = useState('');
  const [newDepartment, setNewDepartment] = useState('Packaged Groceries');
  const [newCategory, setNewCategory] = useState('Atta & Dals');
  const [newMrp, setNewMrp] = useState('120');
  const [newStock, setNewStock] = useState('25');
  const [newGst, setNewGst] = useState('5%');
  const [newBarcode, setNewBarcode] = useState('');
  const [newChannels, setNewChannels] = useState<PublishingChannels>(DEFAULT_CHANNELS);
  const [newPhotoDataUrl, setNewPhotoDataUrl] = useState('');
  const [newPhotoInfo, setNewPhotoInfo] = useState('');

  // Bulk Photo Uploader State
  const [bulkQueue, setBulkQueue] = useState<BulkPhotoQueueItem[]>([]);
  const [bulkProgress, setBulkProgress] = useState<{ done: number; total: number } | null>(null);
  const bulkFileInputRef = useRef<HTMLInputElement | null>(null);

  // Sync & Android Feed State
  const [preserveCustomPhotosOnSync, setPreserveCustomPhotosOnSync] = useState(true);
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);
  const [androidFeedPreview, setAndroidFeedPreview] = useState<string>('');

  // Activity Log & Operator Tag State
  const [activityLogs, setActivityLogs] = useState<AdminActivityEntry[]>([]);
  const [logsLoading, setLogsLoading] = useState<boolean>(false);
  const [operatorId, setOperatorId] = useState<string>(() => getSavedOperatorId());

  const notify = useCallback((type: 'ok' | 'err', text: string) => {
    setStatusBanner({ type, text });
  }, []);

  const refreshLogs = useCallback(async () => {
    setLogsLoading(true);
    const logs = await fetchActivityLogs();
    setActivityLogs(logs);
    setLogsLoading(false);
  }, []);

  const logProductActivity = useCallback(
    async (input: {
      actionType: ActivityActionType;
      sku: string;
      summary: string;
      details?: string;
    }) => {
      const entry = await recordActivityEntry({
        ...input,
        actor: operatorId,
      });
      setActivityLogs((prev) => [entry, ...prev].slice(0, 250));
    },
    [operatorId]
  );

  const loadAdminData = useCallback(async () => {
    setLoading(true);
    setLogsLoading(true);
    const [fbRes, local308, logs] = await Promise.all([
      fetchAllFirebaseProductsRaw(),
      fetchVerified308Catalog(),
      fetchActivityLogs(),
    ]);
    setVerified308(local308);
    setFirebaseConnected(fbRes.connected);
    setActivityLogs(logs);
    setLogsLoading(false);

    if (fbRes.connected && fbRes.products.length > 0) {
      setProducts(fbRes.products);
    } else if (local308.length > 0 && !fbRes.connected) {
      setProducts(local308);
    } else {
      setProducts(fbRes.products);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    if (unlocked) {
      loadAdminData();
    }
  }, [unlocked, loadAdminData]);

  useEffect(() => {
    const syncScannedQuery = () => {
      const scannedSku = readDeepLinkSkuFromLocation();
      if (scannedSku) {
        setPendingDeepLinkSku(scannedSku);
      }
    };
    window.addEventListener('popstate', syncScannedQuery);
    window.addEventListener('hashchange', syncScannedQuery);
    return () => {
      window.removeEventListener('popstate', syncScannedQuery);
      window.removeEventListener('hashchange', syncScannedQuery);
    };
  }, []);

  useEffect(() => {
    if (!unlocked || loading || !pendingDeepLinkSku || products.length === 0) return;
    const targetSku = pendingDeepLinkSku.trim().toUpperCase();
    setPendingDeepLinkSku(null);
    setActiveTab('catalog');
    setSkuSearchQuery(targetSku);

    const matched = products.find((p) => p.id.toUpperCase() === targetSku);
    if (matched) {
      setEditingProduct({ ...matched });
      notify('ok', `Scanned SKU ${matched.id}: Opened management panel for ${matched.name}.`);
    } else {
      notify('err', `Scanned SKU "${targetSku}" was not found in the live Firebase catalog.`);
    }
  }, [unlocked, loading, pendingDeepLinkSku, products, notify]);

  const handleUnlock = () => {
    try {
      sessionStorage.setItem(ADMIN_SESSION_KEY, '1');
    } catch {
      // ignore
    }
    setUnlocked(true);
  };

  const handleLock = () => {
    try {
      sessionStorage.removeItem(ADMIN_SESSION_KEY);
    } catch {
      // ignore
    }
    setUnlocked(false);
  };

  const verified308Set = useMemo(() => new Set(verified308.map((p) => p.id)), [verified308]);

  const stats = useMemo(() => {
    let verifiedCount = 0;
    let customPhotoCount = 0;
    let syntheticCount = 0;
    let outOfStockCount = 0;
    for (const p of products) {
      if (isVerifiedSkuFormat(p.id) && (verified308Set.size === 0 || verified308Set.has(p.id))) {
        verifiedCount++;
      } else {
        syntheticCount++;
      }
      if (hasCustomAdminPhoto(p)) customPhotoCount++;
      if (Number(p.stock) <= 0) outOfStockCount++;
    }
    return {
      total: products.length,
      verifiedCount,
      customPhotoCount,
      syntheticCount,
      outOfStockCount,
    };
  }, [products, verified308Set]);

  const syntheticProducts = useMemo(
    () =>
      products.filter(
        (p) => !isVerifiedSkuFormat(p.id) || (verified308Set.size > 0 && !verified308Set.has(p.id))
      ),
    [products, verified308Set]
  );

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return products.filter((p) => {
      if (!matchesSkuFilter(p.id, skuSearchQuery)) return false;
      if (catalogFilter === 'verified308' && !isVerifiedSkuFormat(p.id)) return false;
      if (catalogFilter === 'synthetic' && isVerifiedSkuFormat(p.id)) return false;
      if (catalogFilter === 'customPhoto' && !hasCustomAdminPhoto(p)) return false;
      if (catalogFilter === 'outOfStock' && Number(p.stock) > 0) return false;

      const ch = p.channels || DEFAULT_CHANNELS;
      if (channelFilter !== 'all' && !ch[channelFilter]) return false;

      if (!q) return true;
      const hay = `${p.id} ${p.brand} ${p.name} ${p.category} ${p.department}`.toLowerCase();
      return q.split(/\s+/).every((tk) => hay.includes(tk));
    });
  }, [products, skuSearchQuery, searchQuery, catalogFilter, channelFilter]);

  useEffect(() => {
    setPage(1);
  }, [skuSearchQuery, searchQuery, catalogFilter, channelFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PAGE_SIZE));
  const pagedProducts = filteredProducts.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const selectedStockSummary = useMemo(() => {
    let inStockCount = 0;
    let outOfStockCount = 0;
    for (const p of products) {
      if (!selectedSkus.has(p.id)) continue;
      if (Number(p.stock) > 0) inStockCount++;
      else outOfStockCount++;
    }
    return {
      inStockCount,
      outOfStockCount,
      allInStock: selectedSkus.size > 0 && outOfStockCount === 0,
    };
  }, [products, selectedSkus]);

  const handleToggleChannel = async (
    product: Product,
    key: keyof PublishingChannels
  ) => {
    const prevChannels = product.channels || { ...DEFAULT_CHANNELS };
    const updated: Product = {
      ...product,
      channels: {
        ...prevChannels,
        [key]: !prevChannels[key],
      },
    };
    const nextList = products.map((item) => (item.id === product.id ? updated : item));
    setProducts(nextList);

    const res = await upsertProductInFirebase(updated);
    if (res.ok) {
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'channel_toggle',
        sku: product.id,
        summary: `Set ${key} to ${updated.channels?.[key] ? 'ON' : 'OFF'} for ${product.name}`,
      });
      notify('ok', `${product.id}: Updated publishing channel (${key}) in Firebase & Android Feed.`);
    } else {
      setProducts(products);
      notify('err', `Failed to update ${product.id} in Firebase.`);
    }
  };

  const handleQuickStockToggle = async (product: Product) => {
    const prevStock = Number(product.stock) || 0;
    const nextStock = prevStock > 0 ? 0 : 25;
    const updated: Product = { ...product, stock: nextStock };
    const nextList = products.map((p) => (p.id === product.id ? updated : p));
    setProducts(nextList);

    const res = await upsertProductInFirebase(updated);
    if (res.ok) {
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'stock_toggle',
        sku: product.id,
        summary: `${nextStock > 0 ? 'Marked In Stock' : 'Marked Out of Stock'} · ${product.name}`,
        details: `Stock qty: ${prevStock} → ${nextStock}`,
      });
      notify('ok', `${product.id}: Stock set to ${nextStock > 0 ? `${nextStock} units (In Stock)` : '0 (Out of Stock)'}.`);
    } else {
      setProducts(products);
      notify('err', `Failed to update stock for ${product.id}.`);
    }
  };

  const toggleSelectSku = (skuId: string) => {
    setSelectedSkus((prev) => {
      const next = new Set(prev);
      if (next.has(skuId)) next.delete(skuId);
      else next.add(skuId);
      return next;
    });
  };

  const handleSelectAllFiltered = () => {
    const filteredIds = filteredProducts.map((p) => p.id);
    const allSelected =
      filteredIds.length > 0 && filteredIds.every((id) => selectedSkus.has(id));
    setSelectedSkus((prev) => {
      const next = new Set(prev);
      if (allSelected) {
        filteredIds.forEach((id) => next.delete(id));
      } else {
        filteredIds.forEach((id) => next.add(id));
      }
      return next;
    });
  };

  const handleBulkUpdateSelectedStock = async (stockValue: number) => {
    const targetIds = Array.from(selectedSkus);
    if (targetIds.length === 0) return;
    setBusyAction('bulk-stock');
    const cleanStock = Math.max(0, Math.round(stockValue));
    const now = new Date().toISOString();
    const patchPayload: Record<string, Product> = {};
    const targetSet = new Set(targetIds);

    const nextList = products.map((p) => {
      if (!targetSet.has(p.id)) return p;
      const updated: Product = { ...p, stock: cleanStock, updatedAt: now };
      patchPayload[p.id] = updated;
      return updated;
    });

    const res = await patchProductsInFirebase(patchPayload);
    if (res.ok) {
      const updatedCount = Object.keys(patchPayload).length;
      const sampleSkus = targetIds.slice(0, 6).join(', ');
      setProducts(nextList);
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'bulk_stock',
        sku: updatedCount === 1 ? targetIds[0] : `${updatedCount} SKUs`,
        summary: `Bulk stock set to ${cleanStock} units (${cleanStock > 0 ? 'In Stock' : 'Out of Stock'})`,
        details: `SKUs: ${sampleSkus}${targetIds.length > 6 ? ` +${targetIds.length - 6} more` : ''}`,
      });
      notify(
        'ok',
        `Updated stock to ${cleanStock} units (${cleanStock > 0 ? 'In Stock' : 'Out of Stock'}) for ${updatedCount} selected SKUs in Firebase.`
      );
    } else {
      notify('err', 'Bulk stock update failed in Firebase.');
    }
    setBusyAction('');
  };

  const handleBulkToggleStockStatus = async () => {
    const restockUnits = Math.max(1, Math.round(Number(bulkStockQty) || 25));
    const targetStock = selectedStockSummary.allInStock ? 0 : restockUnits;
    await handleBulkUpdateSelectedStock(targetStock);
  };

  const handleBulkFlipEachSelectedStock = async () => {
    const targetIds = Array.from(selectedSkus);
    if (targetIds.length === 0) return;
    setBusyAction('bulk-stock');
    const restockUnits = Math.max(1, Math.round(Number(bulkStockQty) || 25));
    const now = new Date().toISOString();
    const patchPayload: Record<string, Product> = {};
    const targetSet = new Set(targetIds);

    const nextList = products.map((p) => {
      if (!targetSet.has(p.id)) return p;
      const nextStock = Number(p.stock) > 0 ? 0 : restockUnits;
      const updated: Product = { ...p, stock: nextStock, updatedAt: now };
      patchPayload[p.id] = updated;
      return updated;
    });

    const res = await patchProductsInFirebase(patchPayload);
    if (res.ok) {
      const updatedCount = Object.keys(patchPayload).length;
      const sampleSkus = targetIds.slice(0, 6).join(', ');
      setProducts(nextList);
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'bulk_stock',
        sku: updatedCount === 1 ? targetIds[0] : `${updatedCount} SKUs`,
        summary: `Toggled In Stock / Out of Stock for ${updatedCount} selected SKUs`,
        details: `SKUs: ${sampleSkus}${targetIds.length > 6 ? ` +${targetIds.length - 6} more` : ''}`,
      });
      notify(
        'ok',
        `Toggled stock status for ${updatedCount} selected SKUs in Firebase & Android Feed.`
      );
    } else {
      notify('err', 'Bulk stock toggle failed in Firebase.');
    }
    setBusyAction('');
  };

  const handleUpdateSelectedDropdown = async (action: string) => {
    if (!action || selectedSkus.size === 0) return;
    if (action === 'mark_in_stock') {
      await handleBulkUpdateSelectedStock(Math.max(1, Math.round(Number(bulkStockQty) || 25)));
    } else if (action === 'mark_out_of_stock') {
      await handleBulkUpdateSelectedStock(0);
    } else if (action === 'toggle_stock') {
      await handleBulkFlipEachSelectedStock();
    } else if (action === 'enable_navGrocery') {
      await handleBulkSetChannelForSelected('navGroceryWeb', true);
    } else if (action === 'disable_navGrocery') {
      await handleBulkSetChannelForSelected('navGroceryWeb', false);
    } else if (action === 'enable_navKart') {
      await handleBulkSetChannelForSelected('navKartWeb', true);
    } else if (action === 'disable_navKart') {
      await handleBulkSetChannelForSelected('navKartWeb', false);
    } else if (action === 'enable_android') {
      await handleBulkSetChannelForSelected('androidApps', true);
    } else if (action === 'disable_android') {
      await handleBulkSetChannelForSelected('androidApps', false);
    } else if (action === 'clear_selection') {
      setSelectedSkus(new Set());
    }
  };

  const handleBulkSetChannelForSelected = async (
    channelKey: keyof PublishingChannels,
    enabled: boolean
  ) => {
    const targetIds = Array.from(selectedSkus);
    if (targetIds.length === 0) return;
    setBusyAction('bulk-channel');
    const now = new Date().toISOString();
    const patchPayload: Record<string, Product> = {};
    const targetSet = new Set(targetIds);

    const nextList = products.map((p) => {
      if (!targetSet.has(p.id)) return p;
      const updated: Product = {
        ...p,
        channels: {
          ...(p.channels || DEFAULT_CHANNELS),
          [channelKey]: enabled,
        },
        updatedAt: now,
      };
      patchPayload[p.id] = updated;
      return updated;
    });

    const res = await patchProductsInFirebase(patchPayload);
    if (res.ok) {
      const updatedCount = Object.keys(patchPayload).length;
      const sampleSkus = targetIds.slice(0, 6).join(', ');
      setProducts(nextList);
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'channel_toggle',
        sku: updatedCount === 1 ? targetIds[0] : `${updatedCount} SKUs`,
        summary: `Bulk channel ${channelKey} set to ${enabled ? 'ON' : 'OFF'}`,
        details: `SKUs: ${sampleSkus}${targetIds.length > 6 ? ` +${targetIds.length - 6} more` : ''}`,
      });
      notify(
        'ok',
        `Set ${channelKey} = ${enabled ? 'ON' : 'OFF'} for ${updatedCount} selected SKUs.`
      );
    } else {
      notify('err', 'Bulk channel update failed in Firebase.');
    }
    setBusyAction('');
  };

  const handleDeleteProduct = async (productId: string) => {
    setBusyAction(`del-${productId}`);
    const existing = products.find((p) => p.id === productId);
    const res = await deleteProductFromFirebase(productId);
    if (res.ok) {
      const nextList = products.filter((p) => p.id !== productId);
      setProducts(nextList);
      await syncAndroidFeedSnapshot(nextList);
      setConfirmDeleteId(null);
      void logProductActivity({
        actionType: 'sku_delete',
        sku: productId,
        summary: `Deleted SKU ${productId}${existing ? ` (${existing.name})` : ''}`,
      });
      notify('ok', `Deleted ${productId} from Firebase RTDB & Android Feed.`);
    } else {
      notify('err', `Could not delete ${productId} from Firebase.`);
    }
    setBusyAction('');
  };

  const handleSaveEditProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProduct) return;
    setBusyAction('save-edit');
    const mrp = Math.max(1, Math.round(Number(editingProduct.mrp) || 1));
    const sale = Math.round(mrp * 0.88);
    const updated: Product = {
      ...editingProduct,
      id: editingProduct.id.trim().toUpperCase(),
      mrp,
      sale,
      price: sale,
      stock: Math.max(0, Math.round(Number(editingProduct.stock) || 0)),
      image: resolveProductImage(editingProduct),
    };
    const original = products.find((p) => p.id === updated.id);
    const res = await upsertProductInFirebase(updated);
    if (res.ok) {
      const nextList = products.map((p) => (p.id === updated.id ? updated : p));
      setProducts(nextList);
      await syncAndroidFeedSnapshot(nextList);
      setEditingProduct(null);

      const diffs: string[] = [];
      if (original && original.mrp !== updated.mrp) {
        diffs.push(`MRP: ₹${original.mrp} → ₹${updated.mrp} (Sale ₹${sale})`);
      }
      if (original && Number(original.stock) !== Number(updated.stock)) {
        diffs.push(`Stock: ${original.stock} → ${updated.stock}`);
      }
      if (original && original.name !== updated.name) {
        diffs.push(`Title updated`);
      }
      if (original && original.brand !== updated.brand) {
        diffs.push(`Brand: ${original.brand} → ${updated.brand}`);
      }

      void logProductActivity({
        actionType: original && original.mrp !== updated.mrp ? 'price_update' : 'sku_edit',
        sku: updated.id,
        summary:
          original && original.mrp !== updated.mrp
            ? `Updated MRP for ${updated.name} (₹${original.mrp} → ₹${updated.mrp})`
            : `Updated product details for ${updated.name}`,
        details: diffs.length > 0 ? diffs.join(' · ') : `MRP ₹${mrp} · Stock ${updated.stock}`,
      });

      notify('ok', `Saved ${updated.id} (MRP ₹${mrp} → Wholesale ₹${sale}) across all channels.`);
    } else {
      notify('err', `Failed to save changes for ${updated.id}.`);
    }
    setBusyAction('');
  };

  const triggerSinglePhotoReplace = (skuId: string) => {
    setSinglePhotoSku(skuId);
    singlePhotoInputRef.current?.click();
  };

  const handleSinglePhotoFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || !singlePhotoSku) return;

    const target = products.find((p) => p.id === singlePhotoSku);
    if (!target) return;

    setBusyAction(`photo-${singlePhotoSku}`);
    try {
      const compressed = await compressImageFileToDataUrl(file, 400, 32 * 1024);
      const updated: Product = {
        ...target,
        customImage: compressed.dataUrl,
        image: compressed.dataUrl,
      };
      const res = await upsertProductInFirebase(updated);
      if (res.ok) {
        const nextList = products.map((p) => (p.id === target.id ? updated : p));
        setProducts(nextList);
        if (editingProduct?.id === target.id) {
          setEditingProduct(updated);
        }
        await syncAndroidFeedSnapshot(nextList);
        void logProductActivity({
          actionType: 'photo_update',
          sku: target.id,
          summary: `Uploaded custom photo for ${target.name}`,
          details: `Compressed ${formatKb(compressed.beforeBytes)} → ${formatKb(compressed.afterBytes)}`,
        });
        notify(
          'ok',
          `${target.id}: Custom photo compressed (${formatKb(compressed.beforeBytes)} → ${formatKb(
            compressed.afterBytes
          )}) & synced live to Firebase.`
        );
      } else {
        notify('err', `Failed to upload photo for ${target.id} to Firebase.`);
      }
    } catch (err) {
      notify('err', err instanceof Error ? err.message : 'Photo compression failed.');
    }
    setBusyAction('');
    setSinglePhotoSku(null);
  };

  const handleResetProductPhoto = async (product: Product) => {
    setBusyAction(`reset-photo-${product.id}`);
    const canonical = isVerifiedSkuFormat(product.id)
      ? `/products/${product.id}.jpg`
      : '';
    const updated: Product = {
      ...product,
      customImage: '',
      image: canonical,
    };
    const res = await upsertProductInFirebase(updated);
    if (res.ok) {
      const nextList = products.map((p) => (p.id === product.id ? updated : p));
      setProducts(nextList);
      if (editingProduct?.id === product.id) {
        setEditingProduct(updated);
      }
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'photo_update',
        sku: product.id,
        summary: `Reverted photo to ${canonical || 'default SKU card'} for ${product.name}`,
      });
      notify('ok', `${product.id}: Reverted photo to ${canonical || 'default SKU card'}.`);
    } else {
      notify('err', `Could not revert photo for ${product.id}.`);
    }
    setBusyAction('');
  };

  // Add Custom SKU handlers
  const handleNewSkuPhotoPick = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const compressed = await compressImageFileToDataUrl(file, 400, 32 * 1024);
      setNewPhotoDataUrl(compressed.dataUrl);
      setNewPhotoInfo(
        `Compressed ${formatKb(compressed.beforeBytes)} → ${formatKb(compressed.afterBytes)}`
      );
    } catch {
      notify('err', 'Could not process selected photo.');
    }
  };

  const handleCreateCustomSku = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanSku = newSku.trim().toUpperCase().replace(/\s+/g, '');
    if (!cleanSku || !newName.trim() || !newBrand.trim()) {
      notify('err', 'SKU ID, Brand Name, and Product Title are required.');
      return;
    }

    const mrpVal = Math.max(1, Math.round(Number(newMrp) || 1));
    const saleVal = Math.round(mrpVal * 0.88);
    const stockVal = Math.max(0, Math.round(Number(newStock) || 0));

    const created: Product = {
      id: cleanSku,
      name: newName.trim(),
      brand: newBrand.trim().toUpperCase(),
      department: newDepartment,
      category: newCategory.trim() || 'General',
      mrp: mrpVal,
      sale: saleVal,
      price: saleVal,
      stock: stockVal,
      gst: newGst,
      barcode: newBarcode.trim() || undefined,
      customImage: newPhotoDataUrl || undefined,
      image:
        newPhotoDataUrl ||
        (isVerifiedSkuFormat(cleanSku) ? `/products/${cleanSku}.jpg` : ''),
      channels: { ...newChannels },
    };

    setBusyAction('create-sku');
    const res = await upsertProductInFirebase(created);
    if (res.ok) {
      const exists = products.some((p) => p.id === cleanSku);
      const nextList = exists
        ? products.map((p) => (p.id === cleanSku ? created : p))
        : [created, ...products].sort((a, b) =>
            a.id.localeCompare(b.id, undefined, { numeric: true })
          );
      setProducts(nextList);
      await syncAndroidFeedSnapshot(nextList);
      void logProductActivity({
        actionType: 'sku_create',
        sku: cleanSku,
        summary: `${exists ? 'Updated' : 'Published'} SKU ${created.name} (MRP ₹${mrpVal}, 12% Off ₹${saleVal})`,
        details: `Dept: ${created.department} · Opening stock: ${stockVal}`,
      });
      setNewSku('');
      setNewName('');
      setNewBrand('');
      setNewBarcode('');
      setNewPhotoDataUrl('');
      setNewPhotoInfo('');
      notify(
        'ok',
        `Published ${cleanSku} (${created.name}) to Firebase RTDB & selected channels.`
      );
      setActiveTab('catalog');
    } else {
      notify('err', `Failed to save ${cleanSku} to Firebase.`);
    }
    setBusyAction('');
  };

  // Bulk Photo Uploader handlers
  const handleBulkFilesSelected = async (files: FileList | null) => {
    if (!files || files.length === 0) return;
    setBusyAction('compress-bulk');
    const items: BulkPhotoQueueItem[] = [];
    for (const file of Array.from(files)) {
      if (!file.type.startsWith('image/')) continue;
      const guessedSku = extractSkuFromFilename(file.name) || '';
      try {
        const res = await compressImageFileToDataUrl(file, 400, 32 * 1024);
        items.push({
          id: `${file.name}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          fileName: file.name,
          targetSku: guessedSku,
          dataUrl: res.dataUrl,
          beforeBytes: res.beforeBytes,
          afterBytes: res.afterBytes,
          status: 'ready',
        });
      } catch (err) {
        items.push({
          id: `${file.name}-${Date.now()}`,
          fileName: file.name,
          targetSku: guessedSku,
          dataUrl: '',
          beforeBytes: file.size,
          afterBytes: 0,
          status: 'error',
          errorMsg: err instanceof Error ? err.message : 'Decode failed',
        });
      }
    }
    setBulkQueue((prev) => [...prev, ...items]);
    setBusyAction('');
  };

  const handleBulkSyncAll = async () => {
    const valid = bulkQueue.filter(
      (item) => item.status !== 'synced' && item.dataUrl && item.targetSku.trim().length > 0
    );
    if (valid.length === 0) {
      notify('err', 'Add images with valid target SKU IDs (e.g. NAV001..NAV308) first.');
      return;
    }

    setBusyAction('sync-bulk');
    setBulkProgress({ done: 0, total: valid.length });
    const byId = new Map(products.map((p) => [p.id, p]));
    let syncedCount = 0;

    // Upload in batches of 15 via Firebase PATCH
    for (let i = 0; i < valid.length; i += 15) {
      const chunk = valid.slice(i, i + 15);
      const patchPayload: Record<string, Product> = {};
      const now = new Date().toISOString();

      for (const qItem of chunk) {
        const cleanSku = qItem.targetSku.trim().toUpperCase();
        const existing =
          byId.get(cleanSku) || verified308.find((v) => v.id === cleanSku);
        if (!existing) continue;
        const updatedProduct: Product = {
          ...existing,
          customImage: qItem.dataUrl,
          image: qItem.dataUrl,
          updatedAt: now,
        };
        patchPayload[cleanSku] = updatedProduct;
        byId.set(cleanSku, updatedProduct);
      }

      if (Object.keys(patchPayload).length > 0) {
        const res = await patchProductsInFirebase(patchPayload);
        setBulkQueue((prev) =>
          prev.map((row) => {
            const match = chunk.find((c) => c.id === row.id);
            if (!match) return row;
            const skuUpper = match.targetSku.trim().toUpperCase();
            if (!patchPayload[skuUpper]) {
              return { ...row, status: 'error', errorMsg: `SKU ${skuUpper} not in catalog` };
            }
            if (res.ok) syncedCount++;
            return {
              ...row,
              status: res.ok ? 'synced' : 'error',
              errorMsg: res.ok ? undefined : 'Firebase PATCH failed',
            };
          })
        );
      }
      setBulkProgress({ done: Math.min(i + chunk.length, valid.length), total: valid.length });
    }

    const mergedList = Array.from(byId.values()).sort((a, b) =>
      a.id.localeCompare(b.id, undefined, { numeric: true })
    );
    setProducts(mergedList);
    await syncAndroidFeedSnapshot(mergedList);
    if (syncedCount > 0) {
      const sample = valid
        .slice(0, 6)
        .map((v) => v.targetSku.trim().toUpperCase())
        .join(', ');
      void logProductActivity({
        actionType: 'photo_update',
        sku: syncedCount === 1 ? valid[0].targetSku.trim().toUpperCase() : `${syncedCount} SKUs`,
        summary: `Bulk uploaded & compressed ${syncedCount} SKU photos`,
        details: `SKUs: ${sample}${valid.length > 6 ? ` +${valid.length - 6} more` : ''}`,
      });
    }
    setBusyAction('');
    setBulkProgress(null);
    notify('ok', `Bulk photo sync complete: ${syncedCount} SKU photos pushed live to Firebase & Android Feed.`);
  };

  // One-Click Sync All 308 Verified SKUs
  const handleOneClickSync308 = async () => {
    if (verified308.length === 0) {
      notify('err', 'Verified 308 catalog could not be loaded.');
      return;
    }
    setBusyAction('sync-308');
    const existingById = new Map(products.map((p) => [p.id, p]));
    const customSkusToKeep = products.filter(
      (p) => !isVerifiedSkuFormat(p.id) && p.channels?.navKartWeb !== undefined
    );

    const synced308 = verified308.map((base) => {
      const existing = existingById.get(base.id);
      const keepCustom =
        preserveCustomPhotosOnSync && existing && hasCustomAdminPhoto(existing);
      const mrp = existing?.mrp && existing.mrp > 0 ? existing.mrp : base.mrp;
      const sale = Math.round(mrp * 0.88);
      return {
        ...base,
        stock: existing?.stock !== undefined ? existing.stock : base.stock,
        mrp,
        sale,
        price: sale,
        channels: existing?.channels || { ...DEFAULT_CHANNELS },
        customImage: keepCustom ? existing?.customImage : '',
        image: keepCustom
          ? existing?.customImage || `/products/${base.id}.jpg`
          : `/products/${base.id}.jpg`,
      };
    });

    const combined = [...synced308, ...customSkusToKeep].sort((a, b) =>
      a.id.localeCompare(b.id, undefined, { numeric: true })
    );

    const res = await replaceAllProductsInFirebase(combined);
    if (res.ok) {
      setProducts(combined);
      void logProductActivity({
        actionType: 'bulk_sync',
        sku: 'NAV001–NAV308',
        summary: `Synced all ${synced308.length} verified SKUs (+${customSkusToKeep.length} custom) to Firebase`,
        details: `Preserve custom photos: ${preserveCustomPhotosOnSync ? 'Yes' : 'No'}`,
      });
      notify(
        'ok',
        `One-Click Sync complete: All ${synced308.length} verified SKUs (/products/NAV001.jpg–NAV308.jpg) synced to Firebase & Android JSON Feed.`
      );
    } else {
      notify('err', 'Firebase bulk sync failed. Check network connection.');
    }
    setBusyAction('');
  };

  const handleDeleteAllSynthetic = async () => {
    if (syntheticProducts.length === 0) {
      notify('ok', 'Zero synthetic products detected in Firebase.');
      return;
    }
    setBusyAction('delete-synthetic');
    const patchMap: Record<string, null> = {};
    for (const s of syntheticProducts) {
      patchMap[s.id] = null;
    }
    const deletedIds = syntheticProducts.map((s) => s.id);
    const res = await patchProductsInFirebase(patchMap);
    if (res.ok) {
      const cleaned = products.filter((p) => !patchMap.hasOwnProperty(p.id));
      setProducts(cleaned);
      await syncAndroidFeedSnapshot(cleaned);
      void logProductActivity({
        actionType: 'sku_delete',
        sku: `${deletedIds.length} SKUs`,
        summary: `Cleaned up ${deletedIds.length} synthetic / non-308 SKUs from Firebase`,
        details: `Removed: ${deletedIds.slice(0, 6).join(', ')}${deletedIds.length > 6 ? '...' : ''}`,
      });
      notify(
        'ok',
        `Deleted ${syntheticProducts.length} old synthetic products from Firebase RTDB & Android Feed.`
      );
    } else {
      notify('err', 'Failed to delete synthetic products.');
    }
    setBusyAction('');
  };

  const handleRefreshAndroidFeed = async () => {
    setBusyAction('android-feed');
    const ok = await syncAndroidFeedSnapshot(products);
    if (ok) {
      const androidCount = products.filter((p) => p.channels?.androidApps !== false).length;
      const sample = {
        store: 'Nav Grocery Store & NavKart',
        updatedAt: new Date().toISOString(),
        totalItems: androidCount,
        items: products
          .filter((p) => p.channels?.androidApps !== false)
          .slice(0, 3)
          .map((p) => ({
            id: p.id,
            name: p.name,
            brand: p.brand,
            mrp: p.mrp,
            sale: p.sale,
            stock: p.stock,
            image: resolveProductImage(p),
            channels: p.channels || DEFAULT_CHANNELS,
          })),
      };
      setAndroidFeedPreview(JSON.stringify(sample, null, 2));
      notify('ok', `Android JSON Feed refreshed (${androidCount} items live at /android_feed.json).`);
    } else {
      notify('err', 'Failed to refresh Android JSON Feed.');
    }
    setBusyAction('');
  };

  const copyFeedUrl = async (url: string) => {
    try {
      await navigator.clipboard.writeText(url);
      setCopiedUrl(url);
      window.setTimeout(() => setCopiedUrl(null), 2000);
    } catch {
      notify('err', 'Clipboard access blocked. Copy the URL directly from the box.');
    }
  };

  const handleExportDatabaseCsv = () => {
    const dataset = products.length > 0 ? products : verified308;
    if (dataset.length === 0) {
      notify('err', 'No products available to export.');
      return;
    }
    const csv = exportCatalogToCsv(dataset);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `nav_grocery_products_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    notify(
      'ok',
      `Exported ${dataset.length} products (SKUs, names, MRPs, and stock status) to CSV.`
    );
  };

  if (!unlocked) {
    return <AdminPinGate expectedPin={ADMIN_PIN} onUnlock={handleUnlock} />;
  }

  const unifiedProductsFeedUrl = `${FIREBASE_RTDB_BASE}/products.json`;
  const androidDedicatedFeedUrl = `${FIREBASE_RTDB_BASE}/android_feed.json`;
  const androidFilteredQueryUrl = `${FIREBASE_RTDB_BASE}/products.json?orderBy=%22channels/androidApps%22&equalTo=true`;

  const navItems: Array<{ id: AdminTab; label: string; icon: React.ReactNode }> = [
    { id: 'catalog', label: 'Catalog & Channels', icon: <Layers className="w-4 h-4" /> },
    { id: 'add', label: 'Add Custom SKU', icon: <Plus className="w-4 h-4" /> },
    { id: 'bulkPhotos', label: 'Bulk Photo Uploader', icon: <Upload className="w-4 h-4" /> },
    { id: 'batchImport', label: 'Batch CSV Import', icon: <FileSpreadsheet className="w-4 h-4" /> },
    { id: 'qrLabels', label: 'SKU QR Codes', icon: <QrCode className="w-4 h-4" /> },
    { id: 'sync', label: '308 Sync & Android Feed', icon: <Smartphone className="w-4 h-4" /> },
    { id: 'activityLog', label: 'Activity Log', icon: <History className="w-4 h-4" /> },
  ];

  const activeLabel = navItems.find((n) => n.id === activeTab)?.label || 'Catalog';

  return (
    <div className="min-h-screen bg-[#FAF8F3] text-[#14221B] flex flex-col lg:flex-row">
      {/* Hidden Single SKU File Picker */}
      <input
        ref={singlePhotoInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={handleSinglePhotoFileChange}
      />

      {/* Sidebar Navigation */}
      <aside className="w-full lg:w-64 bg-[#0B3B24] text-white shrink-0 flex flex-col justify-between border-b lg:border-b-0 lg:border-r border-[#0B3B24] no-print">
        <div>
          <div className="px-5 py-5 border-b border-white/10">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-[#D4AF37] text-[#0B3B24] font-display font-bold flex items-center justify-center text-base shrink-0">
                N
              </div>
              <div className="min-w-0">
                <h1 className="font-display font-bold text-base text-white truncate">
                  Nav Admin Center
                </h1>
                <p className="text-[11px] text-[#D4AF37] truncate">
                  Sandhaur 148020 · Unified Sync
                </p>
              </div>
            </div>
          </div>

          <nav className="p-3 flex lg:flex-col gap-1.5 overflow-x-auto">
            {navItems.map((item) => {
              const active = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setActiveTab(item.id)}
                  className={`px-3.5 py-2.5 rounded-xl text-xs sm:text-sm font-semibold flex items-center gap-2.5 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                    active
                      ? 'bg-[#D4AF37] text-[#0B3B24]'
                      : 'text-white/85 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="hidden lg:block p-4 border-t border-white/10 space-y-2 text-xs text-white/80">
          <div className="flex items-center justify-between">
            <span>Firebase RTDB</span>
            <span className="font-mono-num font-bold text-[#D4AF37]">
              {firebaseConnected ? 'Connected' : 'Offline Fallback'}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span>Synced SKUs</span>
            <span className="font-mono-num font-bold text-white">{stats.total}</span>
          </div>
          <a
            href="/"
            className="mt-2 w-full py-2 px-3 rounded-lg bg-white/10 hover:bg-white/20 text-white font-semibold flex items-center justify-center gap-1.5 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Open Public Website</span>
          </a>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 min-w-0 flex flex-col">
        {/* Top Bar Contract */}
        <header className="bg-white border-b border-[#0B3B24]/12 px-4 sm:px-6 py-3.5 flex flex-wrap items-center justify-between gap-4 no-print">
          <div className="flex items-center gap-2 text-sm">
            <span className="text-[#526058] font-medium">Admin Control Center</span>
            <span className="text-[#526058]">/</span>
            <span className="font-bold text-[#0B3B24]">{activeLabel}</span>
          </div>

          <div className="flex items-center gap-2.5 shrink-0">
            <button
              type="button"
              onClick={loadAdminData}
              disabled={loading || Boolean(busyAction)}
              className="px-3.5 py-2 rounded-lg border border-[#0B3B24]/20 bg-white hover:bg-[#FAF8F3] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>Refresh Firebase</span>
            </button>
            <button
              type="button"
              onClick={handleLock}
              className="px-3.5 py-2 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
            >
              <Lock className="w-3.5 h-3.5 text-[#D4AF37]" />
              <span>Lock Admin</span>
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 sm:p-6 space-y-5 max-w-[1400px] w-full mx-auto">
          {/* Action Status Banner */}
          {statusBanner && (
            <div
              role="status"
              className={`px-4 py-3 rounded-xl border text-xs sm:text-sm font-semibold flex items-center justify-between gap-3 no-print ${
                statusBanner.type === 'ok'
                  ? 'bg-emerald-50 border-emerald-200 text-[#0B3B24]'
                  : 'bg-red-50 border-red-200 text-red-800'
              }`}
            >
              <span>{statusBanner.text}</span>
              <button
                type="button"
                onClick={() => setStatusBanner(null)}
                aria-label="Dismiss message"
                className="p-1 rounded hover:bg-black/5 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* 4-Column Summary Strip */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5 no-print">
            <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
              <div className="text-xs font-semibold text-[#526058]">Total Live SKUs</div>
              <div className="text-2xl font-bold text-[#0B3B24] font-mono-num mt-1">
                {stats.total}
              </div>
              <div className="text-[11px] text-[#526058] mt-1">
                Firebase RTDB `/products.json`
              </div>
            </div>
            <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
              <div className="text-xs font-semibold text-[#526058]">Verified 308 Packaging</div>
              <div className="text-2xl font-bold text-[#0B3B24] font-mono-num mt-1">
                {stats.verifiedCount}
                <span className="text-sm font-normal text-[#526058]"> / 308</span>
              </div>
              <div className="text-[11px] text-[#526058] mt-1">
                Bound to `/products/NAV001–308.jpg`
              </div>
            </div>
            <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
              <div className="text-xs font-semibold text-[#526058]">Custom Admin Photos</div>
              <div className="text-2xl font-bold text-[#0B3B24] font-mono-num mt-1">
                {stats.customPhotoCount}
              </div>
              <div className="text-[11px] text-[#526058] mt-1">
                Priority override active on storefront
              </div>
            </div>
            <div className="bg-white rounded-xl border border-[#0B3B24]/12 p-4">
              <div className="text-xs font-semibold text-[#526058]">Synthetic / Custom SKUs</div>
              <div
                className={`text-2xl font-bold font-mono-num mt-1 ${
                  stats.syntheticCount > 0 ? 'text-amber-700' : 'text-[#0B3B24]'
                }`}
              >
                {stats.syntheticCount}
              </div>
              <div className="text-[11px] text-[#526058] mt-1">
                {stats.outOfStockCount} items currently out of stock
              </div>
            </div>
          </div>

          {/* TAB 1: CATALOG & CHANNELS */}
          {activeTab === 'catalog' && (
            <section className="bg-white rounded-xl border border-[#0B3B24]/12 overflow-hidden">
              {/* Filter Bar */}
              <div className="p-4 border-b border-[#0B3B24]/10 space-y-3">
                <div className="flex flex-col xl:flex-row xl:items-center justify-between gap-3">
                  <div className="flex flex-col sm:flex-row sm:items-center gap-2.5 flex-1">
                    {/* Dedicated SKU ID Filter Input */}
                    <div className="relative w-full sm:w-64 shrink-0">
                      <Search className="w-4 h-4 text-[#0B3B24] absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        id="admin-sku-search"
                        type="text"
                        aria-label="Filter by SKU ID"
                        value={skuSearchQuery}
                        onChange={(e) => setSkuSearchQuery(e.target.value)}
                        placeholder="SKU ID (NAV081, 1-20, NAV005)"
                        className="w-full pl-9 pr-8 py-2 rounded-lg border border-[#0B3B24]/30 bg-white font-mono-num text-xs sm:text-sm font-semibold uppercase placeholder:normal-case placeholder:font-sans placeholder:font-normal focus:outline-none focus:border-[#0B3B24]"
                      />
                      {skuSearchQuery && (
                        <button
                          type="button"
                          onClick={() => setSkuSearchQuery('')}
                          aria-label="Clear SKU ID filter"
                          className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-[#526058] hover:text-[#14221B] cursor-pointer"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>

                    {/* Product Name / Brand / Category Search Input */}
                    <div className="relative flex-1 max-w-md">
                      <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="search"
                        aria-label="Search by product name, brand, or category"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Filter by product name, brand, or category..."
                        className="w-full pl-9 pr-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] text-sm focus:outline-none focus:border-[#0B3B24]"
                      />
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <select
                      id="admin-update-selected"
                      aria-label="Update Selected SKUs"
                      value=""
                      disabled={selectedSkus.size === 0 || Boolean(busyAction)}
                      onChange={(e) => {
                        const val = e.target.value;
                        e.target.value = '';
                        void handleUpdateSelectedDropdown(val);
                      }}
                      title={
                        selectedSkus.size === 0
                          ? 'Check 1 or more SKU rows below to bulk-update'
                          : `Bulk-update ${selectedSkus.size} selected SKU(s)`
                      }
                      className={`px-3 py-2 rounded-lg border text-xs font-bold focus:outline-none transition-colors ${
                        selectedSkus.size > 0
                          ? 'border-[#0B3B24] bg-[#D4AF37]/25 text-[#0B3B24] cursor-pointer'
                          : 'border-[#0B3B24]/20 bg-[#FAF8F3] text-[#526058] opacity-65 cursor-not-allowed'
                      }`}
                    >
                      <option value="">
                        {selectedSkus.size > 0
                          ? `Update Selected (${selectedSkus.size}) ▾`
                          : 'Update Selected (0)'}
                      </option>
                      <option value="mark_in_stock">Set In Stock (25 units)</option>
                      <option value="mark_out_of_stock">Set Out of Stock (0 units)</option>
                      <option value="toggle_stock">Toggle In Stock / Out of Stock</option>
                      <option value="enable_navGrocery">Enable: Nav Grocery Web</option>
                      <option value="disable_navGrocery">Disable: Nav Grocery Web</option>
                      <option value="enable_navKart">Enable: NavKart Web</option>
                      <option value="disable_navKart">Disable: NavKart Web</option>
                      <option value="enable_android">Enable: Android Apps Feed</option>
                      <option value="disable_android">Disable: Android Apps Feed</option>
                      <option value="clear_selection">Clear Selection</option>
                    </select>

                    <select
                      aria-label="Filter by Publishing Channel"
                      value={channelFilter}
                      onChange={(e) => setChannelFilter(e.target.value as ChannelFilter)}
                      className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white text-xs font-semibold text-[#14221B] focus:outline-none"
                    >
                      <option value="all">All Channels</option>
                      <option value="navGroceryWeb">Published: Nav Grocery Web</option>
                      <option value="navKartWeb">Published: NavKart Web</option>
                      <option value="androidApps">Published: Android Apps</option>
                    </select>

                    <button
                      type="button"
                      onClick={handleExportDatabaseCsv}
                      disabled={loading || (products.length === 0 && verified308.length === 0)}
                      className="px-3.5 py-2 rounded-lg border border-[#0B3B24]/25 bg-[#FAF8F3] hover:bg-[#EFECE2] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer disabled:opacity-45"
                    >
                      <Download className="w-3.5 h-3.5" />
                      <span>Export to CSV</span>
                    </button>

                    <button
                      type="button"
                      onClick={handleOneClickSync308}
                      disabled={Boolean(busyAction)}
                      className="px-3.5 py-2 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer disabled:opacity-50"
                    >
                      <Database className="w-3.5 h-3.5 text-[#D4AF37]" />
                      <span>
                        {busyAction === 'sync-308' ? 'Syncing 308 SKUs...' : 'One-Click Sync 308 SKUs'}
                      </span>
                    </button>
                  </div>
                </div>

                {/* Segmented Filter Tabs */}
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-1.5">
                    {(
                      [
                        { id: 'all', label: `All (${products.length})` },
                        { id: 'verified308', label: `Verified 308 (${stats.verifiedCount})` },
                        { id: 'synthetic', label: `Synthetic / Custom (${stats.syntheticCount})` },
                        { id: 'customPhoto', label: `Custom Photos (${stats.customPhotoCount})` },
                        { id: 'outOfStock', label: `Out of Stock (${stats.outOfStockCount})` },
                      ] as Array<{ id: CatalogFilter; label: string }>
                    ).map((tab) => (
                      <button
                        key={tab.id}
                        type="button"
                        onClick={() => setCatalogFilter(tab.id)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer whitespace-nowrap ${
                          catalogFilter === tab.id
                            ? 'bg-[#0B3B24] text-white'
                            : 'bg-[#FAF8F3] text-[#526058] hover:text-[#14221B] border border-[#0B3B24]/10'
                        }`}
                      >
                        {tab.label}
                      </button>
                    ))}
                  </div>
                  {(skuSearchQuery || searchQuery) && (
                    <span className="text-xs font-mono-num font-bold text-[#0B3B24]">
                      Matched: {filteredProducts.length} SKUs
                    </span>
                  )}
                </div>

                {/* Bulk Selection Action Bar */}
                {selectedSkus.size > 0 && (
                  <div className="p-3.5 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/25 flex flex-wrap items-center justify-between gap-3 text-xs">
                    <div className="space-y-0.5">
                      <div className="font-bold text-[#0B3B24] font-mono-num">
                        {selectedSkus.size} SKU(s) selected for bulk edit
                      </div>
                      <div className="text-[11px] text-[#526058] font-mono-num">
                        {selectedStockSummary.inStockCount} In Stock · {selectedStockSummary.outOfStockCount} Out of Stock
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center gap-2.5">
                      {/* Single In Stock / Out of Stock Bulk Toggle */}
                      <button
                        type="button"
                        role="switch"
                        aria-checked={selectedStockSummary.allInStock}
                        aria-label="Toggle selected products between In Stock and Out of Stock"
                        disabled={Boolean(busyAction)}
                        onClick={handleBulkToggleStockStatus}
                        className={`inline-flex items-center gap-2.5 px-3 py-1.5 rounded-lg border font-bold transition-colors cursor-pointer disabled:opacity-50 ${
                          selectedStockSummary.allInStock
                            ? 'bg-emerald-100 border-emerald-300 text-[#0B3B24]'
                            : 'bg-red-50 border-red-300 text-red-800'
                        }`}
                      >
                        <span
                          className={`relative inline-flex h-5 w-9 shrink-0 items-center rounded-full transition-colors ${
                            selectedStockSummary.allInStock ? 'bg-[#0B3B24]' : 'bg-red-600'
                          }`}
                        >
                          <span
                            className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${
                              selectedStockSummary.allInStock ? 'translate-x-4.5' : 'translate-x-1'
                            }`}
                          />
                        </span>
                        <span className="whitespace-nowrap">
                          {selectedStockSummary.allInStock
                            ? 'In Stock (Toggle → Out of Stock)'
                            : 'Out of Stock (Toggle → In Stock)'}
                        </span>
                      </button>

                      <div className="inline-flex items-center gap-1">
                        <input
                          type="number"
                          min={0}
                          aria-label="Bulk stock quantity"
                          value={bulkStockQty}
                          onChange={(e) => setBulkStockQty(e.target.value)}
                          className="w-16 px-2 py-1.5 rounded-lg border border-[#0B3B24]/25 bg-white font-mono-num font-bold text-xs"
                        />
                        <button
                          type="button"
                          disabled={Boolean(busyAction)}
                          onClick={() => handleBulkUpdateSelectedStock(Number(bulkStockQty) || 0)}
                          className="px-2.5 py-1.5 rounded-lg bg-[#0B3B24] text-white font-bold cursor-pointer disabled:opacity-50 whitespace-nowrap"
                        >
                          Set Qty
                        </button>
                      </div>

                      <button
                        type="button"
                        disabled={Boolean(busyAction)}
                        onClick={() => handleBulkSetChannelForSelected('navGroceryWeb', true)}
                        className="px-2.5 py-1.5 rounded-lg border border-[#0B3B24]/25 bg-white text-[#0B3B24] font-semibold cursor-pointer whitespace-nowrap"
                      >
                        Enable Nav Grocery
                      </button>

                      <button
                        type="button"
                        onClick={() => setSelectedSkus(new Set())}
                        className="px-2.5 py-1.5 rounded-lg text-[#526058] hover:text-[#14221B] font-semibold cursor-pointer whitespace-nowrap"
                      >
                        Clear Selection
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Data Grid */}
              {loading ? (
                <div className="p-8 text-center text-sm text-[#526058]">
                  Loading live inventory from Firebase RTDB...
                </div>
              ) : pagedProducts.length === 0 ? (
                <div className="p-10 text-center space-y-3">
                  <p className="text-sm font-semibold text-[#14221B]">
                    No products match this filter.
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setSkuSearchQuery('');
                      setSearchQuery('');
                      setCatalogFilter('all');
                      setChannelFilter('all');
                    }}
                    className="px-4 py-2 rounded-lg bg-[#0B3B24] text-white text-xs font-bold cursor-pointer"
                  >
                    Reset Catalog Filters
                  </button>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="bg-[#FAF8F3] border-b border-[#0B3B24]/12 text-[11px] font-bold uppercase tracking-wider text-[#526058]">
                        <th className="py-2.5 px-3 w-9">
                          <input
                            type="checkbox"
                            aria-label="Select all filtered SKUs"
                            checked={
                              filteredProducts.length > 0 &&
                              filteredProducts.every((p) => selectedSkus.has(p.id))
                            }
                            onChange={handleSelectAllFiltered}
                            className="accent-[#0B3B24] cursor-pointer"
                          />
                        </th>
                        <th className="py-2.5 px-3">Photo</th>
                        <th className="py-2.5 px-3">SKU &amp; Product</th>
                        <th className="py-2.5 px-3 text-right">MRP / 12% Off</th>
                        <th className="py-2.5 px-3 text-center">Stock Status</th>
                        <th className="py-2.5 px-3">Publishing Channels</th>
                        <th className="py-2.5 px-3 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#0B3B24]/8 text-xs">
                      {pagedProducts.map((product) => {
                        const isCustom = hasCustomAdminPhoto(product);
                        const ch = product.channels || DEFAULT_CHANNELS;
                        const inStock = Number(product.stock) > 0;
                        const imgUrl = resolveProductImage(product);
                        const isSelected = selectedSkus.has(product.id);

                        return (
                          <tr
                            key={product.id}
                            className={`transition-colors ${
                              isSelected ? 'bg-emerald-50/60' : 'hover:bg-[#FAF8F3]/70'
                            }`}
                          >
                            <td className="py-2.5 px-3">
                              <input
                                type="checkbox"
                                aria-label={`Select ${product.id}`}
                                checked={isSelected}
                                onChange={() => toggleSelectSku(product.id)}
                                className="accent-[#0B3B24] cursor-pointer"
                              />
                            </td>
                            <td className="py-2.5 px-3 w-20">
                              <div className="w-12 h-12 rounded-lg bg-white border border-[#0B3B24]/15 p-1 flex items-center justify-center overflow-hidden">
                                <img
                                  src={imgUrl}
                                  alt={product.name}
                                  className="w-full h-full object-contain"
                                  referrerPolicy="no-referrer"
                                  onError={(e) => {
                                    const el = e.currentTarget;
                                    if (!el.dataset.fb) {
                                      el.dataset.fb = '1';
                                      el.src = buildBrandPackageSvg(product);
                                    }
                                  }}
                                />
                              </div>
                              <div className="text-[10px] text-center font-mono-num text-[#526058] mt-0.5">
                                {isCustom ? 'Custom' : `/products`}
                              </div>
                            </td>

                            <td className="py-2.5 px-3 min-w-[230px]">
                              <div className="flex items-center gap-2">
                                <span className="font-mono-num font-bold text-[#0B3B24]">
                                  {product.id}
                                </span>
                                {!isVerifiedSkuFormat(product.id) && (
                                  <span className="text-[10px] font-bold text-amber-700">
                                    · Custom / Non-308
                                  </span>
                                )}
                              </div>
                              <div className="font-semibold text-[#14221B] text-sm leading-snug mt-0.5">
                                {product.name}
                              </div>
                              <div className="text-[11px] text-[#526058]">
                                {product.brand} · {product.department} · {product.category}
                              </div>
                            </td>

                            <td className="py-2.5 px-3 text-right font-mono-num whitespace-nowrap">
                              <div className="font-bold text-[#0B3B24] text-sm">
                                ₹{product.sale}
                              </div>
                              <div className="text-[11px] text-[#526058]">
                                MRP ₹{product.mrp}
                              </div>
                            </td>

                            <td className="py-2.5 px-3 text-center whitespace-nowrap">
                              <button
                                type="button"
                                onClick={() => handleQuickStockToggle(product)}
                                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold font-mono-num transition-colors cursor-pointer ${
                                  inStock
                                    ? 'bg-emerald-100 text-[#0B3B24] hover:bg-emerald-200'
                                    : 'bg-red-100 text-red-800 hover:bg-red-200'
                                }`}
                              >
                                {inStock ? `In Stock (${product.stock})` : 'Out of Stock'}
                              </button>
                            </td>

                            <td className="py-2.5 px-3">
                              <div className="flex flex-wrap gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => handleToggleChannel(product, 'navGroceryWeb')}
                                  className={`px-2 py-1 rounded text-[11px] font-semibold inline-flex items-center gap-1 cursor-pointer transition-colors ${
                                    ch.navGroceryWeb
                                      ? 'bg-[#0B3B24] text-white'
                                      : 'bg-neutral-100 text-neutral-500 line-through'
                                  }`}
                                >
                                  <Store className="w-3 h-3" />
                                  <span>Nav Grocery</span>
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleToggleChannel(product, 'navKartWeb')}
                                  className={`px-2 py-1 rounded text-[11px] font-semibold inline-flex items-center gap-1 cursor-pointer transition-colors ${
                                    ch.navKartWeb
                                      ? 'bg-[#D4AF37] text-[#0B3B24]'
                                      : 'bg-neutral-100 text-neutral-500 line-through'
                                  }`}
                                >
                                  <Globe className="w-3 h-3" />
                                  <span>NavKart</span>
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleToggleChannel(product, 'androidApps')}
                                  className={`px-2 py-1 rounded text-[11px] font-semibold inline-flex items-center gap-1 cursor-pointer transition-colors ${
                                    ch.androidApps
                                      ? 'bg-sky-700 text-white'
                                      : 'bg-neutral-100 text-neutral-500 line-through'
                                  }`}
                                >
                                  <Smartphone className="w-3 h-3" />
                                  <span>Android</span>
                                </button>
                              </div>
                            </td>

                            <td className="py-2.5 px-3 text-right whitespace-nowrap">
                              <div className="inline-flex items-center gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => triggerSinglePhotoReplace(product.id)}
                                  disabled={busyAction === `photo-${product.id}`}
                                  title="Replace SKU Photo"
                                  className="px-2 py-1.5 rounded-lg border border-[#0B3B24]/20 hover:bg-[#FAF8F3] text-[#0B3B24] font-semibold inline-flex items-center gap-1 cursor-pointer"
                                >
                                  <ImagePlus className="w-3.5 h-3.5" />
                                  <span>Photo</span>
                                </button>
                                {isCustom && (
                                  <button
                                    type="button"
                                    onClick={() => handleResetProductPhoto(product)}
                                    title="Revert to /products/NAVxxx.jpg"
                                    className="p-1.5 rounded-lg border border-amber-300 bg-amber-50 hover:bg-amber-100 text-amber-800 cursor-pointer"
                                  >
                                    <RotateCcw className="w-3.5 h-3.5" />
                                  </button>
                                )}
                                <button
                                  type="button"
                                  onClick={() => setQrProduct(product)}
                                  title={`View QR Code for ${product.id}`}
                                  className="px-2 py-1.5 rounded-lg border border-[#0B3B24]/20 hover:bg-[#FAF8F3] text-[#0B3B24] font-semibold inline-flex items-center gap-1 cursor-pointer"
                                >
                                  <QrCode className="w-3.5 h-3.5" />
                                  <span>QR</span>
                                </button>
                                <button
                                  type="button"
                                  onClick={() => setEditingProduct({ ...product })}
                                  title="Edit SKU Details"
                                  className="px-2 py-1.5 rounded-lg border border-[#0B3B24]/20 hover:bg-[#FAF8F3] text-[#14221B] font-semibold inline-flex items-center gap-1 cursor-pointer"
                                >
                                  <Pencil className="w-3.5 h-3.5" />
                                  <span>Edit</span>
                                </button>

                                {confirmDeleteId === product.id ? (
                                  <div className="inline-flex items-center gap-1">
                                    <button
                                      type="button"
                                      onClick={() => handleDeleteProduct(product.id)}
                                      disabled={busyAction === `del-${product.id}`}
                                      className="px-2 py-1.5 rounded-lg bg-red-700 hover:bg-red-800 text-white font-bold cursor-pointer"
                                    >
                                      Confirm
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => setConfirmDeleteId(null)}
                                      className="px-2 py-1.5 rounded-lg bg-neutral-200 text-[#14221B] font-semibold cursor-pointer"
                                    >
                                      Cancel
                                    </button>
                                  </div>
                                ) : (
                                  <button
                                    type="button"
                                    onClick={() => setConfirmDeleteId(product.id)}
                                    title="Delete SKU"
                                    className="p-1.5 rounded-lg border border-red-200 text-red-700 hover:bg-red-50 cursor-pointer"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="px-4 py-3 border-t border-[#0B3B24]/10 flex items-center justify-between text-xs">
                  <span className="text-[#526058] font-mono-num">
                    Showing {(page - 1) * PAGE_SIZE + 1}–
                    {Math.min(page * PAGE_SIZE, filteredProducts.length)} of{' '}
                    {filteredProducts.length}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={page <= 1}
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 font-semibold disabled:opacity-40 cursor-pointer"
                    >
                      Previous
                    </button>
                    <span className="font-mono-num font-bold">
                      Page {page} / {totalPages}
                    </span>
                    <button
                      type="button"
                      disabled={page >= totalPages}
                      onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                      className="px-3 py-1.5 rounded-lg border border-[#0B3B24]/20 font-semibold disabled:opacity-40 cursor-pointer"
                    >
                      Next
                    </button>
                  </div>
                </div>
              )}
            </section>
          )}

          {/* TAB 2: ADD CUSTOM SKU */}
          {activeTab === 'add' && (
            <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 sm:p-6 max-w-3xl">
              <div className="pb-4 border-b border-[#0B3B24]/10">
                <h2 className="font-display text-lg font-bold text-[#0B3B24]">
                  Add New Custom SKU &amp; Publish Across Channels
                </h2>
                <p className="text-xs text-[#526058] mt-0.5">
                  Flat 12% Wholesale Discount (Sale = round(MRP × 0.88)) is calculated automatically.
                </p>
              </div>

              <form onSubmit={handleCreateCustomSku} className="pt-5 space-y-4 text-xs sm:text-sm">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      SKU ID *
                    </label>
                    <input
                      type="text"
                      required
                      value={newSku}
                      onChange={(e) => setNewSku(e.target.value.toUpperCase())}
                      placeholder="e.g. NAV309 or NK101"
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 font-mono-num uppercase focus:outline-none focus:border-[#0B3B24]"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      Brand Name *
                    </label>
                    <input
                      type="text"
                      required
                      value={newBrand}
                      onChange={(e) => setNewBrand(e.target.value)}
                      placeholder="e.g. AMUL, VERKA, TATA"
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 focus:outline-none focus:border-[#0B3B24]"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      EAN / Barcode
                    </label>
                    <input
                      type="text"
                      value={newBarcode}
                      onChange={(e) => setNewBarcode(e.target.value)}
                      placeholder="Optional"
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 font-mono-num focus:outline-none focus:border-[#0B3B24]"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#526058] mb-1">
                    Product Title *
                  </label>
                  <input
                    type="text"
                    required
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    placeholder="e.g. Amul Pure Desi Ghee Tin 1L"
                    className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 focus:outline-none focus:border-[#0B3B24]"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      Department
                    </label>
                    <select
                      value={newDepartment}
                      onChange={(e) => setNewDepartment(e.target.value)}
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-white focus:outline-none focus:border-[#0B3B24]"
                    >
                      {DEPARTMENTS.map((d) => (
                        <option key={d} value={d}>
                          {d}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      Category
                    </label>
                    <input
                      type="text"
                      value={newCategory}
                      onChange={(e) => setNewCategory(e.target.value)}
                      placeholder="e.g. Desi Ghee & Oils"
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 focus:outline-none focus:border-[#0B3B24]"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      MRP (₹) *
                    </label>
                    <input
                      type="number"
                      min={1}
                      required
                      value={newMrp}
                      onChange={(e) => setNewMrp(e.target.value)}
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 font-mono-num focus:outline-none focus:border-[#0B3B24]"
                    />
                    <p className="text-[11px] font-semibold text-[#0B3B24] mt-1 font-mono-num">
                      12% Off Sale Price: ₹
                      {Math.round(Math.max(1, Number(newMrp) || 0) * 0.88)}
                    </p>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      Opening Stock Qty
                    </label>
                    <input
                      type="number"
                      min={0}
                      value={newStock}
                      onChange={(e) => setNewStock(e.target.value)}
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 font-mono-num focus:outline-none focus:border-[#0B3B24]"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-[#526058] mb-1">
                      GST Rate
                    </label>
                    <select
                      value={newGst}
                      onChange={(e) => setNewGst(e.target.value)}
                      className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-white focus:outline-none focus:border-[#0B3B24]"
                    >
                      <option value="0%">0% (Exempt)</option>
                      <option value="5%">5%</option>
                      <option value="12%">12%</option>
                      <option value="18%">18%</option>
                    </select>
                  </div>
                </div>

                {/* Publishing Channels */}
                <div className="p-3.5 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/15 space-y-2">
                  <div className="text-xs font-bold text-[#0B3B24]">
                    Simultaneous Publishing Channels
                  </div>
                  <div className="flex flex-wrap gap-4 text-xs font-semibold">
                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newChannels.navGroceryWeb}
                        onChange={(e) =>
                          setNewChannels((c) => ({ ...c, navGroceryWeb: e.target.checked }))
                        }
                        className="accent-[#0B3B24]"
                      />
                      <span>Nav Grocery Website (www.navgrocerystore.com)</span>
                    </label>
                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newChannels.navKartWeb}
                        onChange={(e) =>
                          setNewChannels((c) => ({ ...c, navKartWeb: e.target.checked }))
                        }
                        className="accent-[#0B3B24]"
                      />
                      <span>NavKart Global Store Website</span>
                    </label>
                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newChannels.androidApps}
                        onChange={(e) =>
                          setNewChannels((c) => ({ ...c, androidApps: e.target.checked }))
                        }
                        className="accent-[#0B3B24]"
                      />
                      <span>Android Apps JSON Feed</span>
                    </label>
                  </div>
                </div>

                {/* Optional Photo Upload */}
                <div className="flex flex-wrap items-center gap-4 pt-1">
                  <label className="px-4 py-2 rounded-lg border border-[#0B3B24]/25 bg-[#FAF8F3] hover:bg-[#EFECE2] text-xs font-bold text-[#0B3B24] inline-flex items-center gap-2 cursor-pointer">
                    <ImagePlus className="w-4 h-4" />
                    <span>Upload Product Photo (Auto-Compressed)</span>
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      className="hidden"
                      onChange={handleNewSkuPhotoPick}
                    />
                  </label>
                  {newPhotoInfo && (
                    <span className="text-xs font-mono-num text-[#0B3B24] font-semibold">
                      {newPhotoInfo}
                    </span>
                  )}
                  {newPhotoDataUrl && (
                    <img
                      src={newPhotoDataUrl}
                      alt="New SKU preview"
                      className="w-12 h-12 rounded-lg object-contain border border-[#0B3B24]/15 bg-white"
                    />
                  )}
                </div>

                <div className="pt-3 border-t border-[#0B3B24]/10 flex justify-end">
                  <button
                    type="submit"
                    disabled={busyAction === 'create-sku'}
                    className="px-5 py-2.5 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold text-sm cursor-pointer disabled:opacity-50"
                  >
                    {busyAction === 'create-sku'
                      ? 'Publishing to Firebase...'
                      : 'Publish SKU to Firebase & Android Feed'}
                  </button>
                </div>
              </form>
            </section>
          )}

          {/* TAB 3: BULK PHOTO UPLOADER */}
          {activeTab === 'bulkPhotos' && (
            <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 sm:p-6 space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-[#0B3B24]/10">
                <div>
                  <h2 className="font-display text-lg font-bold text-[#0B3B24]">
                    Bulk Photo Uploader by SKU ID (NAV001 – NAV308)
                  </h2>
                  <p className="text-xs text-[#526058] mt-0.5">
                    Select multiple photos named e.g. NAV001.jpg, NAV002.png. Images are compressed to ≤32 KB JPEG and synced as custom overrides to Firebase RTDB.
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <input
                    ref={bulkFileInputRef}
                    type="file"
                    multiple
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={(e) => {
                      handleBulkFilesSelected(e.target.files);
                      e.target.value = '';
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => bulkFileInputRef.current?.click()}
                    className="px-4 py-2 rounded-lg border border-[#0B3B24]/25 bg-[#FAF8F3] hover:bg-[#EFECE2] text-xs font-bold text-[#0B3B24] inline-flex items-center gap-1.5 cursor-pointer"
                  >
                    <Upload className="w-4 h-4" />
                    <span>Select SKU Photos</span>
                  </button>
                  <button
                    type="button"
                    disabled={bulkQueue.length === 0 || Boolean(busyAction)}
                    onClick={handleBulkSyncAll}
                    className="px-4 py-2 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-45"
                  >
                    <Database className="w-4 h-4 text-[#D4AF37]" />
                    <span>Sync Queued Photos to Firebase</span>
                  </button>
                </div>
              </div>

              {/* Drop Zone */}
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  handleBulkFilesSelected(e.dataTransfer.files);
                }}
                onClick={() => bulkFileInputRef.current?.click()}
                className="border-2 border-dashed border-[#0B3B24]/25 rounded-xl p-8 text-center bg-[#FAF8F3] hover:bg-[#F3EFE4] transition-colors cursor-pointer"
              >
                <Upload className="w-7 h-7 text-[#0B3B24] mx-auto mb-2" />
                <p className="text-sm font-bold text-[#14221B]">
                  Drop SKU product photos here or click to browse
                </p>
                <p className="text-xs text-[#526058] mt-1">
                  Auto-matches filenames like NAV001.jpg through NAV308.jpg · Automatic canvas compression (400×400px JPEG)
                </p>
              </div>

              {bulkProgress && (
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs font-mono-num font-bold text-[#0B3B24]">
                    <span>Syncing compressed photos to Firebase RTDB...</span>
                    <span>
                      {bulkProgress.done} / {bulkProgress.total}
                    </span>
                  </div>
                  <div className="w-full h-2.5 rounded-full bg-[#FAF8F3] overflow-hidden border border-[#0B3B24]/15">
                    <div
                      className="h-full bg-[#0B3B24] transition-all"
                      style={{
                        width: `${Math.round((bulkProgress.done / bulkProgress.total) * 100)}%`,
                      }}
                    />
                  </div>
                </div>
              )}

              {bulkQueue.length > 0 && (
                <div className="space-y-3">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-[#14221B] font-mono-num">
                      Queued Files: {bulkQueue.length}
                    </span>
                    <button
                      type="button"
                      onClick={() => setBulkQueue([])}
                      className="text-xs font-semibold text-red-700 hover:underline cursor-pointer"
                    >
                      Clear Queue
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {bulkQueue.map((item) => {
                      const matchedProduct = products.find(
                        (p) => p.id === item.targetSku.trim().toUpperCase()
                      );
                      return (
                        <div
                          key={item.id}
                          className="p-3 rounded-xl border border-[#0B3B24]/15 bg-[#FAF8F3] flex items-center gap-3"
                        >
                          {item.dataUrl ? (
                            <img
                              src={item.dataUrl}
                              alt={item.fileName}
                              className="w-12 h-12 rounded-lg object-contain bg-white border border-[#0B3B24]/10 shrink-0"
                            />
                          ) : (
                            <div className="w-12 h-12 rounded-lg bg-red-100 shrink-0" />
                          )}
                          <div className="min-w-0 flex-1 space-y-1">
                            <div className="flex items-center justify-between gap-2">
                              <input
                                type="text"
                                value={item.targetSku}
                                onChange={(e) => {
                                  const val = e.target.value.toUpperCase();
                                  setBulkQueue((prev) =>
                                    prev.map((q) =>
                                      q.id === item.id ? { ...q, targetSku: val } : q
                                    )
                                  );
                                }}
                                placeholder="NAV001"
                                className="w-24 px-2 py-0.5 rounded border border-[#0B3B24]/25 bg-white font-mono-num text-xs font-bold uppercase"
                              />
                              <span
                                className={`text-[11px] font-bold ${
                                  item.status === 'synced'
                                    ? 'text-emerald-700'
                                    : item.status === 'error'
                                    ? 'text-red-700'
                                    : 'text-[#526058]'
                                }`}
                              >
                                {item.status === 'synced'
                                  ? 'Synced'
                                  : item.status === 'error'
                                  ? item.errorMsg || 'Error'
                                  : 'Ready'}
                              </span>
                            </div>
                            <p className="text-[11px] text-[#14221B] truncate">
                              {matchedProduct ? matchedProduct.name : item.fileName}
                            </p>
                            <p className="text-[10px] font-mono-num text-[#526058]">
                              {formatKb(item.beforeBytes)} → {formatKb(item.afterBytes)}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </section>
          )}

          {/* TAB 4: BATCH CSV IMPORT */}
          {activeTab === 'batchImport' && (
            <BatchCsvImportPanel
              products={products}
              verified308={verified308}
              onSyncedProducts={setProducts}
              onNotify={notify}
              onLogActivity={logProductActivity}
            />
          )}

          {/* TAB 5: 308 SYNC & ANDROID FEED */}
          {activeTab === 'sync' && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              {/* Left: One-Click Sync & Synthetic Cleanup */}
              <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 sm:p-6 space-y-5">
                <div className="pb-4 border-b border-[#0B3B24]/10">
                  <h2 className="font-display text-lg font-bold text-[#0B3B24]">
                    One-Click 308 Verified SKU Sync &amp; Cleanup
                  </h2>
                  <p className="text-xs text-[#526058] mt-1">
                    Pushes all 308 verified SKUs (NAV001–NAV308) bound to `/products/NAV001.jpg`–`/products/NAV308.jpg` and flat 12% MRP wholesale pricing simultaneously to Firebase RTDB and the Android JSON Feed.
                  </p>
                </div>

                <label className="flex items-start gap-2.5 text-xs text-[#14221B] bg-[#FAF8F3] p-3.5 rounded-xl border border-[#0B3B24]/12 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={preserveCustomPhotosOnSync}
                    onChange={(e) => setPreserveCustomPhotosOnSync(e.target.checked)}
                    className="mt-0.5 accent-[#0B3B24]"
                  />
                  <span>
                    <strong className="block text-[#0B3B24]">
                      Preserve custom Admin replacement photos during 308 Sync
                    </strong>
                    Uncheck if you want to clear all custom uploaded images and force every SKU back to `/products/NAV001.jpg`–`/products/NAV308.jpg`.
                  </span>
                </label>

                <button
                  type="button"
                  disabled={Boolean(busyAction)}
                  onClick={handleOneClickSync308}
                  className="w-full py-3 px-4 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold text-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                >
                  <Database className="w-4 h-4 text-[#D4AF37]" />
                  <span>
                    {busyAction === 'sync-308'
                      ? 'Syncing All 308 SKUs to Firebase...'
                      : 'One-Click Sync All 308 Verified SKUs + Android Feed'}
                  </span>
                </button>

                <div className="pt-4 border-t border-[#0B3B24]/10 space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-[#14221B]">
                        Synthetic / Non-308 Product Cleanup
                      </h3>
                      <p className="text-xs text-[#526058]">
                        Detected {syntheticProducts.length} non-308 / custom records in Firebase RTDB.
                      </p>
                    </div>
                    <button
                      type="button"
                      disabled={syntheticProducts.length === 0 || Boolean(busyAction)}
                      onClick={handleDeleteAllSynthetic}
                      className="px-3.5 py-2 rounded-lg bg-red-700 hover:bg-red-800 text-white text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-40"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>Delete Old Synthetic ({syntheticProducts.length})</span>
                    </button>
                  </div>

                  {syntheticProducts.length > 0 && (
                    <div className="max-h-44 overflow-y-auto rounded-lg border border-[#0B3B24]/12 divide-y divide-[#0B3B24]/8 text-xs">
                      {syntheticProducts.map((sp) => (
                        <div
                          key={sp.id}
                          className="px-3 py-2 flex items-center justify-between gap-2 bg-[#FAF8F3]"
                        >
                          <div className="min-w-0">
                            <span className="font-mono-num font-bold text-amber-800 mr-2">
                              {sp.id}
                            </span>
                            <span className="font-medium truncate">{sp.name}</span>
                          </div>
                          <button
                            type="button"
                            onClick={() => handleDeleteProduct(sp.id)}
                            className="text-red-700 font-bold hover:underline shrink-0 cursor-pointer"
                          >
                            Remove
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </section>

              {/* Right: Live Android App REST API JSON Feed */}
              <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 sm:p-6 space-y-4">
                <div className="pb-4 border-b border-[#0B3B24]/10 flex items-center justify-between gap-2">
                  <div>
                    <h2 className="font-display text-lg font-bold text-[#0B3B24]">
                      Live Android App REST API JSON Feed
                    </h2>
                    <p className="text-xs text-[#526058] mt-0.5">
                      Use these live Firebase RTDB REST endpoints directly in Retrofit / Kotlin / Jetpack Compose Android apps.
                    </p>
                  </div>
                  <button
                    type="button"
                    disabled={Boolean(busyAction)}
                    onClick={handleRefreshAndroidFeed}
                    className="px-3 py-1.5 rounded-lg bg-[#D4AF37] text-[#0B3B24] text-xs font-bold whitespace-nowrap cursor-pointer hover:bg-[#c5a12f]"
                  >
                    {busyAction === 'android-feed' ? 'Refreshing...' : 'Refresh Snapshot'}
                  </button>
                </div>

                {[
                  {
                    label: 'Dedicated Android Feed Snapshot (Recommended for Mobile Apps)',
                    url: androidDedicatedFeedUrl,
                  },
                  {
                    label: 'Unified Firebase RTDB Products Endpoint (Web + NavKart + Android)',
                    url: unifiedProductsFeedUrl,
                  },
                  {
                    label: 'Filtered Android-Only Query URL (channels/androidApps == true)',
                    url: androidFilteredQueryUrl,
                  },
                ].map((ep) => (
                  <div
                    key={ep.url}
                    className="p-3.5 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/15 space-y-1.5"
                  >
                    <div className="text-[11px] font-bold text-[#0B3B24]">{ep.label}</div>
                    <div className="flex items-center gap-2">
                      <code className="flex-1 text-[11px] font-mono-num bg-white px-2.5 py-2 rounded-lg border border-[#0B3B24]/15 text-[#14221B] overflow-x-auto whitespace-nowrap">
                        {ep.url}
                      </code>
                      <button
                        type="button"
                        onClick={() => copyFeedUrl(ep.url)}
                        className="px-2.5 py-2 rounded-lg bg-[#0B3B24] text-white text-xs font-bold inline-flex items-center gap-1 shrink-0 cursor-pointer"
                      >
                        {copiedUrl === ep.url ? (
                          <>
                            <Check className="w-3.5 h-3.5 text-[#D4AF37]" />
                            <span>Copied</span>
                          </>
                        ) : (
                          <>
                            <Copy className="w-3.5 h-3.5" />
                            <span>Copy</span>
                          </>
                        )}
                      </button>
                      <a
                        href={ep.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        aria-label={`Open ${ep.label}`}
                        className="p-2 rounded-lg border border-[#0B3B24]/20 bg-white text-[#0B3B24] hover:bg-[#EFECE2] shrink-0"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  </div>
                ))}

                {androidFeedPreview && (
                  <div className="space-y-1.5 pt-2">
                    <div className="text-xs font-bold text-[#526058]">
                      Live Android JSON Payload Sample:
                    </div>
                    <pre className="p-3 rounded-xl bg-[#14221B] text-[#FAF8F3] text-[11px] font-mono-num overflow-x-auto max-h-56">
                      {androidFeedPreview}
                    </pre>
                  </div>
                )}
              </section>
            </div>
          )}
          {/* TAB 5: SKU QR CODES & PRINTABLE LABELS */}
          {activeTab === 'qrLabels' && (
            <SkuQrLabelsPanel
              products={products}
              selectedSkus={selectedSkus}
              onOpenQrModal={(item) => setQrProduct(item)}
              onOpenManagePanel={(item) => setEditingProduct({ ...item })}
              onNotify={notify}
            />
          )}

          {/* TAB 6: ACTIVITY LOG */}
          {activeTab === 'activityLog' && (
            <ActivityLogPanel
              logs={activityLogs}
              loading={logsLoading}
              operatorId={operatorId}
              onChangeOperatorId={(nextId) => {
                const clean = saveOperatorId(nextId);
                setOperatorId(clean);
                notify('ok', `Activity log operator tag set to "${clean}".`);
              }}
              onRefresh={refreshLogs}
            />
          )}
        </main>
      </div>

      {/* SKU QR Code Modal */}
      <SkuQrModal
        product={qrProduct}
        onClose={() => setQrProduct(null)}
        onOpenManagePanel={(item) => setEditingProduct({ ...item })}
        onNotify={notify}
      />

      {/* Edit SKU Modal */}
      {editingProduct && (
        <div
          className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 no-print"
          onClick={() => setEditingProduct(null)}
        >
          <div
            className="bg-white rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl border border-[#0B3B24]/20"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="px-5 py-3.5 bg-[#0B3B24] text-white flex items-center justify-between">
              <div className="text-sm font-bold font-mono-num">
                Edit SKU · {editingProduct.id}
              </div>
              <button
                type="button"
                onClick={() => setEditingProduct(null)}
                className="p-1 rounded hover:bg-white/15 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveEditProduct} className="p-5 space-y-4 text-xs sm:text-sm">
              <div>
                <label className="block text-xs font-bold text-[#526058] mb-1">
                  Product Name
                </label>
                <input
                  type="text"
                  required
                  value={editingProduct.name}
                  onChange={(e) =>
                    setEditingProduct({ ...editingProduct, name: e.target.value })
                  }
                  className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 focus:outline-none focus:border-[#0B3B24]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#526058] mb-1">
                    Brand
                  </label>
                  <input
                    type="text"
                    required
                    value={editingProduct.brand}
                    onChange={(e) =>
                      setEditingProduct({ ...editingProduct, brand: e.target.value })
                    }
                    className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 focus:outline-none focus:border-[#0B3B24]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#526058] mb-1">
                    Category
                  </label>
                  <input
                    type="text"
                    required
                    value={editingProduct.category}
                    onChange={(e) =>
                      setEditingProduct({ ...editingProduct, category: e.target.value })
                    }
                    className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 focus:outline-none focus:border-[#0B3B24]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#526058] mb-1">
                    MRP (₹)
                  </label>
                  <input
                    type="number"
                    min={1}
                    required
                    value={editingProduct.mrp}
                    onChange={(e) => {
                      const mrp = Math.max(1, Math.round(Number(e.target.value) || 0));
                      setEditingProduct({
                        ...editingProduct,
                        mrp,
                        sale: Math.round(mrp * 0.88),
                        price: Math.round(mrp * 0.88),
                      });
                    }}
                    className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 font-mono-num focus:outline-none focus:border-[#0B3B24]"
                  />
                  <p className="text-[11px] font-bold text-[#0B3B24] mt-1 font-mono-num">
                    12% Off Sale: ₹{Math.round((Number(editingProduct.mrp) || 1) * 0.88)}
                  </p>
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#526058] mb-1">
                    Stock Qty
                  </label>
                  <input
                    type="number"
                    min={0}
                    required
                    value={editingProduct.stock}
                    onChange={(e) =>
                      setEditingProduct({
                        ...editingProduct,
                        stock: Math.max(0, Math.round(Number(e.target.value) || 0)),
                      })
                    }
                    className="w-full px-3 py-2 rounded-lg border border-[#0B3B24]/25 font-mono-num focus:outline-none focus:border-[#0B3B24]"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/15">
                <span className="text-xs font-semibold text-[#526058]">
                  Replace or Revert Photo
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => triggerSinglePhotoReplace(editingProduct.id)}
                    className="px-3 py-1.5 rounded-lg bg-[#0B3B24] text-white text-xs font-bold cursor-pointer"
                  >
                    Upload New Photo
                  </button>
                  {hasCustomAdminPhoto(editingProduct) && (
                    <button
                      type="button"
                      onClick={() => handleResetProductPhoto(editingProduct)}
                      className="px-3 py-1.5 rounded-lg border border-amber-300 bg-amber-50 text-amber-800 text-xs font-bold cursor-pointer"
                    >
                      Revert
                    </button>
                  )}
                </div>
              </div>

              <div className="pt-3 border-t border-[#0B3B24]/10 flex items-center justify-between gap-2">
                <button
                  type="button"
                  onClick={() => {
                    const current = { ...editingProduct };
                    setEditingProduct(null);
                    setQrProduct(current);
                  }}
                  className="px-3.5 py-2 rounded-lg border border-[#0B3B24]/25 bg-[#FAF8F3] hover:bg-[#EFECE2] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer"
                >
                  <QrCode className="w-3.5 h-3.5" />
                  <span>SKU QR Code</span>
                </button>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setEditingProduct(null)}
                    className="px-4 py-2 rounded-lg border border-[#0B3B24]/20 text-xs font-bold cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={busyAction === 'save-edit'}
                    className="px-5 py-2 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold cursor-pointer disabled:opacity-50"
                  >
                    {busyAction === 'save-edit' ? 'Saving...' : 'Save & Sync Firebase'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
