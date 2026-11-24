import { useCallback, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';
import { selectIsAuthed } from '../features/auth/authSlice';
import {
  useAddFavouriteMutation, useAddToCartMutation, useGetFavouritesQuery, useGetProductsQuery, useRemoveFavouriteMutation,
} from '../services/api';
import { errorMessage } from './format';

/** Returns a guard: call it before an authenticated action; it sends guests to /login and returns false. */
export function useRequireAuth() {
  const authed = useSelector(selectIsAuthed);
  const navigate = useNavigate();
  const location = useLocation();
  return useCallback(() => {
    if (authed) return true;
    navigate('/login', { state: { from: location.pathname + location.search } });
    return false;
  }, [authed, navigate, location]);
}

/** `error` holds the reason (e.g. "Only 3 units in stock") of the last failed attempt, to show next to the button. */
export function useAddToCart() {
  const [addToCart, { isLoading }] = useAddToCartMutation();
  const [error, setError] = useState('');
  const requireAuth = useRequireAuth();
  const add = useCallback(async (product, quantity = 1) => {
    if (!requireAuth()) return false;
    setError('');
    try {
      await addToCart({ productId: product.id, quantity }).unwrap();
      return true;
    } catch (e) {
      setError(errorMessage(e));
      return false;
    }
  }, [addToCart, requireAuth]);
  return { add, isLoading, error };
}

export function useFavourite(productId) {
  const authed = useSelector(selectIsAuthed);
  const { data } = useGetFavouritesQuery(undefined, { skip: !authed });
  const [addFav] = useAddFavouriteMutation();
  const [removeFav] = useRemoveFavouriteMutation();
  const requireAuth = useRequireAuth();
  const isFav = Boolean(data?.some((f) => f.productId === productId));
  const toggle = useCallback(async () => {
    if (!requireAuth()) return;
    try {
      if (isFav) await removeFav(productId).unwrap();
      else await addFav(productId).unwrap();
    } catch { /* the heart simply stays as it was */ }
  }, [isFav, productId, addFav, removeFav, requireAuth]);
  return { isFav, toggle };
}

/** Categories that really exist in the catalogue (most products first). Empty when the shop has no products. */
export function useCategories() {
  const { data } = useGetProductsQuery({});
  return useMemo(() => {
    const counts = {};
    (data ?? []).forEach((p) => { if (p.category) counts[p.category] = (counts[p.category] || 0) + 1; });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]).map(([name, count]) => ({ name, count }));
  }, [data]);
}
