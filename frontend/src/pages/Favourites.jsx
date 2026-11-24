import { Link } from 'react-router-dom';
import { Heart, ShoppingCart, Trash2 } from 'lucide-react';
import { useGetFavouritesQuery, useRemoveFavouriteMutation } from '../services/api';
import { EmptyState, ErrorState, PageHeader, PageLoader } from '../components/ui';
import ProductArt from '../components/ProductArt';
import { errorMessage, formatMoney } from '../utils/format';
import { useAddToCart } from '../utils/hooks';

export default function Favourites() {
  const { data, isLoading, error, refetch } = useGetFavouritesQuery();
  const [remove] = useRemoveFavouriteMutation();
  const { add } = useAddToCart();

  if (isLoading) return <PageLoader />;
  if (error) return <div className="container-page py-16"><ErrorState message={errorMessage(error)} onRetry={refetch} /></div>;

  return (
    <div className="container-page py-10">
      <PageHeader title="Favourites" subtitle="Products you've saved for later." />
      {!data?.length ? (
        <EmptyState icon={Heart} title="No favourites yet" text="Tap the heart on any product to save it here." action={{ label: 'Discover products', to: '/shop' }} />
      ) : (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {data.map((f) => {
            const p = f.product;
            return (
              <article key={f.id} className="card group flex flex-col overflow-hidden">
                <Link to={`/products/${f.productId}`}><ProductArt category={p?.category} imageUrl={p?.imageUrl} alt={p?.name} className="aspect-[4/3]" /></Link>
                <div className="flex flex-1 flex-col p-4">
                  <Link to={`/products/${f.productId}`} className="font-bold hover:text-brand-600">{p?.name ?? `Product #${f.productId}`}</Link>
                  {p?.price != null && <p className="mt-1 text-lg font-extrabold">{formatMoney(p.price)}</p>}
                  <div className="mt-4 flex gap-2">
                    <button className="btn-primary btn-sm flex-1" disabled={!p || p.stock <= 0} onClick={() => add(p)}><ShoppingCart size={14} /> Add to cart</button>
                    <button className="btn-outline btn-sm" aria-label="Remove from favourites"
                      onClick={() => remove(f.productId)}>
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
