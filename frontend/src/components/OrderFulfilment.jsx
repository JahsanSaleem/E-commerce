export default function OrderFulfilment({order}) {
 if(!order.fulfilment)return <p className="mt-3 text-sm text-slate-500">Earlier order — fulfilment details not recorded.</p>;
 return <div className="mt-4 space-y-1 border-t pt-3 text-sm"><p className="font-semibold">{order.fulfilment==="DELIVERY"?"Delivery":"Store collection"} · fee Rs. {Number(order.deliveryFee??0).toFixed(2)}</p><p>{order.recipientName} · {order.phone}</p>{order.address&&<p className="break-words">{order.address}</p>}</div>;
}
