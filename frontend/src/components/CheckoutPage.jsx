import { useState, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/authContext.js";
import { getCart, checkoutCart } from "../services/cartService.js";
import apiClient from "../services/apiClient.js";
import { storeContact } from "../config/storeContact.js";
export default function CheckoutPage() {
 const {user}=useAuth(); const navigate=useNavigate(); const client=useQueryClient(); const key=useRef(null);
 const [form,setForm]=useState({fulfilment:"COLLECTION",recipientName:user.name,phone:"",address:""});
 const [review,setReview]=useState(false); const [busy,setBusy]=useState(false); const [error,setError]=useState("");
 const cart=useQuery({queryKey:["checkout-cart",user.id],queryFn:()=>getCart(user.id)});
 const addresses=useQuery({queryKey:["addresses"],queryFn:async()=>(await apiClient.get("/api/account/addresses")).data});
 const options=useQuery({queryKey:["checkout-options"],queryFn:async()=>(await apiClient.get("/api/checkout/options")).data});
 const fee=form.fulfilment==="DELIVERY"?Number(options.data?.deliveryFee??0):0;
 const change=e=>{setForm({...form,[e.target.name]:e.target.value}); key.current=null;};
 async function submit(e) {
  e.preventDefault(); if(!review){setReview(true);return;} if(busy)return;
  setBusy(true);setError("");key.current??=crypto.randomUUID();
  try {const order=await checkoutCart(user.id,key.current,form); await client.invalidateQueries({queryKey:["products"]}); navigate("/orders",{state:{createdOrder:order.orderId}});}
  catch(failure){setError(failure.response?.data?.message||"Unable to place order. Retry safely.");if(failure.response?.status<500)key.current=null;}
  finally{setBusy(false);}
 }
 if(cart.isPending||options.isPending)return <p role="status">Loading checkout…</p>;
 if(cart.isError||options.isError)return <p role="alert">Unable to load checkout. <button onClick={()=>{cart.refetch();options.refetch();}}>Retry</button></p>;
 if(!cart.data.items.length)return <div className="panel p-8">Your cart is empty. <Link to="/products">Browse products</Link></div>;
 return <section className="space-y-6"><h1 className="text-3xl font-bold">{review?"Review your order":"Checkout"}</h1>
 {error&&<p role="alert" className="field-error">{error}</p>}
 <form onSubmit={submit} className="grid gap-6 lg:grid-cols-2">
 <div className="panel space-y-4 p-6">
 {review?<div className="space-y-3"><h2 className="text-xl font-bold">{form.fulfilment==="DELIVERY"?"Delivery":"Store collection"}</h2><p>{form.recipientName} · {form.phone}</p><p className="break-words">{form.fulfilment==="DELIVERY"?form.address:storeContact.address}</p><button type="button" className="btn-outline" disabled={busy} onClick={()=>setReview(false)}>Edit details</button></div>:<>
 <label className="field-label" htmlFor="fulfilment">Receive your order</label><select id="fulfilment" name="fulfilment" className="field-input" value={form.fulfilment} onChange={change}><option value="COLLECTION">Store collection — free</option><option value="DELIVERY">Delivery — Rs. {Number(options.data.deliveryFee).toFixed(2)}</option></select>
 {form.fulfilment==="DELIVERY"&&addresses.data?.length>0&&<div><label className="field-label" htmlFor="checkout-saved-address">Use a saved address</label><select id="checkout-saved-address" className="field-input" defaultValue="" onChange={e=>{const item=addresses.data.find(a=>String(a.id)===e.target.value);if(item){setForm({...form,recipientName:item.recipientName,phone:item.phone,address:item.address});key.current=null;}}}><option value="">Enter details below</option>{addresses.data.map(item=><option key={item.id} value={item.id}>{item.label}</option>)}</select></div>}
 {[['recipientName' ,'Recipient name',255],['phone','Phone number',25]].map(([name,label,max])=><div key={name}><label className="field-label" htmlFor={name}>{label}</label><input id={name} name={name} className="field-input" required maxLength={max} pattern={name==="phone"?"[+0-9 ()-]{7,25}":undefined} value={form[name]} onChange={change}/></div>)}
 {form.fulfilment==="DELIVERY"?<div><label className="field-label" htmlFor="address">Complete delivery address</label><textarea id="address" name="address" className="field-input" required minLength={8} maxLength={500} rows={3} value={form.address} onChange={change}/></div>:<p className="text-sm">Collect from {storeContact.address}. Wait for the store's collection update.</p>}
 </>}
 </div><aside className="panel space-y-4 p-6"><h2 className="text-xl font-bold">Order summary</h2><ul className="divide-y">{cart.data.items.map(item=><li key={item.productId} className="flex justify-between gap-4 py-3"><span>{item.productName} × {item.quantity}</span><span>Rs. {(Number(item.unitPrice)*item.quantity).toFixed(2)}</span></li>)}</ul>
 <p className="flex justify-between"><span>Subtotal</span><span>Rs. {Number(cart.data.totalAmount).toFixed(2)}</span></p><p className="flex justify-between"><span>{form.fulfilment==="DELIVERY"?"Delivery":"Collection"}</span><span>Rs. {fee.toFixed(2)}</span></p><p className="flex justify-between text-lg font-bold"><span>Total</span><span>Rs. {(Number(cart.data.totalAmount)+fee).toFixed(2)}</span></p>
 <p className="text-sm text-slate-500">No online payment is taken here. The store will confirm payment arrangements.</p><button className="btn-primary w-full" disabled={busy}>{busy?"Placing order…":review?"Place order":"Review order"}</button><Link to="/cart" className="block text-sm underline">Back to cart</Link></aside>
 </form></section>;
}
