import apiClient from "./apiClient";

export async function getCart(userId) {
  return (await apiClient.get(`/api/cart/${userId}`)).data;
}

export async function addToCart({ userId, productId, quantity }) {
  return (
    await apiClient.post(`/api/cart/${userId}/items/${productId}`, null, {
      params: { quantity },
    })
  ).data;
}

export async function updateCartQuantity({ userId, productId, quantity }) {
  return (
    await apiClient.put(`/api/cart/${userId}/items/${productId}`, null, {
      params: { quantity },
    })
  ).data;
}

export async function removeCartItem({ userId, productId }) {
  await apiClient.delete(`/api/cart/${userId}/items/${productId}`);
}

export async function checkoutCart(userId, checkoutKey, details) {
  return (await apiClient.post(`/api/cart/${userId}/checkout`, details, { headers: { "X-Checkout-Key": checkoutKey } })).data;
}
