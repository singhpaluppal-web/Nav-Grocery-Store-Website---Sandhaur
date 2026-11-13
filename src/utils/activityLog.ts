import { FIREBASE_RTDB_BASE } from './firebase';

export type ActivityActionType =
  | 'price_update'
  | 'stock_toggle'
  | 'bulk_stock'
  | 'channel_toggle'
  | 'photo_update'
  | 'sku_create'
  | 'sku_edit'
  | 'sku_delete'
  | 'bulk_import'
  | 'bulk_sync';

export interface AdminActivityEntry {
  id: string;
  timestamp: string;
  actor: string;
  actionType: ActivityActionType;
  sku: string;
  summary: string;
  details?: string;
}

const LOG_STORAGE_KEY = 'nav_admin_activity_log_v1';
const OPERATOR_STORAGE_KEY = 'nav_admin_operator_id_v1';
const MAX_LOCAL_LOGS = 250;

export const ACTION_TYPE_LABELS: Record<ActivityActionType, string> = {
  price_update: 'Price Update',
  stock_toggle: 'Stock Toggle',
  bulk_stock: 'Bulk Stock Edit',
  channel_toggle: 'Channel Toggle',
  photo_update: 'Photo Update',
  sku_create: 'New SKU',
  sku_edit: 'Product Edit',
  sku_delete: 'SKU Delete',
  bulk_import: 'CSV Bulk Import',
  bulk_sync: '308 Catalog Sync',
};

export function getSavedOperatorId(): string {
  try {
    const saved = localStorage.getItem(OPERATOR_STORAGE_KEY);
    if (saved && saved.trim()) return saved.trim();
  } catch {
    // ignore storage errors
  }
  return 'Store Admin (PIN 148020)';
}

export function saveOperatorId(operatorId: string): string {
  const clean = operatorId.trim() || 'Store Admin (PIN 148020)';
  try {
    localStorage.setItem(OPERATOR_STORAGE_KEY, clean);
  } catch {
    // ignore
  }
  return clean;
}

export function getLocalActivityLogs(): AdminActivityEntry[] {
  try {
    const raw = localStorage.getItem(LOG_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((e): e is AdminActivityEntry => Boolean(e && e.id && e.timestamp))
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
  } catch {
    return [];
  }
}

function persistLocalLogs(entries: AdminActivityEntry[]): void {
  try {
    const sorted = [...entries]
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp))
      .slice(0, MAX_LOCAL_LOGS);
    localStorage.setItem(LOG_STORAGE_KEY, JSON.stringify(sorted));
  } catch {
    // ignore quota errors
  }
}

export async function recordActivityEntry(
  input: Omit<AdminActivityEntry, 'id' | 'timestamp' | 'actor'> & { actor?: string }
): Promise<AdminActivityEntry> {
  const now = new Date().toISOString();
  const safeId = `ACT_${Date.now()}_${Math.random().toString(36).slice(2, 7).toUpperCase()}`;
  const entry: AdminActivityEntry = {
    id: safeId,
    timestamp: now,
    actor: (input.actor || getSavedOperatorId()).trim(),
    actionType: input.actionType,
    sku: input.sku,
    summary: input.summary,
    details: input.details,
  };

  const existing = getLocalActivityLogs();
  persistLocalLogs([entry, ...existing]);

  try {
    await fetch(`${FIREBASE_RTDB_BASE}/admin_activity_log/${encodeURIComponent(safeId)}.json`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(entry),
    });
  } catch {
    // Local log remains persisted even if network fails temporarily
  }

  return entry;
}

export async function fetchActivityLogs(): Promise<AdminActivityEntry[]> {
  const local = getLocalActivityLogs();
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 6000);
    const res = await fetch(`${FIREBASE_RTDB_BASE}/admin_activity_log.json`, {
      signal: controller.signal,
      cache: 'no-store',
    });
    clearTimeout(timer);
    if (!res.ok) return local;
    const raw = await res.json();
    if (!raw || typeof raw !== 'object') return local;

    const remoteList: AdminActivityEntry[] = (
      Array.isArray(raw) ? raw : Object.values(raw)
    ).filter((e): e is AdminActivityEntry => Boolean(e && typeof e === 'object' && e.id && e.timestamp));

    const mergedById = new Map<string, AdminActivityEntry>();
    for (const item of local) mergedById.set(item.id, item);
    for (const item of remoteList) mergedById.set(item.id, item);

    const sorted = Array.from(mergedById.values())
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp))
      .slice(0, MAX_LOCAL_LOGS);

    persistLocalLogs(sorted);
    return sorted;
  } catch {
    return local;
  }
}

export function exportActivityLogsCsv(entries: AdminActivityEntry[]): string {
  const esc = (v: unknown) => {
    const s = String(v ?? '');
    return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const header = ['timestamp_iso', 'operator_id', 'action_type', 'sku', 'summary', 'details'].join(',');
  const lines = entries.map((e) =>
    [
      e.timestamp,
      e.actor,
      ACTION_TYPE_LABELS[e.actionType] || e.actionType,
      e.sku,
      e.summary,
      e.details || '',
    ]
      .map(esc)
      .join(',')
  );
  return '\uFEFF' + [header, ...lines].join('\n') + '\n';
}
