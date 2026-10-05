import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { browseProducts, deleteProduct } from "../services/productService.js";
import { getCategories } from "../services/categoryService.js";
import ProductImage from "./ProductImage.jsx";
import Icon from "./Icon.jsx";
import ProductForm from "./ProductForm.jsx";

const PAGE_SIZE = 9;

export default function ProductBrowser({ management = false }) {
  const client = useQueryClient();
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [params, setParams] = useSearchParams();
  const [editor, setEditor] = useState(null);
  const [message, setMessage] = useState("");
  const categories = useQuery({ queryKey: ["categories"], queryFn: getCategories });
  const remove = useMutation({ mutationFn: deleteProduct, onSuccess: async () => {
    setMessage("Product deleted successfully.");
    await Promise.all([client.invalidateQueries({ queryKey: ["products"] }), client.invalidateQueries({ queryKey: ["inventory"] })]);
  }});
  const search = params.get("search") ?? "";
  const category = params.get("categoryId") ?? "";
  const min = params.get("min") ?? "";
  const max = params.get("max") ?? "";
  const sort = params.get("sort") ?? "name";
  const requestedAvailability = params.get("availability");
  const availability = ["in", "out"].includes(requestedAvailability) ? requestedAvailability : "";
  const invalidRange = min !== "" && max !== "" && Number(min) > Number(max);
  function filter(name, value) {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (value) next.set(name, value); else next.delete(name);
      next.delete("page");
      return next;
    }, { replace: true });
  }
  const requestedPage = Number(params.get("page") ?? 1);
  const queryPage = Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1;
  const query = { page: queryPage, size: PAGE_SIZE, search, categoryId: category || undefined,
    min: min || undefined, max: max || undefined, availability, sort };
  const products = useQuery({ queryKey: ["products", "browse", query], queryFn: () => browseProducts(query), enabled: !invalidRange });
  const total = products.data?.totalElements ?? 0;
  const pageCount = products.data?.totalPages ?? 1;
  const page = products.data?.page ?? queryPage;
  const start = (page - 1) * PAGE_SIZE;
  const pageProducts = products.data?.content ?? [];
  function changePage(nextPage) {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (nextPage === 1) next.delete("page"); else next.set("page", String(nextPage));
      return next;
    });
  }

  return <div className="space-y-6">
    <div className="catalogue-heading flex flex-wrap items-center justify-between gap-4">
      <div><p className="eyebrow">{management ? "Store management" : "Explore the catalogue"}</p>
        <h1 className="mt-2 text-3xl font-bold">{management ? "Product Management" : "Our Products"}</h1>
        <p className="mt-2 text-slate-500">{management ? "Create and maintain your product catalogue." : "Find tools, electronics and supplies for your next project."}</p></div>
      <div className="flex gap-2"><button className="btn-outline" disabled={products.isFetching} onClick={() => products.refetch()}>{products.isFetching ? "Loading…" : "Refresh"}</button>
        {management && <button className="btn-primary" disabled={editor !== null || remove.isPending} onClick={() => { setMessage(""); remove.reset(); setEditor({}); }}>+ Add Product</button>}</div>
    </div>
    {message && <p role="status" className="rounded bg-green-50 p-3 text-green-700">{message}</p>}
    {remove.isError && <p role="alert" className="rounded bg-red-50 p-3 text-red-700">{remove.error.response?.data?.message || "Unable to delete the product. It may be referenced by a cart or order."}</p>}
    {editor && <ProductForm key={editor.productId ?? "new"} product={editor.productId ? editor : null} onClose={() => setEditor(null)} onSaved={setMessage} />}
    <div className="catalogue-layout grid gap-6 lg:grid-cols-[230px_1fr]">
      <aside className={`filter-panel h-fit ${filtersOpen ? "is-open" : ""}`}>
        <div className="filter-title"><h2><Icon name="filter" />Filters</h2><button type="button" className="filter-toggle" aria-expanded={filtersOpen} aria-controls="filter-content" onClick={() => setFiltersOpen(!filtersOpen)}>{filtersOpen ? "Hide filters" : "Show filters"}</button></div>
        <div id="filter-content" className="filter-content">
        <label className="field-label" htmlFor="product-search">Search products</label>
        <input id="product-search" className="field-input" value={search} onChange={(e) => filter("search", e.target.value)} placeholder="Name or description" />
        <label className="field-label mt-4" htmlFor="category-filter">Category</label>
        <select id="category-filter" className="field-input" value={category} onChange={(e) => filter("categoryId", e.target.value)}>
          <option value="">All categories</option>
          {(categories.data ?? []).map((item) => <option key={item.categoryId} value={item.categoryId}>{item.name}</option>)}
        </select>
        {categories.isError && <p className="field-error">Categories unavailable. <button onClick={() => categories.refetch()} className="underline">Retry</button></p>}
        <label className="field-label mt-4" htmlFor="product-availability">Availability</label>
        <select id="product-availability" className="field-input" value={availability} onChange={(e) => filter("availability", e.target.value)}>
          <option value="">All products</option>
          <option value="in">In stock</option>
          <option value="out">Out of stock</option>
        </select>
        <div className="mt-4 grid grid-cols-2 gap-2">
          <div><label className="field-label" htmlFor="min-price">Min price</label><input id="min-price" className="field-input" type="number" min="0" value={min} onChange={(e) => filter("min", e.target.value)} /></div>
          <div><label className="field-label" htmlFor="max-price">Max price</label><input id="max-price" className="field-input" type="number" min="0" value={max} onChange={(e) => filter("max", e.target.value)} /></div>
        </div>
        {invalidRange && <p role="alert" className="field-error">Minimum price must not exceed maximum.</p>}
        <button className="mt-5 text-sm font-semibold text-orange-700 hover:underline" onClick={() => setParams({})}>Clear filters</button>
        </div>
      </aside>
      <div className="min-w-0">
        <div className="catalogue-toolbar mb-4 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">
          <span role="status">{products.isSuccess ? total ? `Showing ${start + 1}–${start + pageProducts.length} of ${total} products` : "0 products" : "Catalogue"}</span>
          <label className="flex items-center gap-2">Sort by<select className="rounded border border-slate-200 bg-white p-2" value={sort} onChange={(e) => filter("sort", e.target.value)}><option value="name">Name</option><option value="price-low">Price: low to high</option><option value="price-high">Price: high to low</option></select></label>
        </div>
        {invalidRange ? <p className="panel p-8">Correct the price range to browse products.</p> : products.isPending ? <p role="status" className="panel p-8">Loading products…</p> : products.isError ? <div role="alert" className="panel p-8 text-red-700">Unable to load products. Check that Spring Boot is running, then use Refresh.</div> : total === 0 ? <div className="panel p-12 text-center"><h2 className="font-bold">No products found</h2><p className="mt-2 text-sm text-slate-500">{total ? "Try changing your filters." : "Products will appear here after they are added."}</p></div> : management ? (
          <div className="panel overflow-x-auto"><table className="w-full text-left text-sm"><thead className="border-b bg-slate-50 text-slate-500"><tr>{["Product", "Category", "Price", "Stock", "Actions"].map((title) => <th key={title} scope="col" className="p-4">{title}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">
            {pageProducts.map((product) => <tr key={product.productId} className="hover:bg-slate-50"><td className="p-4"><Link className="font-bold hover:text-orange-700" to={`/products/${product.productId}`}>{product.name}</Link><p className="mt-1 max-w-xs truncate text-slate-500">{product.description || "—"}</p></td><td className="p-4">{product.category?.name}</td><td className="whitespace-nowrap p-4">Rs. {Number(product.price).toLocaleString("en-LK", { minimumFractionDigits: 2 })}</td><td className={`whitespace-nowrap p-4 font-semibold ${Number(product.quantity ?? 0) > 0 ? "text-green-700" : "text-red-700"}`}>{Number(product.quantity ?? 0) > 0 ? `${product.quantity} in stock` : "Out of stock"}</td><td className="p-4"><div className="flex gap-2"><button className="btn-outline" disabled={editor !== null || remove.isPending} onClick={() => { setMessage(""); remove.reset(); setEditor(product); }}>Edit</button><button className="btn-danger" disabled={editor !== null || remove.isPending} onClick={() => { if (window.confirm(`Permanently delete "${product.name}"?`)) { setMessage(""); remove.mutate(product.productId); } }}>{remove.isPending && remove.variables === product.productId ? "Deleting…" : "Delete"}</button></div></td></tr>)}
          </tbody></table></div>
        ) : <div className="product-grid grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{pageProducts.map((product) => <article key={product.productId} className="product-card">
          <Link className="product-photo" to={`/products/${product.productId}`} aria-label={`View ${product.name}`}><ProductImage product={product} className="h-48 w-full p-5" /><span className={`stock-badge ${Number(product.quantity ?? 0) > 0 ? "" : "is-out"}`}>{Number(product.quantity ?? 0) > 0 ? "In stock" : "Out of stock"}</span></Link>
          <div className="product-card-body"><p className="eyebrow">{product.category?.name}</p><h2><Link to={`/products/${product.productId}`}>{product.name}</Link></h2><p className="product-description line-clamp-2">{product.description || "View product details."}</p>
            <div className="product-card-bottom"><div><p className="product-price">Rs. {Number(product.price).toLocaleString("en-LK", { minimumFractionDigits: 2 })}</p><p className="product-stock">{Number(product.quantity ?? 0) > 0 ? `${product.quantity} available` : "Currently unavailable"}</p></div><Link className="product-link" to={`/products/${product.productId}`}>View Details <Icon name="arrow" /></Link></div>
          </div></article>)}</div>}
        {products.isSuccess && total > 0 && <nav aria-label="Product pages" className="pagination mt-6 flex flex-wrap items-center justify-between gap-3">
          <button className="btn-outline" disabled={page === 1} onClick={() => changePage(page - 1)}>Previous</button>
          <span className="page-position text-sm text-slate-500">Page <strong>{page}</strong> of {pageCount}</span>
          <button className="btn-outline" disabled={page === pageCount} onClick={() => changePage(page + 1)}>Next</button>
        </nav>}
      </div>
    </div>
  </div>;
}
