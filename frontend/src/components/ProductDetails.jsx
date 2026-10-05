import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { getProduct } from "../services/productService.js";
import { addToCart } from "../services/cartService.js";
import { useAuth } from "../auth/authContext.js";
import ProductImage from "./ProductImage.jsx";

export default function ProductDetails() {
  const { user } = useAuth();
  const { productId } = useParams();
  const valid = /^\d+$/.test(productId) && Number(productId) > 0;
  const [quantity, setQuantity] = useState(1);
  const client = useQueryClient();
  const query = useQuery({
    queryKey: ["products", productId],
    queryFn: () => getProduct(productId),
    enabled: valid,
  });
  const add = useMutation({
    mutationFn: addToCart,
    onSuccess: () => {
      if (user) client.invalidateQueries({ queryKey: ["cart", user.id] });
    },
  });

  if (!valid || query.isError) {
    return (
      <section className="panel p-10">
        <h1 className="text-2xl font-bold">
          {!valid || query.error?.response?.status === 404
            ? "Product not found"
            : "Unable to load product"}
        </h1>
        <p className="my-4 text-slate-500">
          {query.error?.response?.data?.message || "Check the product link or try again."}
        </p>
        <Link className="btn-primary" to="/products">Back to Products</Link>
        {valid && <button className="btn-outline ml-3" onClick={() => query.refetch()}>Retry</button>}
      </section>
    );
  }

  if (query.isPending) return <p role="status">Loading product…</p>;

  const product = query.data;
  const stock = Number(product.quantity ?? 0);
  const addError = add.error?.response?.data?.message;

  return (
    <div>
      <Link className="text-sm font-semibold text-orange-700" to="/products">← Back to Products</Link>
      <article className="detail-panel panel mt-6 grid overflow-hidden md:grid-cols-2">
        <div className="detail-photo"><ProductImage product={product} className="h-72 w-full p-8 md:h-96" /></div>
        <div className="detail-content">
          <Link to={`/products?categoryId=${product.category?.categoryId}`} className="eyebrow">
            {product.category?.name}
          </Link>
          <h1 className="mt-3 text-3xl font-bold">{product.name}</h1>
          <p className="mt-5 text-2xl font-bold">
            Rs. {Number(product.price).toLocaleString("en-LK", { minimumFractionDigits: 2 })}
          </p>
          <p className={`mt-2 text-sm font-semibold ${stock > 0 ? "text-green-700" : "text-red-700"}`}>
            {stock > 0 ? `${stock} in stock` : "Out of stock"}
          </p>
          <h2 className="mt-8 font-bold">Product description</h2>
          <p className="mt-3 whitespace-pre-wrap leading-relaxed text-slate-600">
            {product.description || "No description has been added for this product."}
          </p>

          <div className="mt-8 flex flex-wrap items-end gap-3">
            <div>
              <label className="field-label" htmlFor="cart-quantity">Quantity</label>
              <input
                id="cart-quantity"
                className="field-input w-24"
                type="number"
                min="1"
                max={Math.max(stock, 1)}
                value={quantity}
                disabled={stock === 0 || add.isPending}
                onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))}
              />
            </div>
            {user ? (
              <button
                type="button"
                className="btn-primary"
                disabled={stock === 0 || quantity > stock || add.isPending}
                onClick={() => add.mutate({ userId: user.id, productId: product.productId, quantity })}
              >
                {add.isPending ? "Adding…" : "Add to Cart"}
              </button>
            ) : (
              <Link className="btn-primary" to="/login" state={{ from: `/products/${product.productId}` }}>
                Sign In to Add to Cart
              </Link>
            )}
            {add.isSuccess && <Link className="btn-outline" to="/cart">View Cart</Link>}
          </div>

          {quantity > stock && stock > 0 && (
            <p role="alert" className="mt-3 text-sm font-semibold text-red-700">
              Only {stock} units are available.
            </p>
          )}
          {add.isSuccess && (
            <p role="status" className="mt-3 text-sm font-semibold text-green-700">
              {quantity} × {product.name} added to the cart.
            </p>
          )}
          {add.isError && (
            <p role="alert" className="mt-3 text-sm font-semibold text-red-700">
              {addError || "Unable to add this product to the cart."}
            </p>
          )}
        </div>
      </article>
    </div>
  );
}
