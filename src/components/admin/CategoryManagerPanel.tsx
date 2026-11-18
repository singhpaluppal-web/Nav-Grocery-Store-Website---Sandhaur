import React, { useMemo, useState } from 'react';
import {
  Check,
  Database,
  FolderKanban,
  Layers,
  Pencil,
  Plus,
  Search,
  Tag,
  X,
} from 'lucide-react';
import { Product } from '../../types';

interface CategoryManagerPanelProps {
  products: Product[];
  allCategories: string[];
  departments: string[];
  busyAction: string;
  onAddCustomCategory: (name: string) => void;
  onRenameCategory: (oldCategory: string, newCategory: string) => Promise<void>;
  onBulkAssignCategory: (
    skuIds: string[],
    targetCategory: string,
    targetDepartment?: string
  ) => Promise<void>;
  onViewCategoryInCatalog: (categoryName: string) => void;
  onNotify: (type: 'ok' | 'err', text: string) => void;
}

export const CategoryManagerPanel: React.FC<CategoryManagerPanelProps> = ({
  products,
  allCategories,
  departments,
  busyAction,
  onAddCustomCategory,
  onRenameCategory,
  onBulkAssignCategory,
  onViewCategoryInCatalog,
  onNotify,
}) => {
  const [newCategoryInput, setNewCategoryInput] = useState('');
  const [editingCategory, setEditingCategory] = useState<string | null>(null);
  const [renameDraft, setRenameDraft] = useState('');

  // Bulk assignment workbench state
  const [searchQuery, setSearchQuery] = useState('');
  const [sourceCategoryFilter, setSourceCategoryFilter] = useState<string>('all');
  const [checkedSkus, setCheckedSkus] = useState<Set<string>>(new Set());
  const [targetCategory, setTargetCategory] = useState<string>(
    allCategories[0] || 'Snacks'
  );
  const [customTargetCategory, setCustomTargetCategory] = useState('');
  const [targetDepartment, setTargetDepartment] = useState<string>('keep');

  const categoryStats = useMemo(() => {
    const map = new Map<
      string,
      {
        name: string;
        count: number;
        inStockCount: number;
        departments: Set<string>;
      }
    >();

    for (const cat of allCategories) {
      map.set(cat, {
        name: cat,
        count: 0,
        inStockCount: 0,
        departments: new Set<string>(),
      });
    }

    for (const p of products) {
      const cat = (p.category || 'Uncategorized').trim();
      const existing = map.get(cat) || {
        name: cat,
        count: 0,
        inStockCount: 0,
        departments: new Set<string>(),
      };
      existing.count++;
      if (Number(p.stock) > 0) existing.inStockCount++;
      if (p.department) existing.departments.add(p.department);
      map.set(cat, existing);
    }

    return Array.from(map.values()).sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count;
      return a.name.localeCompare(b.name);
    });
  }, [products, allCategories]);

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return products.filter((p) => {
      if (
        sourceCategoryFilter !== 'all' &&
        (p.category || 'Uncategorized').trim() !== sourceCategoryFilter
      ) {
        return false;
      }
      if (!q) return true;
      const hay = `${p.id} ${p.name} ${p.brand} ${p.category} ${p.department}`.toLowerCase();
      return q.split(/\s+/).every((tk) => hay.includes(tk));
    });
  }, [products, sourceCategoryFilter, searchQuery]);

  const handleCreateCategory = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = newCategoryInput.trim();
    if (!clean) {
      onNotify('err', 'Enter a category name (e.g. Frozen, Dairy, Snacks).');
      return;
    }
    onAddCustomCategory(clean);
    setTargetCategory(clean);
    setNewCategoryInput('');
  };

  const handleConfirmRename = async (oldCat: string) => {
    const clean = renameDraft.trim();
    if (!clean || clean === oldCat) {
      setEditingCategory(null);
      return;
    }
    await onRenameCategory(oldCat, clean);
    setEditingCategory(null);
  };

  const toggleSku = (skuId: string) => {
    setCheckedSkus((prev) => {
      const next = new Set(prev);
      if (next.has(skuId)) next.delete(skuId);
      else next.add(skuId);
      return next;
    });
  };

  const toggleSelectAllVisible = () => {
    const visibleIds = filteredProducts.map((p) => p.id);
    const allChecked =
      visibleIds.length > 0 && visibleIds.every((id) => checkedSkus.has(id));
    setCheckedSkus((prev) => {
      const next = new Set(prev);
      if (allChecked) {
        visibleIds.forEach((id) => next.delete(id));
      } else {
        visibleIds.forEach((id) => next.add(id));
      }
      return next;
    });
  };

  const handleApplyBulkCategory = async () => {
    const chosenCat = (customTargetCategory.trim() || targetCategory).trim();
    if (!chosenCat) {
      onNotify('err', 'Choose or enter a target category name.');
      return;
    }
    if (checkedSkus.size === 0) {
      onNotify('err', 'Select at least 1 SKU row below to assign a category.');
      return;
    }

    await onBulkAssignCategory(
      Array.from(checkedSkus),
      chosenCat,
      targetDepartment === 'keep' ? undefined : targetDepartment
    );
    setCustomTargetCategory('');
    setCheckedSkus(new Set());
  };

  return (
    <div className="space-y-5">
      {/* Top Section: Category Directory + Add / Rename */}
      <section className="bg-white rounded-xl border border-[#0B3B24]/12 p-5 space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-[#0B3B24]/10">
          <div>
            <h2 className="font-display text-lg font-bold text-[#0B3B24] flex items-center gap-2">
              <FolderKanban className="w-5 h-5 text-[#D4AF37]" />
              <span>Inventory Categories Directory</span>
            </h2>
            <p className="text-xs text-[#526058] mt-0.5">
              Create, rename, and organize product categories (such as Frozen, Dairy, Snacks) across Firebase RTDB &amp; Android Feed.
            </p>
          </div>

          <form
            onSubmit={handleCreateCategory}
            className="flex items-center gap-2 shrink-0"
          >
            <input
              id="admin-new-category-input"
              type="text"
              value={newCategoryInput}
              onChange={(e) => setNewCategoryInput(e.target.value)}
              placeholder="New category (e.g. Dairy, Snacks)"
              className="w-56 px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-[#FAF8F3] text-xs font-semibold focus:outline-none focus:border-[#0B3B24]"
            />
            <button
              type="submit"
              className="px-3.5 py-2 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white text-xs font-bold inline-flex items-center gap-1.5 cursor-pointer whitespace-nowrap"
            >
              <Plus className="w-3.5 h-3.5 text-[#D4AF37]" />
              <span>Add Category</span>
            </button>
          </form>
        </div>

        {/* Category Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {categoryStats.map((cat) => {
            const isEditing = editingCategory === cat.name;
            return (
              <div
                key={cat.name}
                className="p-3.5 rounded-xl border border-[#0B3B24]/15 bg-[#FAF8F3] flex flex-col justify-between gap-2.5"
              >
                {isEditing ? (
                  <div className="flex items-center gap-1.5">
                    <input
                      type="text"
                      aria-label={`Rename category ${cat.name}`}
                      value={renameDraft}
                      onChange={(e) => setRenameDraft(e.target.value)}
                      autoFocus
                      className="flex-1 px-2.5 py-1.5 rounded-lg border border-[#0B3B24]/30 bg-white text-xs font-bold focus:outline-none"
                    />
                    <button
                      type="button"
                      disabled={Boolean(busyAction)}
                      onClick={() => void handleConfirmRename(cat.name)}
                      className="p-1.5 rounded-lg bg-[#0B3B24] text-white cursor-pointer disabled:opacity-45"
                      title="Save Category Name"
                    >
                      <Check className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setEditingCategory(null)}
                      className="p-1.5 rounded-lg border border-[#0B3B24]/20 bg-white text-[#526058] cursor-pointer"
                      title="Cancel"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ) : (
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="font-bold text-sm text-[#14221B] truncate flex items-center gap-1.5">
                        <Tag className="w-3.5 h-3.5 text-[#0B3B24] shrink-0" />
                        <span>{cat.name}</span>
                      </div>
                      <div className="text-[11px] text-[#526058] mt-0.5 truncate">
                        {cat.departments.size > 0
                          ? Array.from(cat.departments).join(', ')
                          : 'Preset Category (Ready to assign)'}
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded bg-white border border-[#0B3B24]/15 text-[#0B3B24] font-mono-num text-[11px] font-bold shrink-0">
                      {cat.count} SKUs
                    </span>
                  </div>
                )}

                <div className="pt-2 border-t border-[#0B3B24]/10 flex items-center justify-between text-[11px]">
                  <span className="font-mono-num text-[#526058]">
                    {cat.count > 0 ? `${cat.inStockCount} In Stock` : '0 items assigned'}
                  </span>
                  <div className="flex items-center gap-1.5">
                    {cat.count > 0 && (
                      <>
                        <button
                          type="button"
                          onClick={() => {
                            setEditingCategory(cat.name);
                            setRenameDraft(cat.name);
                          }}
                          className="px-2 py-1 rounded border border-[#0B3B24]/20 bg-white hover:bg-[#EFECE2] text-[#14221B] font-semibold inline-flex items-center gap-1 cursor-pointer"
                        >
                          <Pencil className="w-3 h-3" />
                          <span>Rename</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => onViewCategoryInCatalog(cat.name)}
                          className="px-2 py-1 rounded bg-[#0B3B24] text-white font-semibold inline-flex items-center gap-1 cursor-pointer"
                        >
                          <Layers className="w-3 h-3 text-[#D4AF37]" />
                          <span>View</span>
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Bottom Section: Bulk Category Assignment Workbench */}
      <section className="bg-white rounded-xl border border-[#0B3B24]/12 overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-[#0B3B24]/10 space-y-4">
          <div>
            <h3 className="font-display text-base font-bold text-[#0B3B24]">
              Bulk-Assign Categories to Products
            </h3>
            <p className="text-xs text-[#526058] mt-0.5">
              Filter products by keyword, SKU, or existing category, select multiple rows, and assign them to a unified category in one Firebase batch update.
            </p>
          </div>

          {/* Search & Source Filter */}
          <div className="flex flex-col md:flex-row md:items-center gap-2.5">
            <div className="relative flex-1 max-w-md">
              <Search className="w-4 h-4 text-[#526058] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="search"
                aria-label="Filter SKUs for bulk category assignment"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search SKU (NAV015), product (Cheese, Chips, Ice Cream)..."
                className="w-full pl-9 pr-8 py-2 rounded-lg border border-[#0B3B24]/20 bg-[#FAF8F3] text-xs sm:text-sm focus:outline-none focus:border-[#0B3B24]"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear search"
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-[#526058] cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            <select
              aria-label="Filter by current category"
              value={sourceCategoryFilter}
              onChange={(e) => setSourceCategoryFilter(e.target.value)}
              className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white text-xs font-semibold text-[#14221B] focus:outline-none"
            >
              <option value="all">All Current Categories ({products.length})</option>
              {categoryStats
                .filter((c) => c.count > 0)
                .map((c) => (
                  <option key={c.name} value={c.name}>
                    {c.name} ({c.count})
                  </option>
                ))}
            </select>

            <span className="text-xs font-mono-num font-bold text-[#526058]">
              Showing {filteredProducts.length} SKUs · {checkedSkus.size} selected
            </span>
          </div>

          {/* Target Assignment Bar */}
          <div className="p-3.5 rounded-xl bg-[#FAF8F3] border border-[#0B3B24]/20 flex flex-wrap items-center justify-between gap-3 text-xs">
            <div className="flex flex-wrap items-center gap-2.5 flex-1">
              <label
                htmlFor="bulk-target-category"
                className="font-bold text-[#0B3B24] whitespace-nowrap"
              >
                Assign Target Category:
              </label>
              <select
                id="bulk-target-category"
                value={targetCategory}
                onChange={(e) => setTargetCategory(e.target.value)}
                className="px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-white font-semibold text-[#14221B] focus:outline-none"
              >
                {allCategories.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>

              <input
                type="text"
                aria-label="Or enter custom category name"
                value={customTargetCategory}
                onChange={(e) => setCustomTargetCategory(e.target.value)}
                placeholder="Or type custom (e.g. Dairy)"
                className="w-48 px-3 py-2 rounded-lg border border-[#0B3B24]/25 bg-white font-semibold focus:outline-none focus:border-[#0B3B24]"
              />

              <select
                aria-label="Optional department update"
                value={targetDepartment}
                onChange={(e) => setTargetDepartment(e.target.value)}
                className="px-3 py-2 rounded-lg border border-[#0B3B24]/20 bg-white font-semibold text-[#526058] focus:outline-none"
              >
                <option value="keep">Keep Current Department</option>
                {departments.map((d) => (
                  <option key={d} value={d}>
                    Set Dept: {d}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {checkedSkus.size > 0 && (
                <button
                  type="button"
                  onClick={() => setCheckedSkus(new Set())}
                  className="px-2.5 py-2 rounded-lg text-[#526058] hover:text-[#14221B] font-semibold cursor-pointer"
                >
                  Clear ({checkedSkus.size})
                </button>
              )}
              <button
                type="button"
                disabled={checkedSkus.size === 0 || Boolean(busyAction)}
                onClick={() => void handleApplyBulkCategory()}
                className="px-4 py-2 rounded-lg bg-[#0B3B24] hover:bg-[#082B1A] text-white font-bold inline-flex items-center gap-1.5 cursor-pointer disabled:opacity-45 whitespace-nowrap"
              >
                <Database className="w-3.5 h-3.5 text-[#D4AF37]" />
                <span>
                  {busyAction === 'bulk-category'
                    ? 'Updating...'
                    : `Assign to ${checkedSkus.size} Selected SKU(s)`}
                </span>
              </button>
            </div>
          </div>
        </div>

        {/* Matching Products Selection Table */}
        <div className="overflow-x-auto max-h-[460px]">
          <table className="w-full text-left border-collapse">
            <thead className="sticky top-0 bg-[#FAF8F3] border-b border-[#0B3B24]/12 text-[11px] font-bold uppercase tracking-wider text-[#526058]">
              <tr>
                <th className="py-2.5 px-3 w-9">
                  <input
                    type="checkbox"
                    aria-label="Select all visible products for category update"
                    checked={
                      filteredProducts.length > 0 &&
                      filteredProducts.every((p) => checkedSkus.has(p.id))
                    }
                    onChange={toggleSelectAllVisible}
                    className="accent-[#0B3B24] cursor-pointer"
                  />
                </th>
                <th className="py-2.5 px-3">SKU</th>
                <th className="py-2.5 px-3">Product Name &amp; Brand</th>
                <th className="py-2.5 px-3">Current Category</th>
                <th className="py-2.5 px-3">Department</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#0B3B24]/8 text-xs">
              {filteredProducts.slice(0, 100).map((product) => {
                const isChecked = checkedSkus.has(product.id);
                return (
                  <tr
                    key={product.id}
                    onClick={() => toggleSku(product.id)}
                    className={`cursor-pointer transition-colors ${
                      isChecked ? 'bg-emerald-50/70' : 'hover:bg-[#FAF8F3]/70'
                    }`}
                  >
                    <td
                      className="py-2.5 px-3"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <input
                        type="checkbox"
                        aria-label={`Select ${product.id} for category assignment`}
                        checked={isChecked}
                        onChange={() => toggleSku(product.id)}
                        className="accent-[#0B3B24] cursor-pointer"
                      />
                    </td>
                    <td className="py-2.5 px-3 font-mono-num font-bold text-[#0B3B24] whitespace-nowrap">
                      {product.id}
                    </td>
                    <td className="py-2.5 px-3">
                      <div className="font-semibold text-[#14221B]">{product.name}</div>
                      <div className="text-[11px] text-[#526058]">{product.brand}</div>
                    </td>
                    <td className="py-2.5 px-3 whitespace-nowrap">
                      <span className="inline-block px-2 py-0.5 rounded border border-[#0B3B24]/20 bg-[#FAF8F3] font-semibold text-[#0B3B24]">
                        {product.category}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-[#526058] whitespace-nowrap">
                      {product.department}
                    </td>
                  </tr>
                );
              })}
              {filteredProducts.length > 100 && (
                <tr>
                  <td
                    colSpan={5}
                    className="py-2.5 px-3 text-center text-[11px] text-[#526058] bg-[#FAF8F3]"
                  >
                    Showing first 100 of {filteredProducts.length} matching SKUs. Refine your search filter above to narrow further.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
