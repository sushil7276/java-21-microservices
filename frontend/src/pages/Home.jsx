import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { ArrowRight, Headphones, Laptop, Package, Smartphone, Sparkles, Tablet, Watch } from 'lucide-react';
import { useGetProductsQuery } from '../services/api';
import ProductCard from '../components/ProductCard';
import { ErrorState, NoData, ProductCardSkeleton } from '../components/ui';
import ProductArt from '../components/ProductArt';
import { selectIsAuthed } from '../features/auth/authSlice';
import { errorMessage, formatMoney, categoryMeta } from '../utils/format';
import { useCategories } from '../utils/hooks';

const ICONS = { Smartphone, Laptop, Headphones, Watch, Tablet, Package };

function Hero({ spotlight }) {
  return (
    <section className="relative overflow-hidden bg-slate-950 text-white">
      <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-brand-600/40 blur-3xl" />
      <div className="absolute -bottom-40 right-0 h-[28rem] w-[28rem] rounded-full bg-fuchsia-600/30 blur-3xl" />
      <div className="container-page relative grid items-center gap-12 py-16 sm:py-24 lg:grid-cols-2">
        <div className="animate-fade-up">
          <span className="badge mb-6 bg-white/10 text-brand-200 ring-1 ring-white/15"><Sparkles size={13} /> New season, new tech</span>
          <h1 className="text-4xl font-extrabold leading-[1.05] tracking-tight sm:text-6xl">
            Upgrade your everyday with{' '}
            <span className="bg-gradient-to-r from-brand-300 via-violet-300 to-fuchsia-300 bg-clip-text text-transparent">smarter gear.</span>
          </h1>
          <p className="mt-6 max-w-lg text-lg text-slate-300">
            Phones, laptops and audio picked for quality. Free delivery on every order, with tracking from checkout to your door.
          </p>
          <div className="mt-9 flex flex-wrap gap-3">
            <Link to="/shop" className="btn-primary btn-lg">Shop now <ArrowRight size={18} /></Link>
            <Link to="/shop?category=Laptops" className="btn btn-lg border border-white/20 text-white hover:bg-white/10">Browse laptops</Link>
          </div>
        </div>

        <div className="relative mx-auto hidden w-full max-w-md lg:block">
          <div className="absolute inset-0 rotate-6 rounded-3xl bg-gradient-to-br from-brand-500/30 to-fuchsia-500/30 blur-xl" />
          <div className="relative animate-float">
            {spotlight ? (
              <Link to={`/products/${spotlight.id}`} className="group block overflow-hidden rounded-3xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur">
                <ProductArt category={spotlight.category} imageUrl={spotlight.imageUrl} alt={spotlight.name} className="aspect-square rounded-2xl" iconSize={120} />
                <div className="flex items-center justify-between px-3 pb-2 pt-4">
                  <div>
                    <p className="text-xs uppercase tracking-wider text-slate-300">Featured</p>
                    <p className="text-lg font-bold">{spotlight.name}</p>
                  </div>
                  <span className="rounded-full bg-white px-4 py-1.5 text-sm font-bold text-slate-900">{formatMoney(spotlight.price)}</span>
                </div>
              </Link>
            ) : <div className="aspect-square rounded-3xl bg-white/10" />}
          </div>
        </div>
      </div>
    </section>
  );
}

export default function Home() {
  const { data: products, isLoading, error, refetch } = useGetProductsQuery({});
  const sorted = [...(products ?? [])].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const inStock = sorted.filter((p) => p.stock > 0);
  const spotlight = [...inStock].sort((a, b) => b.price - a.price)[0];
  const categories = useCategories();
  const authed = useSelector(selectIsAuthed);

  return (
    <>
      <Hero spotlight={spotlight} />

      {categories.length > 0 && (
        <section className="container-page py-16">
          <div className="mb-8 flex items-end justify-between">
            <div>
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">Shop by category</h2>
              <p className="mt-1 text-slate-500 dark:text-slate-400">Find exactly what you need.</p>
            </div>
            <Link to="/shop" className="hidden items-center gap-1 text-sm font-semibold text-brand-600 hover:underline sm:flex">All products <ArrowRight size={15} /></Link>
          </div>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            {categories.slice(0, 4).map(({ name, count }) => {
              const meta = categoryMeta(name);
              const Icon = ICONS[meta.icon] ?? Package;
              return (
                <Link
                  key={name}
                  to={`/shop?category=${encodeURIComponent(name)}`}
                  className={`group relative flex aspect-[5/4] flex-col justify-end overflow-hidden rounded-2xl bg-gradient-to-br ${meta.gradient} p-5 text-white shadow-soft transition duration-300 hover:-translate-y-1 hover:shadow-lift`}
                >
                  <Icon size={90} strokeWidth={1.1} className="absolute -right-3 -top-3 text-white/25 transition duration-500 group-hover:scale-110 group-hover:text-white/35" />
                  <span className="relative text-xl font-bold">{name}</span>
                  <span className="relative flex items-center gap-1 text-sm text-white/85">{count} {count === 1 ? 'product' : 'products'} <ArrowRight size={14} className="transition group-hover:translate-x-1" /></span>
                </Link>
              );
            })}
          </div>
        </section>
      )}

      <section className="container-page pb-4">
        <div className="mb-8 flex items-end justify-between">
          <div>
            <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">New arrivals</h2>
            <p className="mt-1 text-slate-500 dark:text-slate-400">The latest additions to the store.</p>
          </div>
          <Link to="/shop" className="btn-outline btn-sm">View all</Link>
        </div>
        {error ? <ErrorState message={errorMessage(error)} onRetry={refetch} /> : !isLoading && inStock.length === 0 ? (
          <div className="card"><NoData icon={Package} title="No products yet" text="The store has no products to show right now. Please check back soon." /></div>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {isLoading
              ? Array.from({ length: 4 }, (_, i) => <ProductCardSkeleton key={i} />)
              : inStock.slice(0, 4).map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>

      {!authed && (
      <section className="container-page mt-16">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-brand-600 via-violet-600 to-fuchsia-600 px-8 py-12 text-white sm:px-14 sm:py-16">
          <Package size={220} strokeWidth={0.8} className="absolute -right-10 -top-6 text-white/10" />
          <div className="relative max-w-xl">
            <h2 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Create an account, check out in seconds.</h2>
            <p className="mt-3 text-brand-100">Save favourites, keep your cart and follow every order live.</p>
            <Link to="/register" className="btn mt-7 bg-white px-6 py-3 text-brand-700 hover:bg-brand-50">Get started free <ArrowRight size={17} /></Link>
          </div>
        </div>
      </section>
      )}
    </>
  );
}
