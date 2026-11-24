import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { PackageSearch, Search, SlidersHorizontal, X } from 'lucide-react';
import { useGetProductsQuery } from '../services/api';
import ProductCard from '../components/ProductCard';
import { EmptyState, ErrorState, PageHeader, ProductCardSkeleton } from '../components/ui';
import { errorMessage, formatMoney } from '../utils/format';

const SORTS = {
  newest: ['Newest', (a, b) => new Date(b.createdAt) - new Date(a.createdAt)],
  'price-asc': ['Price: low to high', (a, b) => a.price - b.price],
  'price-desc': ['Price: high to low', (a, b) => b.price - a.price],
  name: ['Name: A–Z', (a, b) => a.name.localeCompare(b.name)],
};

export default function Shop() {
  const [params, setParams] = useSearchParams();
  const category = params.get('category') ?? '';
  const name = params.get('name') ?? '';
  const sort = params.get('sort') ?? 'newest';
  const [maxPrice, setMaxPrice] = useState('');
  const [inStockOnly, setInStockOnly] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);

  // category + name are filtered by the backend; the rest is client-side on the returned list
  const { data: filtered, isLoading, isFetching, error, refetch } = useGetProductsQuery({ category, name });
  const { data: all } = useGetProductsQuery({});
  const categories = useMemo(() => [...new Set((all ?? []).map((p) => p.category).filter(Boolean))].sort(), [all]);

  const products = useMemo(() => {
    let list = filtered ?? [];
    if (inStockOnly) list = list.filter((p) => p.stock > 0);
    if (maxPrice) list = list.filter((p) => p.price <= Number(maxPrice));
    return [...list].sort(SORTS[sort]?.[1] ?? SORTS.newest[1]);
  }, [filtered, inStockOnly, maxPrice, sort]);

  const set = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value); else next.delete(key);
    setParams(next, { replace: true });
  };
  const clearAll = () => { setParams({}, { replace: true }); setMaxPrice(''); setInStockOnly(false); };
  const hasFilters = category || name || maxPrice || inStockOnly;
  const maxCatalogPrice = Math.ceil(Math.max(0, ...(all ?? []).map((p) => p.price)) / 100) * 100;

  const chip = (active) => `rounded-full border px-3.5 py-1.5 text-sm font-medium transition ${active
    ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 bg-white text-slate-600 hover:border-brand-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300'}`;

  const Filters = (
    <div className="space-y-7">
      <div>
        <h3 className="mb-3 text-sm font-bold">Category</h3>
        <div className="flex flex-wrap gap-2">
          <button className={chip(!category)} onClick={() => set('category', '')}>All</button>
          {categories.map((c) => <button key={c} className={chip(category === c)} onClick={() => set('category', c)}>{c}</button>)}
        </div>
      </div>
      <div>
        <div className="mb-3 flex items-center justify-between"><h3 className="text-sm font-bold">Max price</h3><span className="text-sm font-semibold text-brand-600">{maxPrice ? formatMoney(maxPrice) : 'Any'}</span></div>
        <input type="range" min="0" max={maxCatalogPrice || 2000} step="50" value={maxPrice || maxCatalogPrice || 2000}
          onChange={(e) => setMaxPrice(Number(e.target.value) >= (maxCatalogPrice || 2000) ? '' : e.target.value)}
          className="w-full accent-brand-600" aria-label="Maximum price" />
      </div>
      <label className="flex cursor-pointer items-center gap-3 text-sm font-medium">
        <input type="checkbox" checked={inStockOnly} onChange={(e) => setInStockOnly(e.target.checked)} className="h-4 w-4 rounded accent-brand-600" /> In stock only
      </label>
      {hasFilters && <button onClick={clearAll} className="btn-outline w-full"><X size={15} /> Clear all filters</button>}
    </div>
  );

  return (
    <div className="container-page py-10">
      <PageHeader title={category || 'All products'} subtitle={name ? `Results for “${name}”` : 'Everything in store, ready to ship.'} />

      <div className="mb-6 flex flex-wrap items-center gap-3">
        <div className="relative min-w-[14rem] flex-1">
          <Search size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input defaultValue={name} key={name} onKeyDown={(e) => e.key === 'Enter' && set('name', e.currentTarget.value.trim())}
            onBlur={(e) => e.currentTarget.value.trim() !== name && set('name', e.currentTarget.value.trim())}
            placeholder="Search by product name…" aria-label="Search by product name" className="input pl-10" />
        </div>
        <select value={sort} onChange={(e) => set('sort', e.target.value === 'newest' ? '' : e.target.value)} className="input w-auto" aria-label="Sort products">
          {Object.entries(SORTS).map(([k, [label]]) => <option key={k} value={k}>{label}</option>)}
        </select>
        <button className="btn-outline lg:hidden" onClick={() => setFiltersOpen((o) => !o)} aria-expanded={filtersOpen}><SlidersHorizontal size={16} /> Filters</button>
      </div>
      {filtersOpen && <div className="card mb-6 animate-fade-up p-5 lg:hidden">{Filters}</div>}

      <div className="grid gap-8 lg:grid-cols-[16rem_1fr]">
        <aside className="hidden lg:block"><div className="card sticky top-24 p-5">{Filters}</div></aside>
        <section aria-busy={isFetching}>
          {error ? <ErrorState message={errorMessage(error)} onRetry={refetch} /> : (
            <>
              {!isLoading && <p className="mb-4 text-sm text-slate-500">{products.length} {products.length === 1 ? 'product' : 'products'}</p>}
              {isLoading ? (
                <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">{Array.from({ length: 6 }, (_, i) => <ProductCardSkeleton key={i} />)}</div>
              ) : (all ?? []).length === 0 ? (
                <EmptyState icon={PackageSearch} title="No products available" text="The store has no products yet. Please check back soon." />
              ) : products.length === 0 ? (
                <EmptyState icon={PackageSearch} title="No products match" text="Try removing a filter or searching for something else." action={{ label: 'Clear filters', onClick: clearAll }} />
              ) : (
                <div className={`grid grid-cols-1 gap-6 transition-opacity sm:grid-cols-2 xl:grid-cols-3 ${isFetching ? 'opacity-60' : ''}`}>
                  {products.map((p) => <ProductCard key={p.id} product={p} />)}
                </div>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
