import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getCategories } from "../services/categoryService.js";
import ProductImage from "./ProductImage.jsx";
import apiClient from "../services/apiClient.js";
import { saveProduct } from "../services/productService.js";

export default function ProductForm({ product, onClose, onSaved }) {
  const client = useQueryClient();
  const [form, setForm] = useState({
    name: product?.name ?? "", description: product?.description ?? "",
    price: product?.price ?? "", quantity: product?.quantity ?? 0, imageUrl: product?.imageUrl ?? "",
    categoryId: product?.category?.categoryId ?? "",
  });
  const [uploading,setUploading]=useState(false);
  const [uploadError,setUploadError]=useState("");
  async function upload(event){const file=event.target.files?.[0];if(!file)return;setUploading(true);setUploadError("");try{const data=new FormData();data.append("file",file);const result=await apiClient.post("/api/products/images",data,{headers:{"Content-Type":undefined}});setForm(previous=>({...previous,imageUrl:result.data.imageUrl}));}catch(e){setUploadError(e.response?.data?.message||"Upload failed. Choose a JPEG or PNG up to 5 MB.");}finally{setUploading(false);event.target.value="";}}
  const categories = useQuery({ queryKey: ["categories"], queryFn: getCategories });
  const mutation = useMutation({
    mutationFn: saveProduct,
    onSuccess: async () => {
      await Promise.all([client.invalidateQueries({ queryKey: ["products"] }), client.invalidateQueries({ queryKey: ["inventory"] })]);
      onSaved(product ? "Product updated successfully." : "Product created successfully.");
      onClose();
    },
  });
  const errors = mutation.error?.response?.data ?? {};
  const change = (event) => setForm({ ...form, [event.target.name]: event.target.value });
  function submit(event) {
    event.preventDefault();
    if (mutation.isPending) return;
    // The backend expects a nested Category, not a top-level categoryId.
    mutation.mutate({ productId: product?.productId, product: {
      name: form.name.trim(), description: form.description.trim() || null,
      price: form.price === "" ? null : Number(form.price),
      quantity: form.quantity === "" ? null : Number(form.quantity),
      imageUrl: form.imageUrl.trim() || null,
      category: form.categoryId === "" ? null : { categoryId: Number(form.categoryId) },
    }});
  }
  const field = (name, label, options = {}) => (
    <div>
      <label className="field-label" htmlFor={`product-${name}`}>{label}</label>
      <input id={`product-${name}`} name={name} value={form[name]} onChange={change}
        className="field-input" disabled={mutation.isPending} {...options}
        aria-invalid={Boolean(errors[name])} aria-describedby={errors[name] ? `${name}-error` : undefined} />
      {errors[name] && <p id={`${name}-error`} role="alert" className="field-error">{errors[name]}</p>}
    </div>
  );
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm" aria-labelledby="product-form-title">
      <h2 id="product-form-title" className="text-xl font-bold">{product ? "Edit Product" : "Add Product"}</h2>
      <p className="mt-1 text-sm text-slate-500">Fields marked * are required.</p>
      {mutation.isError && <p role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-700">
        {errors.message || (mutation.error.response?.status === 400 ? "Check the fields below." : "Unable to save. Check that the backend is running and try again.")}
      </p>}
      <form noValidate onSubmit={submit} className="mt-5 space-y-4">
        {field("name", "Product name *", { required: true, maxLength: 150, autoFocus: true })}
        <div className="grid gap-4 sm:grid-cols-3">
          {field("price", "Price (Rs.) *", { type: "number", step: "0.01", min: "0.01", required: true })}
          {field("quantity", "Stock quantity *", { type: "number", step: "1", min: "0", required: true })}
          <div>
            <label className="field-label" htmlFor="product-category">Category *</label>
            <select id="product-category" name="categoryId" value={form.categoryId} onChange={change}
              className="field-input" required disabled={mutation.isPending || categories.isPending || categories.isError}
              aria-invalid={Boolean(errors.category)} aria-describedby={errors.category ? "category-error" : undefined}>
              <option value="">Select a category</option>
              {(categories.data ?? []).map((category) => <option key={category.categoryId} value={category.categoryId}>{category.name}</option>)}
            </select>
            {errors.category && <p id="category-error" role="alert" className="field-error">{errors.category}</p>}
            {categories.isPending && <p role="status" className="text-sm text-slate-500">Loading categories…</p>}
            {categories.isError && <p role="alert" className="field-error">Cannot load categories. <button type="button" className="underline" onClick={() => categories.refetch()}>Retry</button></p>}
            {categories.isSuccess && categories.data.length === 0 && <p className="mt-2 text-sm text-slate-600">Create a category first in <Link className="text-orange-700 underline" to="/admin/categories">Category Management</Link>.</p>}
          </div>
        </div>
        <div><label className="field-label" htmlFor="product-upload">Upload product image</label><input id="product-upload" type="file" accept="image/jpeg,image/png" disabled={uploading||mutation.isPending} onChange={upload}/><p className="mt-1 text-sm text-slate-500">JPEG or PNG, up to 5 MB. {uploading?"Uploading…":""}</p>{uploadError&&<p role="alert" className="field-error">{uploadError}</p>}</div>
        {field("imageUrl", "Image URL", { maxLength: 500, placeholder: "https://…" })}
        {form.imageUrl&&<ProductImage product={{name:form.name||"Product preview",imageUrl:form.imageUrl}} className="h-40 w-40 border p-2"/>}
        <div>
          <label className="field-label" htmlFor="product-description">Description</label>
          <textarea id="product-description" name="description" value={form.description} onChange={change}
            maxLength={1000} rows={4} className="field-input" disabled={mutation.isPending}
            aria-invalid={Boolean(errors.description)} />
          {errors.description && <p role="alert" className="field-error">{errors.description}</p>}
        </div>
        <div className="flex gap-3">
          <button className="btn-primary" disabled={uploading || mutation.isPending || !categories.data?.length}>{mutation.isPending ? "Saving…" : "Save Product"}</button>
          <button type="button" className="btn-outline" disabled={mutation.isPending} onClick={onClose}>Cancel</button>
        </div>
      </form>
    </section>
  );
}
