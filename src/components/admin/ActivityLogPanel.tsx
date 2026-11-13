import React, { useMemo, useState } from 'react';
import {
  Download,
  History,
  RefreshCw,
  Search,
  UserCheck,
  X,
} from 'lucide-react';
import {
  ACTION_TYPE_LABELS,
  ActivityActionType,
  AdminActivityEntry,
  exportActivityLogsCsv,
} from '../../utils/activityLog';

interface ActivityLogPanelProps {
  logs: AdminActivityEntry[];
  loading: boolean;
  operatorId: string;
  onChangeOperatorId: (nextId: string) => void;
  onRefresh: () => void;
}

function formatLocalTimestamp(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  });
}

function getActionBadgeStyle(type: ActivityActionType): string {
  switch (type) {
    case 'price_update':
      return 'bg-amber-50 text-amber-800 border-amber-200';
    case 'stock_toggle':
    case 'bulk_stock':
      return 'bg-emerald-50 text-[#0B3B24] border-emerald-200';
    case 'bulk_import':
    case 'bulk_sync':
      return 'bg-sky-50 text-sky-800 border-sky-200';
    case 'sku_delete':
      return 'bg-red-50 text-red-800 border-red-200';
    default:
      return 'bg-[#FAF8F3] text-[#14221B] border-[#0B3B24]/20';
  }
}

export const ActivityLogPanel: React.FC<ActivityLogPanelProps> = ({
  logs,
  loading,
  operatorId,
  onChangeOperatorId,
  onRefresh,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [actionFilter, setActionFilter] = useState<'all' | ActivityActionType>('all');
  const [draftOperator, setDraftOperator] = useState(operatorId);
  const [operatorSaved, setOperatorSaved] = useState(false);

  const filteredLogs = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return logs.filter((entry) => {
      if (actionFilter !== 'all' && entry.actionType !== actionFilter) return false;
      if (!q) return true;
      const hay = `${entry.sku} ${entry.actor} ${entry.summary} ${entry.details || ''} ${
        ACTION_TYPE_LABELS[entry.actionType] || ''
      }`.toLowerCase();
      return q.split(/\s+/).every((tk) => hay.includes(tk));
    });
  }, [logs, searchQuery, actionFilter]);

  const handleSaveOperator = (e: React.FormEvent) => {
    e.preventDefault();
    onChangeOperatorId(draftOperator);
    setOperatorSaved(true);
    window.setTimeout(() => setOperatorSaved(false), 1800);
  };

  const handleDownloadCsv = () => {
    if (filteredLogs.length === 0) return;
    const csv = exportActivityLogsCsv(filteredLogs);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `nav_admin_activity_log_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <section className="bg-white rounded-xl border border-[#0B3B24]/12 overflow-hidden">
      {/* Header & Operator Identity */}
      <div className="p-4 sm:p-5 border-b border-[#0B3B24]/10 space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <h2 className="font-display text-lg font-bold text-[#0B3B24]">
              Product Data Activity Log
            </h2>
            <p className="text-xs text-[#526058] mt-0.5">
              Audit trail of all price updates, stock status toggles, channel edits, photo replacements, and CSV bulk imports synced with Firebase RTDB.
            </p>
          </div>

          {/* Operator Identifier Form */}
          <form
            onSubmit={handleSaveOperator}
            className="flex flex-wrap items-center gap-2 bg-[#FAF8F3] px-3 py-2 rounded-xl border border-[#0B3B24]/15"
          >
            <UserCheck className="w-4 h-4 text-[#0B3B24] shrink-0" />
            <label
              htmlFor="admin-operator-id"
              className="text-xs font-bold text-[#526058] whitespace-nowrap"
            >
              Operator ID:
            </label>
            <input
              id="admin-operator-id"
              type="text"
              value={draftOperator}
              onChange={(e) => setDraftOperator(e.target.value)}
              placeholder="Store Admin (PIN 148020)"
              className="w-48 px-2.5 py-1 rounded-lg border border-[#0B3B24]/25 bg-white text-xs font-semibold text-[#14221B] focus:outline-none focus:border-[#0B3B24]"
            />
            <button
              type="submit"
              className="px-2.5 py-1 rounded-lg bg-[#0B3B24] text-white text-xs font-bold cursor-pointer hover:bg-[#082B1A] whitespace-nowrap"
            >
              {operatorSaved ? 'Saved' : 'Set Tag'}
            </button>
          </form>
        </div>

        {/* Search, Action Filter & Export */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div className="flex flex-col sm:flex-row sm:items-center gap-2.5 flex-1">
            <div className="relative flex-1 max-w-md">
              <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                aria-label="Filter activity log"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search SKU (NAV001), summary, or operator..."
                className="w-full pl-9 pr-8 py-2 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] text-xs sm:text-sm focus:outline-none focus:border-[#0B3B24]"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear log filter"
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-[#526058] hover:text-[#14221B] cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            <select
              aria-label="Filter by Action Type"
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value as 'all' | ActivityActionType)}
              className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white text-xs font-semibold text-[#14221B] focus:outline-none"
            >
              <option value="all">All Actions ({logs.length})</option>
              {(Object.keys(ACTION_TYPE_LABELS) as ActivityActionType[]).map((key) => (
                <option key={key} value={key}>
                  {ACTION_TYPE_LABELS[key]}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={handleDownloadCsv}
              disabled={filteredLogs.length === 0}
              className="px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-[#FAF8F3] hover:bg-[#EFECE2] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-45 whitespace-nowrap"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export Log CSV</span>
            </button>
            <button
              type="button"
              onClick={onRefresh}
              disabled={loading}
              className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white hover:bg-[#FAF8F3] text-[#14221B] text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-45 whitespace-nowrap"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>
        </div>
      </div>

      {/* Audit Table */}
      {loading && logs.length === 0 ? (
        <div className="p-8 text-center text-sm text-[#526058]">
          Loading activity log from Firebase RTDB...
        </div>
      ) : filteredLogs.length === 0 ? (
        <div className="p-10 text-center space-y-2">
          <History className="w-8 h-8 text-[#526058]/60 mx-auto" />
          <p className="text-sm font-semibold text-[#14221B]">
            {logs.length === 0
              ? 'No product edits recorded yet in this log.'
              : 'No activity entries match your filter.'}
          </p>
          <p className="text-xs text-[#526058]">
            Any price edit, stock toggle, channel switch, photo replacement, or CSV import will appear here automatically with timestamp and operator ID.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-[#FAF8F3] border-b border-[#0B3B24]/12 text-[11px] font-bold uppercase tracking-wider text-[#526058]">
                <th className="py-2.5 px-3">Timestamp</th>
                <th className="py-2.5 px-3">User / Operator</th>
                <th className="py-2.5 px-3">Action</th>
                <th className="py-2.5 px-3">SKU / Target</th>
                <th className="py-2.5 px-3">Change Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#0B3B24]/8 text-xs">
              {filteredLogs.map((entry) => (
                <tr key={entry.id} className="hover:bg-[#FAF8F3]/70 transition-colors">
                  <td className="py-2.5 px-3 font-mono-num text-[11px] text-[#526058] whitespace-nowrap">
                    {formatLocalTimestamp(entry.timestamp)}
                  </td>
                  <td className="py-2.5 px-3 font-semibold text-[#14221B] whitespace-nowrap">
                    {entry.actor}
                  </td>
                  <td className="py-2.5 px-3 whitespace-nowrap">
                    <span
                      className={`inline-block px-2 py-0.5 rounded border text-[11px] font-bold ${getActionBadgeStyle(
                        entry.actionType
                      )}`}
                    >
                      {ACTION_TYPE_LABELS[entry.actionType] || entry.actionType}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 font-mono-num font-bold text-[#0B3B24] whitespace-nowrap">
                    {entry.sku}
                  </td>
                  <td className="py-2.5 px-3 min-w-[260px]">
                    <div className="font-semibold text-[#14221B]">{entry.summary}</div>
                    {entry.details && (
                      <div className="text-[11px] text-[#526058] mt-0.5">{entry.details}</div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
};
