import { useEffect, useState } from 'react';
import { Boxes, CheckCircle2, ImageOff, Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { useCreateProductMutation, useDeleteProductMutation, useGetProductsQuery, useUpdateProductMutation } from '../../services/api';
import ProductArt from '../../components/ProductArt';
import { ConfirmModal, ErrorState, Modal, NoData, PageHeader, PageLoader, Spinner } from '../../components/ui';
import { errorMessage, formatMoney } from '../../utils/format';

const EMPTY = { name: '', description: '', price: '', stock: '', category: '', imageUrl: '' };

/** Paste a link, see the picture right away. We store only the link, never the image file. */
function ImageUrlField({ value, onChange }) {
  const url = value.trim();
  const valid = /^https?:\/\/\S+$/i.test(url);
  const [status, setStatus] = useState('idle'); // idle | loading | ok | error
  useEffect(() => { setStatus(url && valid ? 'loading' : 'idle'); }, [url, valid]);

  const showImg = url && valid && status !== 'error';
  let hint = <span className="text-slate-500">Optional. On any website, right-click a picture and choose “Copy image address”, then paste it here.</span>;
  if (url && !valid) hint = <span className="text-rose-600">The link must start with http:// or https://</span>;
  else if (status === 'loading') hint = <span className="text-slate-500">Loading preview…</span>;
  else if (status === 'ok') hint = <span className="flex items-center gap-1 text-emerald-600"><CheckCircle2 size={14} /> Picture found. This is how it will look in the shop.</span>;
  else if (status === 'error') hint = <span className="text-amber-600">This link can't be shown as a picture. Use the direct image address (usually ends in .jpg, .png or .webp), not the address of a web page.</span>;

  return (
    <div>
      <label className="label" htmlFor="pi">Image URL</label>
      <div className="flex items-start gap-4">
        <div className="grid h-24 w-24 shrink-0 place-items-center overflow-hidden rounded-xl border border-slate-200 bg-white text-slate-300 dark:border-slate-700">
          {showImg
            ? <img key={url} src={url} alt="Preview" referrerPolicy="no-referrer" onLoad={() => setStatus('ok')} onError={() => setStatus('error')} className="h-full w-full object-contain p-1" />
            : <ImageOff size={28} />}
        </div>
        <div className="min-w-0 flex-1">
          <input id="pi" type="url" inputMode="url" pattern="https?://.+" maxLength={2000} className="input" placeholder="https://example.com/photo.jpg" value={value} onChange={onChange} />
          <p className="mt-1.5 text-xs">{hint}</p>
        </div>
      </div>
    </div>
  );
}

function ProductForm({ initial, onClose, categories }) {
  const [form, setForm] = useState(initial ? { ...initial, price: String(initial.price), stock: String(initial.stock), description: initial.description ?? '', category: initial.category ?? '', imageUrl: initial.imageUrl ?? '' } : EMPTY);
  const [error, setError] = useState('');
  const [create, { isLoading: creating }] = useCreateProductMutation();
  const [update, { isLoading: updating }] = useUpdateProductMutation();
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    const body = { name: form.name.trim(), description: form.description.trim(), category: form.category.trim(), imageUrl: form.imageUrl.trim() || null, price: Number(form.price), stock: Number(form.stock) };
    try {
      if (initial) await update({ id: initial.id, ...body }).unwrap(); else await create(body).unwrap();
      onClose();
    } catch (err) { setError(errorMessage(err)); }
  };

  return (
    <form onSubmit={submit} className="space-y-4">
      {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{error}</div>}
      <div><label className="label" htmlFor="pn">Name</label><input id="pn" className="input" value={form.name} onChange={set('name')} required /></div>
      <div><label className="label" htmlFor="pd">Description</label><textarea id="pd" rows={3} className="input resize-none" value={form.description} onChange={set('description')} /></div>
      <div className="grid gap-4 sm:grid-cols-3">
        <div><label className="label" htmlFor="pr">Price (₹)</label><input id="pr" type="number" step="0.01" min="0.01" className="input" value={form.price} onChange={set('price')} required /></div>
        <div><label className="label" htmlFor="ps">Stock</label><input id="ps" type="number" min="0" step="1" className="input" value={form.stock} onChange={set('stock')} required /></div>
        <div><label className="label" htmlFor="pc">Category</label><input id="pc" className="input" value={form.category} onChange={set('category')} list="cats" /><datalist id="cats">{categories.map((c) => <option key={c} value={c} />)}</datalist></div>
      </div>
      <ImageUrlField value={form.imageUrl} onChange={set('imageUrl')} />
      <div className="flex justify-end gap-3 pt-2">
        <button type="button" onClick={onClose} className="btn-outline">Cancel</button>
        <button className="btn-primary" disabled={creating || updating}>{(creating || updating) && <Spinner size={16} />}{initial ? 'Save changes' : 'Create product'}</button>
      </div>
    </form>
  );
}

export default function AdminProducts() {
  const { data, isLoading, error, refetch } = useGetProductsQuery({});
  const [del, { isLoading: deleting }] = useDeleteProductMutation();
  const [editing, setEditing] = useState(null); // null | 'new' | product
  const [toDelete, setToDelete] = useState(null);
  const [actionError, setActionError] = useState('');
  const [q, setQ] = useState('');

  if (isLoading) return <PageLoader />;
  if (error) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;
  const rows = (data ?? []).filter((p) => `${p.name} ${p.category}`.toLowerCase().includes(q.toLowerCase())).sort((a, b) => b.id - a.id);

  const confirmDelete = async () => {
    setActionError('');
    try { await del(toDelete.id).unwrap(); }
    catch (e) { setActionError(errorMessage(e)); }
    setToDelete(null);
  };

  return (
    <>
      <PageHeader title="Products" subtitle={`${data.length} in catalogue`}>
        <button className="btn-primary" onClick={() => setEditing('new')}><Plus size={17} /> New product</button>
      </PageHeader>
      {actionError && <div role="alert" className="mb-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{actionError}</div>}
      {data.length === 0 ? (
        <div className="card"><NoData icon={Boxes} title="No products yet" text="Your catalogue is empty. Add your first product so customers can see it in the shop." action={{ label: 'Add product', onClick: () => setEditing('new') }} /></div>
      ) : (<>
      <div className="relative mb-4 max-w-sm"><Search size={16} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" /><input className="input pl-10" placeholder="Filter by name or category" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Filter products" /></div>

      <div className="card overflow-x-auto">
        <table className="w-full">
          <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">ID</th><th className="th">Product</th><th className="th">Category</th><th className="th text-right">Price</th><th className="th text-right">Stock</th><th className="th" /></tr></thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {rows.map((p) => (
              <tr key={p.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/30">
                <td className="td text-slate-500">#{p.id}</td>
                <td className="td"><div className="flex items-center gap-3">
                  <ProductArt category={p.category} imageUrl={p.imageUrl} alt="" className="h-11 w-11 shrink-0 rounded-lg border border-slate-200 dark:border-slate-700" iconSize={18} />
                  <div className="min-w-0"><p className="font-semibold">{p.name}</p><p className="line-clamp-1 max-w-xs text-xs text-slate-500">{p.description}</p></div>
                </div></td>
                <td className="td">{p.category && <span className="badge bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">{p.category}</span>}</td>
                <td className="td text-right font-semibold tabular-nums">{formatMoney(p.price)}</td>
                <td className="td text-right"><span className={`badge ${p.stock === 0 ? 'bg-rose-100 text-rose-700' : p.stock <= 5 ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-700'}`}>{p.stock}</span></td>
                <td className="td"><div className="flex justify-end gap-1">
                  <button className="btn-ghost btn-sm" onClick={() => setEditing(p)} aria-label={`Edit ${p.name}`}><Pencil size={15} /></button>
                  <button className="btn-ghost btn-sm text-rose-600" onClick={() => setToDelete(p)} aria-label={`Delete ${p.name}`}><Trash2 size={15} /></button>
                </div></td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={6} className="td py-10 text-center text-slate-500">No products found.</td></tr>}
          </tbody>
        </table>
      </div>

      </>)}

      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing === 'new' ? 'New product' : 'Edit product'} wide>
        {editing && <ProductForm initial={editing === 'new' ? null : editing} onClose={() => setEditing(null)} categories={[...new Set((data ?? []).map((p) => p.category).filter(Boolean))].sort()} />}
      </Modal>
      <ConfirmModal open={Boolean(toDelete)} onClose={() => setToDelete(null)} onConfirm={confirmDelete} busy={deleting} title="Delete product?" text={`“${toDelete?.name}” will be removed from the catalogue. This cannot be undone.`} />
    </>
  );
}
