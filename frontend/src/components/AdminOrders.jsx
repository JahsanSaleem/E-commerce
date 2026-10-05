import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getCustomerOrders, getOrderItems, getOrdersByStatus, updateOrderStatus } from "../services/orderService.js";

const transitions = {
  PENDING: ["CONFIRMED", "CANCELLED"], CONFIRMED: ["PROCESSING", "CANCELLED"],
  PROCESSING: ["SHIPPED", "CANCELLED"], SHIPPED: ["DELIVERED"], DELIVERED: [], CANCELLED: [],
};
const statuses = ["PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"];
const errorMessage = (error) => error?.response?.data?.message || "Unable to complete the request. Please try again.";

function OrderCard({ order, onSaved }) {
  const client = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const [status, setStatus] = useState(order.status);
  const items = useQuery({ queryKey: ["order-items", order.orderId], queryFn: () => getOrderItems(order.orderId), enabled: expanded });
  const update = useMutation({
    mutationFn: updateOrderStatus,
    onSuccess: async () => {
      onSaved(`Order #${order.orderId} status updated to ${status}.`);
      await Promise.all([client.invalidateQueries({ queryKey: ["admin-orders"] }), client.invalidateQueries({ queryKey: ["products"] }), client.invalidateQueries({ queryKey: ["inventory"] })]);
    },
  });
  return <article className="panel p-5">
    <div className="flex flex-wrap justify-between gap-4">
      <div><h2 className="text-lg font-bold">Order #{order.orderId}</h2><p className="mt-1 text-sm text-slate-500">Customer #{order.customerId} · {new Date(order.orderDate).toLocaleString()}</p></div>
      <div className="text-right"><p className="font-bold">Rs. {Number(order.totalAmount).toFixed(2)}</p><span className="mt-2 inline-block rounded-full bg-orange-100 px-3 py-1 text-xs font-bold text-orange-800">{order.status}</span></div>
    </div>
    <form className="mt-5 flex flex-wrap items-end gap-3" onSubmit={(event) => { event.preventDefault(); if (status !== "CANCELLED" || window.confirm("Cancel this order and restore its stock?")) update.mutate({ orderId: order.orderId, status }); }}>
      <div><label className="field-label" htmlFor={`status-${order.orderId}`}>Order status</label><select id={`status-${order.orderId}`} className="field-input" value={status} disabled={update.isPending} onChange={(event) => { setStatus(event.target.value); update.reset(); }}>{[order.status, ...transitions[order.status]].map((value) => <option key={value}>{value}</option>)}</select></div>
      <button className="btn-primary" disabled={update.isPending || status === order.status}>{update.isPending ? "Saving…" : "Update status"}</button>
      <button type="button" className="btn-outline" aria-expanded={expanded} onClick={() => setExpanded(!expanded)}>{expanded ? "Hide items" : "View items"}</button>
    </form>
    {transitions[order.status].length === 0 && <p className="mt-3 text-sm text-slate-500">This order is final; its status cannot be changed.</p>}
    {update.isError && <p role="alert" className="field-error">{errorMessage(update.error)}</p>}
    {update.isSuccess && <p role="status" className="mt-3 text-sm text-green-700">Order status updated.</p>}
    {expanded && <div className="mt-5 border-t border-slate-200 pt-4">
      {items.isPending ? <p role="status">Loading items…</p> : items.isError ? <p role="alert" className="text-red-700">{errorMessage(items.error)} <button className="underline" onClick={() => items.refetch()}>Retry</button></p> : items.data.length === 0 ? <p>No order items found.</p> : <ul className="divide-y divide-slate-100">{items.data.map((item) => <li key={item.orderItemId} className="flex justify-between gap-4 py-3 text-sm"><span>{item.productName} × {item.quantity}</span><span className="font-semibold">Rs. {(Number(item.unitPrice) * item.quantity).toFixed(2)}</span></li>)}</ul>}
    </div>}
  </article>;
}

export default function AdminOrders() {
  const [message, setMessage] = useState("");
  const [mode, setMode] = useState("status");
  const [status, setStatus] = useState("PENDING");
  const [customerInput, setCustomerInput] = useState("");
  const [customerId, setCustomerId] = useState("");
  const orders = useQuery({
    queryKey: ["admin-orders", mode, mode === "status" ? status : customerId],
    queryFn: () => mode === "status" ? getOrdersByStatus(status) : getCustomerOrders(customerId),
    enabled: mode === "status" || Boolean(customerId),
  });
  return <section className="space-y-6">
    <div className="flex flex-wrap items-center justify-between gap-4"><div><p className="eyebrow">Store management</p><h1 className="mt-2 text-3xl font-bold">Order Management</h1><p className="mt-2 text-slate-500">Review customer purchases and update their status.</p></div><button className="btn-outline" disabled={orders.isFetching || (mode === "customer" && !customerId)} onClick={() => orders.refetch()}>Refresh</button></div>
    {message && <p role="status" className="rounded bg-green-50 p-3 text-green-700">{message}</p>}
    <div className="panel flex flex-wrap items-end gap-4 p-5">
      <div><label className="field-label" htmlFor="order-mode">Find orders by</label><select id="order-mode" className="field-input" value={mode} onChange={(event) => setMode(event.target.value)}><option value="status">Status</option><option value="customer">Customer ID</option></select></div>
      {mode === "status" ? <div><label className="field-label" htmlFor="order-filter">Status</label><select id="order-filter" className="field-input" value={status} onChange={(event) => setStatus(event.target.value)}>{statuses.map((value) => <option key={value}>{value}</option>)}</select></div> : <form className="flex items-end gap-3" onSubmit={(event) => { event.preventDefault(); const id = Number(customerInput); if (Number.isSafeInteger(id) && id > 0) setCustomerId(String(id)); }}><div><label className="field-label" htmlFor="customer-id">Customer ID</label><input id="customer-id" className="field-input" type="number" min="1" step="1" required value={customerInput} onChange={(event) => setCustomerInput(event.target.value)} /></div><button className="btn-primary">Find orders</button></form>}
    </div>
    {mode === "customer" && !customerId ? <p className="panel p-8">Enter a customer ID to view their orders.</p> : orders.isPending ? <p role="status" className="panel p-8">Loading orders…</p> : orders.isError ? <p role="alert" className="panel p-8 text-red-700">{errorMessage(orders.error)} <button className="underline" onClick={() => orders.refetch()}>Retry</button></p> : orders.data.length === 0 ? <p className="panel p-8">No orders found for this {mode === "status" ? "status" : "customer"}.</p> : <div className="space-y-4">{[...orders.data].sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate)).map((order) => <OrderCard key={`${order.orderId}-${order.status}`} order={order} onSaved={setMessage} />)}</div>}
  </section>;
}
