import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ReceiptText, Truck } from 'lucide-react';
import { useGetAllOrdersQuery, useGetShipmentForOrderQuery, useUpdateShipmentStatusMutation } from '../../services/api';
import { ErrorState, Modal, NoData, PageHeader, PageLoader, Spinner, StatusBadge } from '../../components/ui';
import { errorMessage, formatDate, formatMoney } from '../../utils/format';

const NEXT = { CREATED: ['SHIPPED'], SHIPPED: ['IN_TRANSIT', 'DELIVERED'], IN_TRANSIT: ['DELIVERED'], DELIVERED: [] };

function ShipmentPanel({ order }) {
  const { data: shipment, isLoading, error } = useGetShipmentForOrderQuery(order.id);
  const [update, { isLoading: saving }] = useUpdateShipmentStatusMutation();
  const [moveError, setMoveError] = useState('');

  if (isLoading) return <div className="grid place-items-center py-8"><Spinner size={26} className="text-brand-500" /></div>;
  if (error) return <p className="text-sm text-slate-500">{error.status === 404 ? 'No shipment exists for this order (payment not completed).' : errorMessage(error)}</p>;

  const move = async (status) => {
    setMoveError('');
    try { await update({ id: shipment.id, status }).unwrap(); }
    catch (e) { setMoveError(errorMessage(e)); }
  };

  return (
    <div className="space-y-4">
      {moveError && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{moveError}</div>}
      <dl className="grid grid-cols-2 gap-3 text-sm">
        <div><dt className="text-slate-500">Tracking</dt><dd className="font-mono font-bold">{shipment.trackingNumber}</dd></div>
        <div><dt className="text-slate-500">Status</dt><dd><StatusBadge status={shipment.status} /></dd></div>
        <div className="col-span-2"><dt className="text-slate-500">Address</dt><dd>{shipment.address}</dd></div>
      </dl>
      <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-4 dark:border-slate-800">
        {NEXT[shipment.status].length === 0 ? <p className="text-sm text-emerald-600">Delivered. Nothing more to do.</p>
          : NEXT[shipment.status].map((s) => <button key={s} className="btn-primary btn-sm" disabled={saving} onClick={() => move(s)}>Mark as {s.replace('_', ' ').toLowerCase()}</button>)}
      </div>
    </div>
  );
}

export default function AdminOrders() {
  const { data, isLoading, error, refetch } = useGetAllOrdersQuery();
  const [selected, setSelected] = useState(null);
  const [filter, setFilter] = useState('ALL');

  if (isLoading) return <PageLoader />;
  if (error) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;
  const statuses = ['ALL', ...new Set((data ?? []).map((o) => o.status))];
  const rows = [...data].filter((o) => filter === 'ALL' || o.status === filter).sort((a, b) => b.id - a.id);

  return (
    <>
      <PageHeader title="Orders" subtitle={`${data.length} orders in total`} />
      {data.length === 0 ? (
        <div className="card"><NoData icon={ReceiptText} title="No orders yet" text="When customers place orders they will appear here." /></div>
      ) : (<>
      <div className="mb-4 flex flex-wrap gap-2">
        {statuses.map((s) => (
          <button key={s} onClick={() => setFilter(s)} className={`rounded-full border px-3.5 py-1.5 text-xs font-semibold transition ${filter === s ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 hover:border-brand-400 dark:border-slate-700'}`}>{s.replace('_', ' ')}</button>
        ))}
      </div>
      <div className="card overflow-x-auto">
        <table className="w-full">
          <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">Order</th><th className="th">Customer</th><th className="th">Date</th><th className="th">Status</th><th className="th text-right">Total</th><th className="th" /></tr></thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {rows.map((o) => (
              <tr key={o.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/30">
                <td className="td"><Link to={`/admin/orders/${o.id}`} className="font-semibold text-brand-600 hover:underline">#{o.id}</Link></td>
                <td className="td">{o.customerName}</td><td className="td text-slate-500">{formatDate(o.createdAt, true)}</td>
                <td className="td"><StatusBadge status={o.status} /></td><td className="td text-right font-semibold tabular-nums">{formatMoney(o.totalAmount)}</td>
                <td className="td text-right"><button className="btn-outline btn-sm" onClick={() => setSelected(o)}><Truck size={14} /> Shipment</button></td>
              </tr>
            ))}
            {rows.length === 0 && <tr><td colSpan={6} className="td py-10 text-center text-slate-500">No orders.</td></tr>}
          </tbody>
        </table>
      </div>
      </>)}
      <Modal open={Boolean(selected)} onClose={() => setSelected(null)} title={`Order #${selected?.id} shipment`}>{selected && <ShipmentPanel order={selected} />}</Modal>
    </>
  );
}
