'use client';

import React, { useState, useEffect } from 'react';
import { Product, CATEGORIES, calculateWholesalePrice } from '@/lib/domain/catalog';

export default function AdminControlCenter() {
  const [products, setProducts] = useState<Product[]>([]);
  const [pin, setPin] = useState('');
  const [unlocked, setUnlocked] = useState(false);
  const [form, setForm] = useState({
    name: '',
    brand: '',
    category: CATEGORIES[0] as string,
    packagingSize: '1 kg',
    barcode: '',
    mrp: '',
    imageUrl: 'https://images.navgrocerystore.com/default.png',
    isFrozen: false,
  });

  const loadCatalog = async () => {
    const res = await fetch('/api/products');
    const data = await res.json();
    setProducts(data.data || []);
  };

  useEffect(() => {
    if (unlocked) loadCatalog();
  }, [unlocked]);

  const handleAddProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    const res = await fetch('/api/products', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form, mrp: Number(form.mrp) }),
    });
    if (res.ok) {
      setForm({ ...form, name: '', brand: '', barcode: '', mrp: '' });
      loadCatalog();
    } else {
      alert('Error: Fresh produce is blocked or invalid MRP.');
    }
  };

  const handleDelete = async (id: string) => {
    await fetch(`/api/products?id=${id}`, { method: 'DELETE' });
    loadCatalog();
  };

  if (!unlocked) {
    return (
      <div className="min-h-screen bg-brand-emerald flex items-center justify-center p-4">
        <div className="bg-white rounded-card p-6 max-w-sm w-full space-y-4 shadow-modal">
          <h1 className="text-lg font-black text-brand-emerald">Navo Hub Admin Control</h1>
          <p className="text-xs text-gray-500">Sandhaur, Punjab | Enter Admin PIN</p>
          <input
            type="password"
            placeholder="Admin PIN (Default: 148020)"
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            className="w-full p-2.5 border rounded-card text-xs"
          />
          <button
            onClick={() => (pin === '148020' ? setUnlocked(true) : alert('Wrong PIN'))}
            className="w-full bg-brand-gold text-brand-charcoal font-black py-2.5 rounded-card text-xs"
          >
            Unlock Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-brand-surface p-4 md:p-8 text-brand-charcoal">
      <div className="max-w-6xl mx-auto space-y-6">
        <header className="bg-brand-emerald text-white p-4 rounded-card flex justify-between items-center">
          <div>
            <h1 className="text-xl font-black">Nav Grocery Store - Admin Control Center</h1>
            <p className="text-xs text-brand-gold">Active SKUs: {products.length} | Flat 12% Discount Enforced</p>
          </div>
          <a href="/" className="bg-brand-gold text-brand-charcoal text-xs font-black px-4 py-2 rounded-pill">
            View Storefront
          </a>
        </header>

        <form onSubmit={handleAddProduct} className="bg-white p-5 rounded-card shadow-soft grid grid-cols-1 md:grid-cols-4 gap-3">
          <input
            required
            placeholder="Product Name (No fresh produce)"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="border p-2 rounded-card text-xs"
          />
          <input
            required
            placeholder="Brand"
            value={form.brand}
            onChange={(e) => setForm({ ...form, brand: e.target.value })}
            className="border p-2 rounded-card text-xs"
          />
          <select
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
            className="border p-2 rounded-card text-xs"
          >
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <input
            required
            type="number"
            placeholder="MRP (Rs)"
            value={form.mrp}
            onChange={(e) => setForm({ ...form, mrp: e.target.value })}
            className="border p-2 rounded-card text-xs"
          />
          <input
            placeholder="Pack Size (e.g. 1 kg)"
            value={form.packagingSize}
            onChange={(e) => setForm({ ...form, packagingSize: e.target.value })}
            className="border p-2 rounded-card text-xs"
          />
          <input
            placeholder="Barcode"
            value={form.barcode}
            onChange={(e) => setForm({ ...form, barcode: e.target.value })}
            className="border p-2 rounded-card text-xs"
          />
          <label className="flex items-center gap-2 text-xs font-bold">
            <input
              type="checkbox"
              checked={form.isFrozen}
              onChange={(e) => setForm({ ...form, isFrozen: e.target.checked })}
            />
            IQF Frozen Item
          </label>
          <button type="submit" className="bg-brand-emerald text-white font-bold py-2 rounded-card text-xs">
            + Add SKU (Sale: Rs {form.mrp ? calculateWholesalePrice(Number(form.mrp)) : 0})
          </button>
        </form>

        <div className="bg-white rounded-card shadow-soft overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-brand-emerald-dark text-white">
              <tr>
                <th className="p-3">SKU Name</th>
                <th className="p-3">Brand</th>
                <th className="p-3">Category</th>
                <th className="p-3">MRP</th>
                <th className="p-3">12% Off Price</th>
                <th className="p-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} className="border-b">
                  <td className="p-3 font-bold">{p.name}</td>
                  <td className="p-3">{p.brand}</td>
                  <td className="p-3">{p.category}</td>
                  <td className="p-3">Rs {p.mrp}</td>
                  <td className="p-3 text-brand-emerald font-black">Rs {p.salePrice}</td>
                  <td className="p-3">
                    <button onClick={() => handleDelete(p.id)} className="text-brand-danger font-bold">
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
