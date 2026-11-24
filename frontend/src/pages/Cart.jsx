import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ShoppingCart, Trash2 } from 'lucide-react';
import { useClearCartMutation, useGetCartQuery, useGetProductsQuery, useRemoveFromCartMutation, useSetCartQuantityMutation } from '../services/api';
import { EmptyState, ErrorState, PageHeader, PageLoader, QuantityStepper } from '../components/ui';
import ProductArt from '../components/ProductArt';
import { errorMessage, formatMoney } from '../utils/format';

export function OrderSummary({ items, total, children }) {
  return (
    <aside className="card h-fit p-6 lg:sticky lg:top-24">
      <h2 className="text-lg font-bold">Order summary</h2>
      <ul className="mt-4 max-h-60 space-y-2 overflow-y-auto text-sm">
        {items.map((i) => (
          <li key={i.productId} className="flex justify-between gap-3 text-slate-600 dark:text-slate-400">
            <span className="truncate">{i.quantity} × {i.productName}</span><span className="tabular-nums">{formatMoney(i.lineTotal)}</span>
          </li>
        ))}
      </ul>
      <dl className="mt-4 space-y-2 border-t border-slate-200 pt-4 text-sm dark:border-slate-800">
        <div className="flex justify-between"><dt className="text-slate-500">Subtotal</dt><dd className="tabular-nums">{formatMoney(total)}</dd></div>
        <div className="flex justify-between"><dt className="text-slate-500">Shipping</dt><dd className="font-medium text-emerald-600">Free</dd></div>
        <div className="flex justify-between border-t border-slate-200 pt-3 text-lg font-extrabold dark:border-slate-800"><dt>Total</dt><dd className="tabular-nums">{formatMoney(total)}</dd></div>
      </dl>
      {children}
    </aside>
  );
}

export default function Cart() {
  const { data: cart, isLoading, error, refetch } = useGetCartQuery();
  const [setQty, { isLoading: updating }] = useSetCartQuantityMutation();
  const [remove] = useRemoveFromCartMutation();
  const [clear, { isLoading: clearing }] = useClearCartMutation();
  // cart lines carry no category or picture, so look the product up in the (cached) catalogue for the thumbnail
  const { data: catalogue } = useGetProductsQuery({});
  const productOf = (id) => catalogue?.find((p) => p.id === id);

  const [actionError, setActionError] = useState('');
  const run = async (fn) => {
    setActionError('');
    try { await fn().unwrap(); } catch (e) { setActionError(errorMessage(e)); refetch(); }
  };
  const change = (productId, quantity) => run(() => setQty({ productId, quantity }));
  const clearAll = () => run(() => clear());
  const removeItem = (i) => run(() => remove(i.productId));

  if (isLoading) return <PageLoader />;
  if (error) return <div className="container-page py-16"><ErrorState message={errorMessage(error)} onRetry={refetch} /></div>;
  // changing a quantity is remove + add on the backend, so sort to keep lines from jumping around
  const items = [...(cart?.items ?? [])].sort((a, b) => a.productId - b.productId);

  return (
    <div className="container-page py-10">
      {actionError && <div role="alert" className="mb-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{actionError}</div>}
      <PageHeader title="Shopping cart" subtitle={items.length ? `${items.reduce((n, i) => n + i.quantity, 0)} items` : undefined}>
        {items.length > 0 && <button onClick={clearAll} disabled={clearing} className="btn-ghost text-rose-600"><Trash2 size={16} /> Clear cart</button>}
      </PageHeader>

      {items.length === 0 ? (
        <EmptyState icon={ShoppingCart} title="Your cart is empty" text="Browse the store and add something you like." action={{ label: 'Start shopping', to: '/shop' }} />
      ) : (
        <div className="grid gap-8 lg:grid-cols-[1fr_22rem]">
          <ul className="space-y-4">
            {items.map((i) => (
              <li key={i.productId} className="card flex flex-wrap items-center gap-4 p-4 sm:flex-nowrap">
                <Link to={`/products/${i.productId}`} className="shrink-0"><ProductArt category={productOf(i.productId)?.category} imageUrl={productOf(i.productId)?.imageUrl} alt={i.productName} className="h-24 w-24 rounded-xl border border-slate-200 dark:border-slate-800" iconSize={36} /></Link>
                <div className="min-w-0 flex-1">
                  <Link to={`/products/${i.productId}`} className="line-clamp-1 font-bold hover:text-brand-600">{i.productName}</Link>
                  <p className="mt-0.5 text-sm text-slate-500">{formatMoney(i.unitPrice)} each</p>
                  <div className="mt-3"><QuantityStepper value={i.quantity} disabled={updating} onChange={(q) => change(i.productId, q)} /></div>
                </div>
                <div className="flex flex-col items-end gap-3">
                  <span className="text-lg font-extrabold tabular-nums">{formatMoney(i.lineTotal)}</span>
                  <button onClick={() => removeItem(i)} className="btn-ghost btn-sm text-rose-600" aria-label={`Remove ${i.productName}`}><Trash2 size={15} /> Remove</button>
                </div>
              </li>
            ))}
          </ul>
          <OrderSummary items={items} total={cart.total}>
            <Link to="/checkout" className="btn-primary btn-lg mt-6 w-full">Checkout <ArrowRight size={18} /></Link>
            <Link to="/shop" className="btn-ghost mt-2 w-full">Continue shopping</Link>
          </OrderSummary>
        </div>
      )}
    </div>
  );
}
