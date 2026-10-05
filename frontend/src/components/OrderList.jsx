import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { getCustomerOrders, getOrderItems } from "../services/orderService.js";
import { useAuth } from "../auth/authContext.js";

function OrderList() {
    const { user } = useAuth();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [expandedOrderId, setExpandedOrderId] = useState(null);
    const [itemsByOrder, setItemsByOrder] = useState({});
    const [itemsError, setItemsError] = useState("");

    const fetchOrders = async () => {
        try {
            setLoading(true);
            setError("");

            const data = await getCustomerOrders(user.id);
            setOrders(data);
        } catch (err) {
            setError(err.message || "Unable to load orders");
        } finally {
            setLoading(false);
        }
    };

    const toggleOrderItems = async (orderId) => {
        if (expandedOrderId === orderId) {
            setExpandedOrderId(null);
            return;
        }

        setExpandedOrderId(orderId);
        setItemsError("");

        if (!itemsByOrder[orderId]) {
            try {
                const items = await getOrderItems(orderId);
                setItemsByOrder((current) => ({ ...current, [orderId]: items }));
            } catch (err) {
                setItemsError(
                    err.response?.data?.message || "Unable to load order items"
                );
            }
        }
    };

    useEffect(() => {
        let active = true;

        getCustomerOrders(user.id)
            .then((data) => {
                if (active) {
                    setOrders(data);
                    setLoading(false);
                }
            })
            .catch((err) => {
                if (active) {
                    setError(
                        err.response?.data?.message || "Unable to load orders"
                    );
                    setLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [user.id]);

    const formatDate = (dateValue) => {
        if (!dateValue) {
            return "Date unavailable";
        }

        return new Date(dateValue).toLocaleString();
    };

    const getStatusClasses = (status) => {
        switch (status) {
            case "PENDING":
                return "bg-amber-100 text-amber-700";
            case "CONFIRMED":
                return "bg-blue-100 text-blue-700";
            case "PROCESSING":
                return "bg-indigo-100 text-indigo-700";
            case "SHIPPED":
                return "bg-purple-100 text-purple-700";
            case "DELIVERED":
                return "bg-green-100 text-green-700";
            case "CANCELLED":
                return "bg-red-100 text-red-700";
            default:
                return "bg-slate-100 text-slate-700";
        }
    };

    return (
        <section
            id="orders"
            className="panel p-6 sm:p-8"
        >
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <p className="eyebrow">
                        Your purchases
                    </p>

                    <h1 className="mt-2 text-3xl font-bold text-slate-900">
                        My Orders
                    </h1>

                    <p className="mt-1 text-sm text-slate-500">
                        View your previous orders and their current status.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={fetchOrders}
                    className="btn-outline"
                >
                    Refresh Orders
                </button>
            </div>

            {loading && (
                <div className="mt-6 rounded-lg bg-slate-50 p-6 text-center text-sm text-slate-500">
                    Loading orders...
                </div>
            )}

            {error && (
                <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">
                    {error}
                </div>
            )}

            {!loading && !error && orders.length === 0 && (
                <div className="mt-6 rounded-lg bg-slate-50 p-8 text-center">
                    <p className="font-semibold text-slate-700">
                        You have no orders yet.
                    </p>

                    <p className="mt-1 text-sm text-slate-500">
                        Orders will appear here after you complete checkout.
                    </p>
                    <Link to="/products" className="btn-primary mt-6">Browse products</Link>
                </div>
            )}

            {!loading && !error && orders.length > 0 && (
                <div className="mt-6 space-y-4">
                    {orders.map((order) => (
                        <article
                            key={order.orderId}
                            className="rounded-lg border border-slate-200 bg-slate-50 p-5"
                        >
                            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                                <div>
                                    <p className="text-sm font-semibold text-slate-500">
                                        Order #{order.orderId}
                                    </p>

                                    <p className="mt-1 text-sm text-slate-500">
                                        {formatDate(order.orderDate)}
                                    </p>
                                </div>

                                <span
                                    className={`w-fit rounded-full px-3 py-1 text-xs font-bold ${getStatusClasses(
                                        order.status
                                    )}`}
                                >
                  {order.status}
                </span>
                            </div>

                            <div className="mt-5 flex items-center justify-between border-t border-slate-200 pt-4">
                <span className="text-sm font-semibold text-slate-600">
                  Total
                </span>

                                <span className="text-lg font-bold text-slate-900">
                  Rs. {Number(order.totalAmount || 0).toFixed(2)}
                </span>
                            </div>

                            <button
                                type="button"
                                className="mt-4 text-sm font-bold text-orange-700 hover:underline"
                                onClick={() => toggleOrderItems(order.orderId)}
                            >
                                {expandedOrderId === order.orderId
                                    ? "Hide order items"
                                    : "View order items"}
                            </button>

                            {expandedOrderId === order.orderId && (
                                <div className="mt-4 rounded-md bg-white p-4">
                                    {itemsError ? (
                                        <p role="alert" className="text-sm text-red-700">{itemsError}</p>
                                    ) : !itemsByOrder[order.orderId] ? (
                                        <p role="status" className="text-sm text-slate-500">Loading order items…</p>
                                    ) : (
                                        <ul className="divide-y divide-slate-100">
                                            {itemsByOrder[order.orderId].map((item) => (
                                                <li key={item.orderItemId} className="flex justify-between gap-4 py-3 text-sm">
                                                    <span>{item.productName} × {item.quantity}</span>
                                                    <span className="font-semibold">
                                                        Rs. {(Number(item.unitPrice) * item.quantity).toFixed(2)}
                                                    </span>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            )}
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}

export default OrderList;
