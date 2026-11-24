import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Check, ChevronRight, Heart, ShieldCheck, ShoppingCart, Truck, Zap } from 'lucide-react';
import { useGetProductQuery, useGetProductsQuery } from '../services/api';
import ProductArt from '../components/ProductArt';
import ProductCard from '../components/ProductCard';
import { EmptyState, ErrorState, PageLoader, QuantityStepper, Spinner } from '../components/ui';
import { errorMessage, formatMoney, formatDate } from '../utils/format';
import { useAddToCart, useFavourite } from '../utils/hooks';

export default function ProductDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: product, isLoading, error, refetch } = useGetProductQuery(id);
  const { data: related } = useGetProductsQuery({ category: product?.category }, { skip: !product?.category });
  const { add, isLoading: adding, error: addError } = useAddToCart();
  const { isFav, toggle } = useFavourite(Number(id));
  const [qty, setQty] = useState(1);

  if (isLoading) return <PageLoader />;
  if (error?.status === 404) return <div className="container-page py-16"><EmptyState title="Product not found" text="It may have been removed." action={{ label: 'Back to shop', to: '/shop' }} /></div>;
  if (error) return <div className="container-page py-16"><ErrorState message={errorMessage(error)} onRetry={refetch} /></div>;

  const out = product.stock <= 0;
  const more = (related ?? []).filter((p) => p.id !== product.id && p.stock > 0).slice(0, 4);

  const buyNow = async () => { if (await add(product, qty)) navigate('/cart'); };

  return (
    <div className="container-page py-8">
      <nav className="mb-6 flex items-center gap-1.5 text-sm text-slate-500" aria-label="Breadcrumb">
        <Link to="/" className="hover:text-brand-600">Home</Link><ChevronRight size={14} />
        <Link to="/shop" className="hover:text-brand-600">Shop</Link>
        {product.category && <><ChevronRight size={14} /><Link to={`/shop?category=${product.category}`} className="hover:text-brand-600">{product.category}</Link></>}
        <ChevronRight size={14} /><span className="truncate font-medium text-slate-800 dark:text-slate-200">{product.name}</span>
      </nav>

      <div className="grid gap-10 lg:grid-cols-2">
        <div className="group"><ProductArt category={product.category} imageUrl={product.imageUrl} alt={product.name} iconSize={160} className="aspect-square rounded-3xl border border-slate-200 shadow-lift dark:border-slate-800" /></div>

        <div className="flex flex-col">
          {product.category && <span className="badge w-fit bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">{product.category}</span>}
          <h1 className="mt-3 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl dark:text-white">{product.name}</h1>
          <p className="mt-4 text-4xl font-extrabold text-slate-900 dark:text-white">{formatMoney(product.price)}</p>
          <p className="mt-5 text-base leading-relaxed text-slate-600 dark:text-slate-400">{product.description || 'No description available.'}</p>

          <div className="mt-6">
            {out ? <span className="badge bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300">Out of stock</span>
              : product.stock <= 5 ? <span className="badge bg-amber-100 text-amber-800">Hurry, only {product.stock} left</span>
              : <span className="badge bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"><Check size={13} /> In stock ({product.stock} available)</span>}
          </div>

          <div className="mt-8 flex flex-wrap items-center gap-3">
            <QuantityStepper value={qty} onChange={setQty} max={Math.max(1, product.stock)} disabled={out} />
            <button className="btn-primary btn-lg flex-1 sm:flex-none" disabled={out || adding} onClick={() => add(product, qty)}>
              {adding ? <Spinner size={18} /> : <ShoppingCart size={18} />} Add to cart
            </button>
            <button className="btn-dark btn-lg" disabled={out || adding} onClick={buyNow}><Zap size={18} /> Buy now</button>
            <button onClick={toggle} aria-pressed={isFav} aria-label={isFav ? 'Remove from favourites' : 'Add to favourites'} className="btn-outline h-[3.25rem] w-[3.25rem] p-0">
              <Heart size={20} className={isFav ? 'fill-rose-500 text-rose-500' : ''} />
            </button>
          </div>

          {addError && <p role="alert" className="mt-3 text-sm font-medium text-rose-600 dark:text-rose-400">{addError}</p>}

          <ul className="mt-8 grid gap-3 border-t border-slate-200 pt-6 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
            <li className="flex items-center gap-3"><Truck size={18} className="text-brand-600" /> Free delivery with live tracking</li>
            <li className="flex items-center gap-3"><ShieldCheck size={18} className="text-brand-600" /> Secure checkout</li>
          </ul>
          <p className="mt-4 text-xs text-slate-400">Listed {formatDate(product.createdAt)} · SKU #{product.id}</p>
        </div>
      </div>

      {more.length > 0 && (
        <section className="mt-20">
          <h2 className="mb-6 text-2xl font-extrabold tracking-tight">You may also like</h2>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">{more.map((p) => <ProductCard key={p.id} product={p} />)}</div>
        </section>
      )}
    </div>
  );
}
