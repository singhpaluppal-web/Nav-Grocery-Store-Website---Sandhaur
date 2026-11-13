import React, { useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Database,
  Download,
  FileSpreadsheet,
  Trash2,
  Upload,
} from 'lucide-react';
import { Product, PublishingChannels } from '../../types';
import {
  buildSampleCsvTemplate,
  buildUpdatedProductFromCsvRow,
  CsvImportRow,
  exportCatalogToCsv,
  parseCatalogCsv,
} from '../../utils/csvCatalog';
import {
  patchProductsInFirebase,
  syncAndroidFeedSnapshot,
} from '../../utils/firebase';

interface BatchCsvImportPanelProps {
  products: Product[];
  verified308: Product[];
  onSyncedProducts: (nextProducts: Product[]) => void;
  onNotify: (type: 'ok' | 'err', text: string) => void;
  onLogActivity?: (entry: {
    actionType: 'bulk_import';
    sku: string;
    summary: string;
    details?: string;
  }) => void;
}

const DEFAULT_CHANNELS: PublishingChannels = {
  navGroceryWeb: true,
  navKartWeb: true,
  androidApps: true,
};

function downloadTextFile(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export const BatchCsvImportPanel: React.FC<BatchCsvImportPanelProps> = ({
  products,
  verified308,
  onSyncedProducts,
  onNotify,
  onLogActivity,
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [csvText, setCsvText] = useState<string>('');
  const [fileName, setFileName] = useState<string>('');
  const [nameAndMrpOnly, setNameAndMrpOnly] = useState<boolean>(true);
  const [newSkuChannels, setNewSkuChannels] = useState<PublishingChannels>(DEFAULT_CHANNELS);
  const [excludedRows, setExcludedRows] = useState<Set<number>>(new Set());
  const [syncing, setSyncing] = useState<boolean>(false);
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);

  const summary = useMemo(() => {
    if (!csvText.trim()) return null;
    return parseCatalogCsv(csvText, products, verified308);
  }, [csvText, products, verified308]);

  const activeValidRows = useMemo(() => {
    if (!summary) return [];
    return summary.rows.filter(
      (r) => r.action !== 'invalid' && !excludedRows.has(r.rowNumber)
    );
  }, [summary, excludedRows]);

  const handleFileLoad = async (file: File | undefined) => {
    if (!file) return;
    try {
      const text = await file.text();
      setFileName(file.name);
      setCsvText(text);
      setExcludedRows(new Set());
    } catch {
      onNotify('err', `Could not read CSV file "${file.name}".`);
    }
  };

  const handleToggleExcludeRow = (rowNum: number) => {
    setExcludedRows((prev) => {
      const next = new Set(prev);
      if (next.has(rowNum)) next.delete(rowNum);
      else next.add(rowNum);
      return next;
    });
  };

  const handleCommitBatch = async () => {
    if (activeValidRows.length === 0) {
      onNotify('err', 'No valid CSV rows ready to import.');
      return;
    }

    setSyncing(true);
    setProgress({ done: 0, total: activeValidRows.length });

    const byId = new Map<string, Product>(products.map((p) => [p.id.toUpperCase(), p]));
    const verifiedMap = new Map<string, Product>(verified308.map((p) => [p.id.toUpperCase(), p]));
    let appliedCount = 0;

    for (let i = 0; i < activeValidRows.length; i += 50) {
      const chunk = activeValidRows.slice(i, i + 50);
      const patchPayload: Record<string, Product> = {};

      for (const row of chunk) {
        const existing = byId.get(row.sku) || verifiedMap.get(row.sku);
        const built = buildUpdatedProductFromCsvRow(
          row,
          existing,
          newSkuChannels,
          nameAndMrpOnly
        );
        patchPayload[row.sku] = built;
        byId.set(row.sku, built);
      }

      const res = await patchProductsInFirebase(patchPayload);
      if (!res.ok) {
        setSyncing(false);
        setProgress(null);
        onNotify(
          'err',
          `Firebase batch PATCH failed after syncing ${appliedCount} SKUs. Check network connectivity.`
        );
        return;
      }

      appliedCount += chunk.length;
      setProgress({ done: appliedCount, total: activeValidRows.length });
    }

    const nextList = Array.from(byId.values()).sort((a, b) =>
      a.id.localeCompare(b.id, undefined, { numeric: true })
    );
    onSyncedProducts(nextList);
    await syncAndroidFeedSnapshot(nextList);

    const updatedCount = activeValidRows.filter((r) => r.action === 'update').length;
    const createdCount = activeValidRows.filter((r) => r.action === 'create').length;
    const sampleSkus = activeValidRows
      .slice(0, 6)
      .map((r) => r.sku)
      .join(', ');
    onLogActivity?.({
      actionType: 'bulk_import',
      sku:
        activeValidRows.length === 1
          ? activeValidRows[0].sku
          : `${activeValidRows.length} SKUs`,
      summary: `CSV bulk import applied (${updatedCount} updated, ${createdCount} new)`,
      details: `${fileName ? `File: ${fileName} · ` : ''}SKUs: ${sampleSkus}${
        activeValidRows.length > 6 ? ` +${activeValidRows.length - 6} more` : ''
      }`,
    });

    setSyncing(false);
    setProgress(null);
    onNotify(
      'ok',
      `Batch CSV Import complete: ${appliedCount} SKUs updated directly in Firebase RTDB & Android Feed (12% wholesale pricing applied).`
    );
  };

  const renderDiffBadge = (row: CsvImportRow) => {
    if (row.action === 'invalid') {
      return (
        <span className="inline-flex items-center gap-1 text-red-700 font-bold">
          <AlertCircle className="w-3.5 h-3.5 shrink-0" />
          <span>Invalid</span>
        </span>
      );
    }
    if (row.action === 'create') {
      return (
        <span className="inline-flex items-center gap-1 text-[#0369A1] font-bold">
          <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
          <span>New SKU</span>
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 text-[#0B3B24] font-bold">
        <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
        <span>Update</span>
      </span>
    );
  };

  return (
    <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 sm:p-6 space-y-5">
      {/* Header & Template/Export Actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-[#0B3B24]/10">
        <div>
          <h2 className="font-display text-lg font-bold text-[#0B3B24]">
            Batch CSV Catalog Import (SKU ID, MRP &amp; Product Name)
          </h2>
          <p className="text-xs text-[#526058] mt-0.5">
            Upload a CSV spreadsheet to bulk-update MRPs, product names, or add new SKUs directly into Firebase RTDB. Sale prices auto-calculate at flat 12% off MRP.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => downloadTextFile('nav_batch_template.csv', buildSampleCsvTemplate())}
            className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] hover:bg-[#EFECE2] text-[#0B3B24] text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer whitespace-nowrap"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Sample CSV</span>
          </button>
          <button
            type="button"
            onClick={() =>
              downloadTextFile(
                `nav_catalog_${new Date().toISOString().slice(0, 10)}.csv`,
                exportCatalogToCsv(products)
              )
            }
            disabled={products.length === 0}
            className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white hover:bg-[#FAF8F3] text-[#14221B] text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer whitespace-nowrap disabled:opacity-45"
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-[#0B3B24]" />
            <span>Export Live CSV ({products.length})</span>
          </button>
        </div>
      </div>

      {/* Upload Zone + Quick Paste */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        <div
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            handleFileLoad(e.dataTransfer.files?.[0]);
          }}
          onClick={() => fileInputRef.current?.click()}
          className="lg:col-span-6 border-2 border-dashed border-[#0B3B24]/25 rounded-xl p-6 text-center bg-[#FAF8F3] hover:bg-[#F3EFE4] transition-colors cursor-pointer flex flex-col items-center justify-center"
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,text/csv"
            className="hidden"
            onChange={(e) => {
              handleFileLoad(e.target.files?.[0]);
              e.target.value = '';
            }}
          />
          <Upload className="w-7 h-7 text-[#0B3B24] mb-2" />
          <p className="text-sm font-bold text-[#14221B]">
            {fileName ? `Loaded: ${fileName}` : 'Drop .CSV file here or click to upload'}
          </p>
          <p className="text-xs text-[#526058] mt-1">
            Required columns: <span className="font-mono-num">sku_id, product_name, mrp</span> (optional: brand, department, category, stock)
          </p>
        </div>

        <div className="lg:col-span-6 flex flex-col">
          <div className="flex items-center justify-between mb-1">
            <label
              htmlFor="csv-paste-input"
              className="text-xs font-bold text-[#526058]"
            >
              Or Paste CSV Rows Directly
            </label>
            {csvText && (
              <button
                type="button"
                onClick={() => {
                  setCsvText('');
                  setFileName('');
                  setExcludedRows(new Set());
                }}
                className="text-xs font-semibold text-red-700 hover:underline cursor-pointer"
              >
                Clear CSV
              </button>
            )}
          </div>
          <textarea
            id="csv-paste-input"
            value={csvText}
            onChange={(e) => {
              setCsvText(e.target.value);
              setFileName('');
              setExcludedRows(new Set());
            }}
            placeholder={`sku_id,product_name,mrp\nNAV001,Aashirvaad Superior MP Sharbati Atta 10kg,540\nNAV002,Fortune Chakki Fresh Atta 10kg,475`}
            className="w-full flex-1 min-h-[128px] p-3 rounded-xl border border-[#0B3B24]/20 bg-[#FAF8F3] font-mono-num text-xs text-[#14221B] focus:outline-none focus:border-[#0B3B24]"
          />
        </div>
      </div>

      {/* Options */}
      <div className="p-4 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/15 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <label className="inline-flex items-start gap-2.5 text-xs text-[#14221B] cursor-pointer">
          <input
            type="checkbox"
            checked={nameAndMrpOnly}
            onChange={(e) => setNameAndMrpOnly(e.target.checked)}
            className="mt-0.5 accent-[#0B3B24]"
          />
          <span>
            <strong className="text-[#0B3B24]">
              Preserve existing Stock, Photos &amp; Channels on updates
            </strong>
            <span className="block text-[#526058]">
              Recommended when uploading 3-column CSVs (`sku_id, product_name, mrp`).
            </span>
          </span>
        </label>

        <div className="flex flex-wrap items-center gap-3 text-xs font-semibold">
          <span className="text-[#526058]">New SKU Channels:</span>
          <label className="inline-flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={newSkuChannels.navGroceryWeb}
              onChange={(e) =>
                setNewSkuChannels((c) => ({ ...c, navGroceryWeb: e.target.checked }))
              }
              className="accent-[#0B3B24]"
            />
            <span>Nav Grocery</span>
          </label>
          <label className="inline-flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={newSkuChannels.navKartWeb}
              onChange={(e) =>
                setNewSkuChannels((c) => ({ ...c, navKartWeb: e.target.checked }))
              }
              className="accent-[#0B3B24]"
            />
            <span>NavKart</span>
          </label>
          <label className="inline-flex items-center gap-1.5 cursor-pointer">
            <input
              type="checkbox"
              checked={newSkuChannels.androidApps}
              onChange={(e) =>
                setNewSkuChannels((c) => ({ ...c, androidApps: e.target.checked }))
              }
              className="accent-[#0B3B24]"
            />
            <span>Android</span>
          </label>
        </div>
      </div>

      {/* Fatal Parse Error */}
      {summary?.fatalError && (
        <div
          role="alert"
          className="px-4 py-3 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs font-semibold flex items-center gap-2"
        >
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{summary.fatalError}</span>
        </div>
      )}

      {/* Parsed Preview Table & Commit Bar */}
      {summary && !summary.fatalError && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-4 text-xs font-mono-num">
              <span className="font-bold text-[#14221B]">
                Parsed Rows: {summary.rows.length}
              </span>
              <span className="text-[#526058]">·</span>
              <span className="font-bold text-[#0B3B24]">
                Updates: {summary.updateCount}
              </span>
              <span className="text-[#526058]">·</span>
              <span className="font-bold text-[#0369A1]">
                New SKUs: {summary.createCount}
              </span>
              {summary.invalidCount > 0 && (
                <>
                  <span className="text-[#526058]">·</span>
                  <span className="font-bold text-red-700">
                    Invalid (Skipped): {summary.invalidCount}
                  </span>
                </>
              )}
            </div>

            <button
              type="button"
              disabled={syncing || activeValidRows.length === 0}
              onClick={handleCommitBatch}
              className="px-4 py-2.5 rounded-xl bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs sm:text-sm font-bold inline-flex items-center gap-2 cursor-pointer disabled:opacity-45 whitespace-nowrap"
            >
              <Database className="w-4 h-4 text-[#D4AF37]" />
              <span>
                {syncing
                  ? 'Syncing Batch to Firebase...'
                  : `Bulk Update ${activeValidRows.length} SKUs in Firebase`}
              </span>
            </button>
          </div>

          {progress && (
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-mono-num font-bold text-[#0B3B24]">
                <span>Writing batch updates to Firebase RTDB...</span>
                <span>
                  {progress.done} / {progress.total}
                </span>
              </div>
              <div className="w-full h-2.5 rounded-full bg-[#FAF8F3] overflow-hidden border border-[#0B3B24]/15">
                <div
                  className="h-full bg-[#0B3B24] transition-all"
                  style={{
                    width: `${Math.round((progress.done / progress.total) * 100)}%`,
                  }}
                />
              </div>
            </div>
          )}

          <div className="overflow-x-auto border border-[#0B3B24]/12 rounded-xl max-h-[420px]">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 bg-[#FAF8F3] border-b border-[#0B3B24]/12 text-[11px] font-bold uppercase tracking-wider text-[#526058]">
                <tr>
                  <th className="py-2.5 px-3">Row</th>
                  <th className="py-2.5 px-3">SKU</th>
                  <th className="py-2.5 px-3">Action</th>
                  <th className="py-2.5 px-3">Product Name</th>
                  <th className="py-2.5 px-3 text-right">MRP (₹)</th>
                  <th className="py-2.5 px-3 text-right">12% Off Sale</th>
                  <th className="py-2.5 px-3 text-right">Include</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#0B3B24]/8 text-xs">
                {summary.rows.map((row) => {
                  const isExcluded = excludedRows.has(row.rowNumber);
                  return (
                    <tr
                      key={`${row.rowNumber}-${row.sku}`}
                      className={
                        row.action === 'invalid'
                          ? 'bg-red-50/60'
                          : isExcluded
                          ? 'opacity-45 bg-neutral-50'
                          : 'hover:bg-[#FAF8F3]/70'
                      }
                    >
                      <td className="py-2 px-3 font-mono-num text-[#526058]">
                        {row.rowNumber}
                      </td>
                      <td className="py-2 px-3 font-mono-num font-bold text-[#0B3B24]">
                        {row.sku}
                      </td>
                      <td className="py-2 px-3 whitespace-nowrap">
                        {renderDiffBadge(row)}
                      </td>
                      <td className="py-2 px-3 min-w-[220px]">
                        <div className="font-semibold text-[#14221B]">{row.name}</div>
                        {row.previousName && row.previousName !== row.name && (
                          <div className="text-[11px] text-[#526058]">
                            Prev: {row.previousName}
                          </div>
                        )}
                        {row.error && (
                          <div className="text-[11px] font-semibold text-red-700">
                            {row.error}
                          </div>
                        )}
                      </td>
                      <td className="py-2 px-3 text-right font-mono-num whitespace-nowrap">
                        <span className="font-bold text-[#14221B]">
                          {row.mrp > 0 ? `₹${row.mrp}` : '—'}
                        </span>
                        {row.previousMrp && row.previousMrp !== row.mrp && (
                          <span className="block text-[11px] text-[#526058] line-through">
                            ₹{row.previousMrp}
                          </span>
                        )}
                      </td>
                      <td className="py-2 px-3 text-right font-mono-num font-bold text-[#0B3B24]">
                        {row.sale > 0 ? `₹${row.sale}` : '—'}
                      </td>
                      <td className="py-2 px-3 text-right">
                        {row.action !== 'invalid' && (
                          <button
                            type="button"
                            onClick={() => handleToggleExcludeRow(row.rowNumber)}
                            aria-label={isExcluded ? `Include row ${row.rowNumber}` : `Skip row ${row.rowNumber}`}
                            className="p-1.5 rounded-lg border border-[#0B3B24]/15 hover:bg-white text-[#526058] cursor-pointer"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
};
