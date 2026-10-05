import { Link } from "react-router-dom"
import Icon from "./Icon.jsx"
import { useEffect, useRef, useState } from 'react'
import {
    checkoutCart,
    getCart,
    removeCartItem,
    updateCartQuantity,
} from '../services/cartService.js'
import { useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/authContext.js'

function Cart() {
    const { user } = useAuth()
    const client = useQueryClient()
    const checkoutKey = useRef(null)
    const operationPending = useRef(false)
    const [busy, setBusy] = useState(false)
    const [cart, setCart] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [checkoutMessage, setCheckoutMessage] = useState('')

    const userId = user.id

    const loadCart = () => {
        setLoading(true)
        setError('')

        getCart(userId)
            .then((data) => {
                setCart(data)
                setLoading(false)
            })
            .catch((error) => {
                console.error('Failed to load cart:', error)
                setError('Unable to load your cart.')
                setLoading(false)
            })
    }

    useEffect(() => {
        let active = true

        getCart(userId)
            .then((data) => {
                if (active) {
                    setCart(data)
                    setLoading(false)
                }
            })
            .catch((error) => {
                console.error('Failed to load cart:', error)
                if (active) {
                    setError(error.response?.data?.message || 'Unable to load your cart.')
                    setLoading(false)
                }
            })

        return () => {
            active = false
        }
    }, [userId])

    const updateQuantity = (productId, quantity) => {
        if (quantity < 1) {
            return
        }

        if (operationPending.current) return
        operationPending.current = true
        setBusy(true)
        checkoutKey.current = null
        updateCartQuantity({ userId, productId, quantity })
            .then(() => {
                loadCart()
            })
            .catch((error) => {
                console.error('Failed to update quantity:', error)
                setError(error.response?.data?.message || 'Unable to update item quantity.')
            }).finally(() => { operationPending.current = false; setBusy(false) })
    }

    const removeItem = (productId) => {
        if (operationPending.current) return
        operationPending.current = true
        setBusy(true)
        checkoutKey.current = null
        removeCartItem({ userId, productId })
            .then(() => {
                loadCart()
            })
            .catch((error) => {
                console.error('Failed to remove item:', error)
                setError(error.response?.data?.message || 'Unable to remove item from cart.')
            }).finally(() => { operationPending.current = false; setBusy(false) })
    }

    const checkout = () => {
        if (operationPending.current) return
        operationPending.current = true
        setBusy(true)
        checkoutKey.current ??= crypto.randomUUID()
        setCheckoutMessage('')
        setError('')

        checkoutCart(userId, checkoutKey.current)
            .then((order) => {
                checkoutKey.current = null
                client.invalidateQueries({ queryKey: ["products"] })
                client.invalidateQueries({ queryKey: ["inventory"] })
                setCheckoutMessage(
                    `Order #${order.orderId} created successfully.`
                )
                loadCart()
            })
            .catch((error) => {
                console.error('Checkout failed:', error)
                const responseMessage = error.response?.data?.message
                setError(
                    error.response?.status < 500 && responseMessage
                        ? responseMessage
                        : 'Unable to complete checkout. Please try again.'
                )
                if (error.response?.status < 500) checkoutKey.current = null
            }).finally(() => { operationPending.current = false; setBusy(false) })
    }

    if (loading) {
        return (
            <section className="panel p-8">
                <p className="text-sm text-slate-500">Loading cart...</p>
            </section>
        )
    }

    if (error && !cart) {
        return (
            <section className="panel p-8">
                <p className="font-semibold text-red-600">{error}</p>
            </section>
        )
    }

    const items = cart?.items ?? []

    return (
        <section
            id="cart"
            className="cart-page"
        >
            <div className="catalogue-heading mb-6">
                <p className="eyebrow">
                    Your project essentials
                </p>

                <h1 className="mt-2 text-3xl font-bold text-slate-900">
                    Shopping Cart
                </h1>

                <p className="mt-2 text-slate-500">
                    Review your selected hardware items before checkout.
                </p>
            </div>

            {checkoutMessage && (
                <div className="mb-6 rounded-lg border border-green-200 bg-green-50 px-5 py-4 text-sm font-semibold text-green-700">
                    {checkoutMessage}
                </div>
            )}

            {error && (
                <div className="mb-6 rounded-lg border border-red-200 bg-red-50 px-5 py-4 text-sm font-semibold text-red-600">
                    {error}
                </div>
            )}

            {items.length === 0 ? (
                <article className="panel empty-state p-10 text-center">
                    <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-orange-100 font-bold text-orange-600">
                        <Icon name="cart" />
                    </div>

                    <h3 className="text-xl font-bold text-slate-900">
                        Your cart is empty
                    </h3>

                    <p className="mt-2 text-sm text-slate-500">
                        Add products to your cart to continue shopping.
                    </p>
                    <Link to="/products" className="btn-primary mt-6">Explore products <Icon name="arrow" /></Link>
                </article>
            ) : (
                <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
                    <article className="panel overflow-hidden">
                        <div className="border-b border-slate-200 px-6 py-4">
                            <h3 className="font-bold text-slate-900">
                                Cart Items
                            </h3>
                        </div>

                        <div>
                            {items.map((item) => {
                                const itemTotal =
                                    Number(item.unitPrice) * item.quantity

                                return (
                                    <div
                                        key={item.cartItemId}
                                        className="border-b border-slate-200 px-6 py-5 last:border-b-0"
                                    >
                                        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                                            <div className="min-w-0">
                                                <h4 className="font-bold text-slate-900">
                                                    {item.productName}
                                                </h4>

                                                <p className="mt-1 text-sm text-slate-500">
                                                    Unit Price: Rs.{' '}
                                                    {Number(
                                                        item.unitPrice
                                                    ).toFixed(2)}
                                                </p>
                                            </div>

                                            <div className="flex flex-wrap items-center gap-4">
                                                <div className="flex items-center rounded-md border border-slate-200">
                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            updateQuantity(
                                                                item.productId,
                                                                item.quantity - 1
                                                            )
                                                        }
                                                        aria-label={`Decrease quantity of ${item.productName}`}
                                                        disabled={
                                                            busy || item.quantity <= 1
                                                        }
                                                        className="px-3 py-2 font-bold text-slate-700 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40"
                                                    >
                                                        −
                                                    </button>

                                                    <span className="min-w-10 px-2 text-center text-sm font-bold text-slate-900">
                                                        {item.quantity}
                                                    </span>

                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            updateQuantity(
                                                                item.productId,
                                                                item.quantity + 1
                                                            )
                                                        }
                                                        aria-label={`Increase quantity of ${item.productName}`}
                                                        disabled={busy}
                                                        className="px-3 py-2 font-bold text-slate-700 hover:bg-slate-100"
                                                    >
                                                        +
                                                    </button>
                                                </div>

                                                <p className="min-w-28 text-right font-bold text-slate-900">
                                                    Rs.{' '}
                                                    {itemTotal.toFixed(2)}
                                                </p>

                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        removeItem(
                                                            item.productId
                                                        )
                                                    }
                                                    disabled={busy}
                                                    className="text-sm font-semibold text-red-600 hover:text-red-700"
                                                >
                                                    Remove
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    </article>

                    <aside className="panel h-fit p-6">
                        <h3 className="text-xl font-bold text-slate-900">
                            Order Summary
                        </h3>

                        <div className="mt-6 flex items-center justify-between border-b border-slate-200 pb-4">
                            <span className="text-sm text-slate-500">
                                Items
                            </span>

                            <span className="text-sm font-semibold text-slate-900">
                                {items.length}
                            </span>
                        </div>

                        <div className="mt-4 flex items-center justify-between">
                            <span className="font-bold text-slate-900">
                                Total
                            </span>

                            <span className="text-xl font-bold text-orange-600">
                                Rs.{' '}
                                {Number(cart?.totalAmount ?? 0).toFixed(2)}
                            </span>
                        </div>

                        <button
                            type="button"
                            disabled={busy}
                            onClick={checkout}
                            className="btn-primary mt-6 w-full py-3"
                        >
                            {busy ? "Please wait…" : "Proceed to Checkout"}
                        </button>
                    </aside>
                </div>
            )}
        </section>
    )
}

export default Cart
