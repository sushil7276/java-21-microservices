import { Link } from 'react-router-dom';
import { Heart, ShoppingCart } from 'lucide-react';
import ProductArt from './ProductArt';
import { Spinner } from './ui';
import { formatMoney } from '../utils/format';
import { useAddToCart, useFavourite } from '../utils/hooks';

export default function ProductCard({ product }) {
  const { add, isLoading, error } = useAddToCart();
  const { isFav, toggle } = useFavourite(product.id);
  const out = product.stock <= 0;
  const low = !out && product.stock <= 5;

  return (
    <article className="card group relative flex flex-col overflow-hidden transition duration-300 hover:-translate-y-1 hover:shadow-lift">
      <Link to={`/products/${product.id}`} className="relative block" aria-label={product.name}>
        <ProductArt category={product.category} imageUrl={product.imageUrl} alt={product.name} className="aspect-[4/3]" />
        {product.category && (
          <span className="badge absolute left-3 top-3 bg-white/90 text-slate-700 backdrop-blur">{product.category}</span>
        )}
        {out && <span className="badge absolute bottom-3 left-3 bg-slate-900/80 text-white">Sold out</span>}
        {low && <span className="badge absolute bottom-3 left-3 bg-amber-400 text-amber-950">Only {product.stock} left</span>}
      </Link>
      <button
        onClick={toggle}
        aria-pressed={isFav}
        aria-label={isFav ? 'Remove from favourites' : 'Add to favourites'}
        className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-full bg-white/90 shadow backdrop-blur transition hover:scale-110"
      >
        <Heart size={18} className={isFav ? 'fill-rose-500 text-rose-500' : 'text-slate-600'} />
      </button>

      <div className="flex flex-1 flex-col p-4">
        <Link to={`/products/${product.id}`} className="line-clamp-1 text-base font-bold text-slate-900 hover:text-brand-600 dark:text-white">
          {product.name}
        </Link>
        <p className="mt-1 line-clamp-2 min-h-[2.5rem] text-sm text-slate-500 dark:text-slate-400">{product.description}</p>
        <div className="mt-4 flex items-center justify-between gap-2">
          <span className="text-xl font-extrabold text-slate-900 dark:text-white">{formatMoney(product.price)}</span>
          <button onClick={() => add(product)} disabled={out || isLoading} className="btn-primary btn-sm" aria-label={`Add ${product.name} to cart`}>
            {isLoading ? <Spinner size={14} /> : <ShoppingCart size={15} />} Add
          </button>
        </div>
        {error && <p role="alert" className="mt-2 text-xs font-medium text-rose-600 dark:text-rose-400">{error}</p>}
      </div>
    </article>
  );
}
