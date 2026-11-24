import { Link } from 'react-router-dom';
import { BarChart3, Boxes, CircleDollarSign, PackageCheck, ReceiptText, TriangleAlert, Trophy, Users } from 'lucide-react';
import { useGetAllOrdersQuery, useGetProductsQuery, useGetUsersQuery } from '../../services/api';
import { ErrorState, NoData, PageHeader, PageLoader, StatusBadge } from '../../components/ui';
import { errorMessage, formatDate, formatMoney } from '../../utils/format';

const BAR = { PAID: 'bg-emerald-500', PENDING: 'bg-amber-500', SHIPPED: 'bg-sky-500', DELIVERED: 'bg-teal-500', PAYMENT_FAILED: 'bg-rose-500', CANCELLED: 'bg-slate-400' };
// an order counts as revenue only once it is paid (not pending, failed or cancelled)
const COUNTS_AS_SALE = ['PAID', 'SHIPPED', 'DELIVERED'];

function Stat({ icon: Icon, label, value, tint }) {
  return (
    <div className="card flex items-center gap-4 p-5">
      <span className={`grid h-12 w-12 place-items-center rounded-xl ${tint}`}><Icon size={22} /></span>
      <div className="min-w-0"><p className="text-sm text-slate-500">{label}</p><p className="truncate text-2xl font-extrabold tabular-nums">{value}</p></div>
    </div>
  );
}

const Card = ({ title, icon: Icon, children, className = '' }) => (
  <section className={`card p-6 ${className}`}>
    <h2 className="mb-5 flex items-center gap-2 font-bold">{Icon && <Icon size={17} className="text-brand-600" />}{title}</h2>
    {children}
  </section>
);

/** Revenue for each of the last 7 calendar days (local time), oldest first. */
function lastSevenDays(sales) {
  const key = (d) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (6 - i));
    return { key: key(d), label: d.toLocaleDateString('en-IN', { weekday: 'short' }), full: d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }), total: 0, count: 0 };
  });
  sales.forEach((o) => {
    const day = days.find((x) => x.key === key(new Date(o.createdAt)));
    if (day) { day.total += Number(o.totalAmount); day.count += 1; }
  });
  return days;
}

export default function Dashboard() {
  const orders = useGetAllOrdersQuery();
  const products = useGetProductsQuery({});
  const users = useGetUsersQuery();
  if (orders.isLoading || products.isLoading || users.isLoading) return <PageLoader />;
  const failed = orders.error || products.error || users.error;
  if (failed) return <ErrorState message={errorMessage(failed)} onRetry={() => { orders.refetch(); products.refetch(); users.refetch(); }} />;

  const allOrders = orders.data ?? [];
  const allProducts = products.data ?? [];
  const customers = (users.data ?? []).filter((u) => u.role === 'USER');

  const sales = allOrders.filter((o) => COUNTS_AS_SALE.includes(o.status));
  const revenue = sales.reduce((s, o) => s + Number(o.totalAmount), 0);
  const delivered = allOrders.filter((o) => o.status === 'DELIVERED').length;

  const byStatus = allOrders.reduce((m, o) => ({ ...m, [o.status]: (m[o.status] ?? 0) + 1 }), {});
  const days = lastSevenDays(sales);
  const maxDay = Math.max(...days.map((d) => d.total), 1);

  const sold = {};
  sales.forEach((o) => o.items.forEach((i) => {
    const row = (sold[i.productId] ??= { name: i.productName, qty: 0, revenue: 0 });
    row.qty += i.quantity;
    row.revenue += Number(i.unitPrice) * i.quantity;
  }));
  const top = Object.values(sold).sort((a, b) => b.qty - a.qty).slice(0, 5);

  const lowStock = allProducts.filter((p) => p.stock <= 5).sort((a, b) => a.stock - b.stock);
  const recent = [...allOrders].sort((a, b) => b.id - a.id).slice(0, 6);

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Live numbers from your store. Nothing here is sample data." />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <Stat icon={CircleDollarSign} label="Revenue (paid orders)" value={formatMoney(revenue)} tint="bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300" />
        <Stat icon={ReceiptText} label="Orders" value={allOrders.length} tint="bg-brand-100 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300" />
        <Stat icon={PackageCheck} label="Delivered" value={delivered} tint="bg-teal-100 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300" />
        <Stat icon={Boxes} label="Products" value={allProducts.length} tint="bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300" />
        <Stat icon={Users} label="Customers" value={customers.length} tint="bg-fuchsia-100 text-fuchsia-700 dark:bg-fuchsia-500/15 dark:text-fuchsia-300" />
        <Stat icon={TriangleAlert} label="Low or out of stock" value={lowStock.length} tint="bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <Card title="Sales, last 7 days" icon={BarChart3}>
          {sales.length === 0 ? <NoData compact icon={BarChart3} title="No sales yet" text="Paid orders will appear here." /> : (
            <div className="flex h-44 items-end gap-2" role="img" aria-label="Revenue per day for the last seven days">
              {days.map((d) => (
                <div key={d.key} className="flex h-full flex-1 flex-col items-center justify-end gap-1.5" title={`${d.full}: ${formatMoney(d.total)} from ${d.count} ${d.count === 1 ? 'order' : 'orders'}`}>
                  <span className="text-[11px] font-semibold tabular-nums text-slate-500">{d.total ? formatMoney(d.total) : ''}</span>
                  <div className="w-full rounded-t-lg bg-gradient-to-t from-brand-600 to-fuchsia-500" style={{ height: `${d.total ? Math.max((d.total / maxDay) * 100, 4) : 2}%`, opacity: d.total ? 1 : 0.25 }} />
                  <span className="text-xs text-slate-500">{d.label}</span>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card title="Top selling products" icon={Trophy}>
          {top.length === 0 ? <NoData compact icon={Trophy} title="No data yet" text="Best sellers appear after the first paid order." /> : (
            <ol className="space-y-3">
              {top.map((t, n) => (
                <li key={t.name} className="flex items-center gap-3 text-sm">
                  <span className="grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-brand-50 text-xs font-bold text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">{n + 1}</span>
                  <span className="min-w-0 flex-1 truncate font-medium">{t.name}</span>
                  <span className="text-slate-500">{t.qty} sold</span>
                  <span className="w-24 text-right font-semibold tabular-nums">{formatMoney(t.revenue)}</span>
                </li>
              ))}
            </ol>
          )}
        </Card>

        <Card title="Orders by status" icon={ReceiptText}>
          {allOrders.length === 0 ? <NoData compact icon={ReceiptText} title="No orders yet" /> : (
            <ul className="space-y-4">
              {Object.entries(byStatus).map(([s, n]) => (
                <li key={s}>
                  <div className="mb-1.5 flex justify-between text-sm"><StatusBadge status={s} /><span className="font-semibold tabular-nums">{n}</span></div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800"><div className={`h-full rounded-full ${BAR[s] ?? 'bg-slate-400'}`} style={{ width: `${(n / allOrders.length) * 100}%` }} /></div>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Low stock" icon={TriangleAlert}>
          {allProducts.length === 0 ? <NoData compact icon={Boxes} title="No products yet" /> : lowStock.length === 0 ? (
            <NoData compact title="All products are well stocked" />
          ) : (
            <ul className="divide-y divide-slate-100 dark:divide-slate-800">
              {lowStock.slice(0, 6).map((p) => (
                <li key={p.id} className="flex items-center justify-between py-2.5 text-sm"><span className="font-medium">{p.name}</span>
                  <span className={`badge ${p.stock === 0 ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-800'}`}>{p.stock === 0 ? 'Out of stock' : `${p.stock} left`}</span></li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <section className="card mt-6 overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4"><h2 className="font-bold">Recent orders</h2>{recent.length > 0 && <Link to="/admin/orders" className="text-sm font-semibold text-brand-600 hover:underline">View all</Link>}</div>
        {recent.length === 0 ? <NoData icon={ReceiptText} title="No orders yet" text="Orders placed by customers will show up here." /> : (
          <div className="overflow-x-auto"><table className="w-full">
            <thead className="border-y border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">Order</th><th className="th">Customer</th><th className="th">Date</th><th className="th">Status</th><th className="th text-right">Total</th></tr></thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {recent.map((o) => (
                <tr key={o.id}><td className="td"><Link to={`/admin/orders/${o.id}`} className="font-semibold text-brand-600 hover:underline">#{o.id}</Link></td><td className="td">{o.customerName}</td><td className="td text-slate-500">{formatDate(o.createdAt)}</td><td className="td"><StatusBadge status={o.status} /></td><td className="td text-right font-semibold tabular-nums">{formatMoney(o.totalAmount)}</td></tr>
              ))}
            </tbody>
          </table></div>
        )}
      </section>
    </>
  );
}
