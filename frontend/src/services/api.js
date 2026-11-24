import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import { logout } from '../features/auth/authSlice';

const rawBaseQuery = fetchBaseQuery({
  baseUrl: import.meta.env.VITE_API_URL || '/api',
  prepareHeaders: (headers, { getState }) => {
    const token = getState().auth.token;
    if (token) headers.set('Authorization', `Bearer ${token}`);
    return headers;
  },
});

// An expired or invalid token (401) on any authenticated call logs the user out.
const baseQuery = async (args, api, extra) => {
  const result = await rawBaseQuery(args, api, extra);
  const url = typeof args === 'string' ? args : args.url;
  if (result.error?.status === 401 && api.getState().auth.token && !url.startsWith('/auth/')) {
    api.dispatch(logout());
  }
  return result;
};

const inv = (...tags) => (_r, _e) => tags;

export const api = createApi({
  reducerPath: 'api',
  baseQuery,
  tagTypes: ['Product', 'Cart', 'Favourite', 'Order', 'User', 'Shipment', 'Payment', 'Audit'],
  endpoints: (b) => ({
    // ---- auth & users ----
    login: b.mutation({ query: (body) => ({ url: '/auth/login', method: 'POST', body }) }),
    register: b.mutation({ query: (body) => ({ url: '/auth/register', method: 'POST', body }) }),
    getMe: b.query({ query: () => '/users/me', providesTags: ['User'] }),
    updateUser: b.mutation({
      query: ({ id, ...body }) => ({ url: `/users/${id}`, method: 'PUT', body }),
      invalidatesTags: ['User'],
    }),
    getUsers: b.query({ query: () => '/users', providesTags: ['User'] }),
    deleteUser: b.mutation({ query: (id) => ({ url: `/users/${id}`, method: 'DELETE' }), invalidatesTags: ['User'] }),

    // ---- products ----
    getProducts: b.query({
      query: ({ category, name } = {}) => ({ url: '/products', params: { category: category || undefined, name: name || undefined } }),
      providesTags: (res) => [...(res ?? []).map(({ id }) => ({ type: 'Product', id })), { type: 'Product', id: 'LIST' }],
    }),
    getProduct: b.query({ query: (id) => `/products/${id}`, providesTags: (_r, _e, id) => [{ type: 'Product', id }] }),
    createProduct: b.mutation({
      query: (body) => ({ url: '/products', method: 'POST', body }),
      invalidatesTags: [{ type: 'Product', id: 'LIST' }],
    }),
    updateProduct: b.mutation({
      query: ({ id, ...body }) => ({ url: `/products/${id}`, method: 'PUT', body }),
      invalidatesTags: (_r, _e, { id }) => [{ type: 'Product', id }, { type: 'Product', id: 'LIST' }],
    }),
    deleteProduct: b.mutation({
      query: (id) => ({ url: `/products/${id}`, method: 'DELETE' }),
      invalidatesTags: [{ type: 'Product', id: 'LIST' }],
    }),

    // ---- cart ----
    getCart: b.query({ query: () => '/cart', providesTags: ['Cart'] }),
    addToCart: b.mutation({
      query: (body) => ({ url: '/cart/items', method: 'POST', body }),
      invalidatesTags: ['Cart'],
    }),
    removeFromCart: b.mutation({
      query: (productId) => ({ url: `/cart/items/${productId}`, method: 'DELETE' }),
      invalidatesTags: ['Cart'],
    }),
    clearCart: b.mutation({ query: () => ({ url: '/cart', method: 'DELETE' }), invalidatesTags: ['Cart'] }),
    // The API only adds (increments) or removes a line, so setting an exact quantity = remove + add.
    setCartQuantity: b.mutation({
      async queryFn({ productId, quantity }, _api, _x, baseQ) {
        const del = await baseQ({ url: `/cart/items/${productId}`, method: 'DELETE' });
        if (del.error) return { error: del.error };
        if (quantity <= 0) return { data: null };
        const add = await baseQ({ url: '/cart/items', method: 'POST', body: { productId, quantity } });
        return add.error ? { error: add.error } : { data: add.data };
      },
      invalidatesTags: ['Cart'],
    }),

    // ---- favourites ----
    getFavourites: b.query({ query: () => '/favourites', providesTags: ['Favourite'] }),
    addFavourite: b.mutation({
      query: (productId) => ({ url: `/favourites/${productId}`, method: 'POST' }),
      invalidatesTags: ['Favourite'],
    }),
    removeFavourite: b.mutation({
      query: (productId) => ({ url: `/favourites/${productId}`, method: 'DELETE' }),
      invalidatesTags: ['Favourite'],
    }),

    // ---- orders, payments, shipments ----
    // 402 means "order created but payment declined": the body is still the order, so callers inspect error.data.
    checkout: b.mutation({
      query: (body) => ({ url: '/orders/checkout', method: 'POST', body }),
      invalidatesTags: ['Cart', 'Order', 'Product', 'Payment'],
    }),
    getOrders: b.query({ query: () => '/orders', providesTags: ['Order'] }),
    getAllOrders: b.query({ query: () => '/orders/all', providesTags: ['Order'] }),
    getOrder: b.query({ query: (id) => `/orders/${id}`, providesTags: (_r, _e, id) => [{ type: 'Order', id }, 'Order'] }),
    getPayments: b.query({ query: () => '/payments', providesTags: ['Payment'] }),
    getPaymentsForOrder: b.query({ query: (id) => `/payments/order/${id}`, providesTags: ['Payment'] }),
    getShipmentForOrder: b.query({ query: (id) => `/shipments/order/${id}`, providesTags: ['Shipment'] }),
    trackShipment: b.query({ query: (tn) => `/shipments/track/${tn}`, providesTags: ['Shipment'] }),
    updateShipmentStatus: b.mutation({
      query: ({ id, status }) => ({ url: `/shipments/${id}/status`, method: 'PUT', body: { status } }),
      invalidatesTags: ['Shipment', 'Order'],
    }),

    // ---- audit (admin) ----
    getAudit: b.query({
      query: ({ eventType, source, entityId } = {}) => ({
        url: '/audit',
        params: { eventType: eventType || undefined, source: source || undefined, entityId: entityId || undefined },
      }),
      providesTags: ['Audit'],
    }),
  }),
});

export const {
  useLoginMutation, useRegisterMutation, useGetMeQuery, useUpdateUserMutation, useGetUsersQuery, useDeleteUserMutation,
  useGetProductsQuery, useGetProductQuery, useCreateProductMutation, useUpdateProductMutation, useDeleteProductMutation,
  useGetCartQuery, useAddToCartMutation, useRemoveFromCartMutation, useClearCartMutation, useSetCartQuantityMutation,
  useGetFavouritesQuery, useAddFavouriteMutation, useRemoveFavouriteMutation,
  useCheckoutMutation, useGetOrdersQuery, useGetAllOrdersQuery, useGetOrderQuery,
  useGetPaymentsQuery, useGetPaymentsForOrderQuery, useGetShipmentForOrderQuery, useTrackShipmentQuery, useUpdateShipmentStatusMutation,
  useGetAuditQuery,
} = api;
