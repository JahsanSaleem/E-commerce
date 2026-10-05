import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getInventory, getInventoryProduct, updateStock } from "../services/inventoryService.js";

function StockEditor({ productId, onClose, onSaved }) {
  const client = useQueryClient();
  const [quantity, setQuantity] = useState(null);
  const product = useQuery({ queryKey: ["inventory", productId], queryFn: () => getInventoryProduct(productId) });
  const update = useMutation({ mutationFn: updateStock, onSuccess: async (saved) => {
    client.setQueryData(["inventory", productId], saved);
    await Promise.all([client.invalidateQueries({ queryKey: ["inventory"] }), client.invalidateQueries({ queryKey: ["products"] })]);
    onSaved(`Stock updated for ${saved.name}.`);
    onClose();
  }});
  const value = quantity === null ? String(product.data?.quantity ?? "") : quantity;
  const valid = value !== "" && Number.isSafeInteger(Number(value)) && Number(value) >= 0 && Number(value) <= 2147483647;
  return <section className="panel p-5" aria-labelledby="stock-title">
    <h2 id="stock-title" className="text-xl font-bold">Update stock</h2>
    {product.isPending ? <p role="status" className="mt-4">Loading product…</p> : product.isError ? <p role="alert" className="mt-4 text-red-700">Unable to load this product. <button className="underline" onClick={() => product.refetch()}>Retry</button></p> : <form className="mt-4 space-y-4" onSubmit={(event) => { event.preventDefault(); if (valid && !update.isPending) update.mutate({ productId, quantity: Number(value) }); }}>
      <p className="font-semibold">{product.data.name} <span className="font-normal text-slate-500">· Current stock: {product.data.quantity}</span></p>
      <p className="text-sm text-slate-500">Enter the total available quantity after this update.</p>
      <div className="max-w-xs"><label className="field-label" htmlFor="stock-quantity">Stock quantity</label><input id="stock-quantity" className="field-input" type="number" min="0" max="2147483647" step="1" required value={value} disabled={update.isPending} onChange={(event) => setQuantity(event.target.value)} />{!valid && <p className="field-error">Enter a whole number between 0 and 2147483647.</p>}</div>
      {update.isError && <p role="alert" className="field-error">{update.error.response?.data?.message || "Unable to update stock. Please try again."}</p>}
      <button className="btn-primary" disabled={!valid || update.isPending}>{update.isPending ? "Saving…" : "Save stock"}</button>
    </form>}
    <button className="btn-outline mt-3" disabled={update.isPending} onClick={onClose}>Cancel</button>
  </section>;
}

export default function Inventory() {
  const [search, setSearch] = useState("");
  const [availability, setAvailability] = useState("all");
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState("");
  const inventory = useQuery({ queryKey: ["inventory"], queryFn: getInventory });
  const products = inventory.data ?? [];
  const visible = products.filter((product) => `${product.name} ${product.category?.name ?? ""}`.toLowerCase().includes(search.trim().toLowerCase()) && (availability === "all" || (availability === "out" ? Number(product.quantity) === 0 : Number(product.quantity) > 0)));
  return <section className="space-y-6">
    <div className="flex flex-wrap items-center justify-between gap-4"><div><p className="eyebrow">Store management</p><h1 className="mt-2 text-3xl font-bold">Inventory Management</h1><p className="mt-2 text-slate-500">Monitor availability and update product stock.</p></div><button className="btn-outline" disabled={inventory.isFetching} onClick={() => inventory.refetch()}>{inventory.isFetching ? "Loading…" : "Refresh"}</button></div>
    {message && <p role="status" className="rounded bg-green-50 p-3 text-green-700">{message}</p>}
    {selected !== null && <StockEditor key={selected} productId={selected} onClose={() => setSelected(null)} onSaved={setMessage} />}
    {inventory.isSuccess && <div className="grid gap-4 sm:grid-cols-3">{[["Products", products.length], ["Units available", products.reduce((sum, product) => sum + Number(product.quantity ?? 0), 0)], ["Out of stock", products.filter((product) => Number(product.quantity) === 0).length]].map(([label, value]) => <div key={label} className="panel p-5"><p className="text-sm text-slate-500">{label}</p><p className="mt-2 text-2xl font-bold">{value}</p></div>)}</div>}
    <div className="panel flex flex-wrap gap-4 p-5"><div className="min-w-0 flex-1"><label className="field-label" htmlFor="inventory-search">Search products or categories</label><input id="inventory-search" className="field-input" value={search} onChange={(event) => setSearch(event.target.value)} /></div><div><label className="field-label" htmlFor="availability">Availability</label><select id="availability" className="field-input" value={availability} onChange={(event) => setAvailability(event.target.value)}><option value="all">All products</option><option value="in">In stock</option><option value="out">Out of stock</option></select></div></div>
    {inventory.isPending ? <p role="status" className="panel p-8">Loading inventory…</p> : inventory.isError ? <p role="alert" className="panel p-8 text-red-700">Unable to load inventory. <button className="underline" onClick={() => inventory.refetch()}>Retry</button></p> : visible.length === 0 ? <p className="panel p-8">{products.length ? "No products match these filters." : "Add products in Product Management to start tracking stock."}</p> : <div className="panel overflow-x-auto"><table className="w-full text-left text-sm"><thead className="border-b bg-slate-50"><tr>{["Product", "Category", "Stock", "Availability", "Actions"].map((title) => <th key={title} scope="col" className="p-4">{title}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{visible.map((product) => <tr key={product.productId}><td className="p-4"><Link className="font-bold hover:text-orange-700" to={`/products/${product.productId}`}>{product.name}</Link><p className="text-xs text-slate-500">#{product.productId}</p></td><td className="p-4">{product.category?.name || "—"}</td><td className="p-4 font-semibold">{product.quantity}</td><td className={`p-4 ${Number(product.quantity) > 0 ? "text-green-700" : "text-red-700"}`}>{Number(product.quantity) > 0 ? "In stock" : "Out of stock"}</td><td className="p-4"><button className="btn-outline whitespace-nowrap" disabled={selected !== null} onClick={() => { setSelected(product.productId); setMessage(""); }}>Update stock</button></td></tr>)}</tbody></table></div>}
  </section>;
}
