import { Link, useLocation, useParams } from 'react-router-dom';
import { Check, ChevronRight, CircleAlert, CreditCard, MapPin, PackageCheck, PartyPopper, ReceiptText, Truck } from 'lucide-react';
import { useGetOrderQuery, useGetOrdersQuery, useGetPaymentsForOrderQuery, useGetShipmentForOrderQuery } from '../services/api';
import { EmptyState, ErrorState, PageHeader, PageLoader, StatusBadge } from '../components/ui';
import { errorMessage, formatDate, formatMoney } from '../utils/format';

export function Orders() {
  const { data, isLoading, error, refetch } = useGetOrdersQuery();
  if (isLoading) return <PageLoader />;
  if (error) return <div className="container-page py-16"><ErrorState message={errorMessage(error)} onRetry={refetch} /></div>;
  const orders = [...(data ?? [])].sort((a, b) => b.id - a.id);

  return (
    <div className="container-page max-w-4xl py-10">
      <PageHeader title="My orders" subtitle="Track and review everything you've bought." />
      {orders.length === 0 ? (
        <EmptyState icon={ReceiptText} title="No orders yet" text="When you place an order it will show up here." action={{ label: 'Start shopping', to: '/shop' }} />
      ) : (
        <ul className="space-y-4">
          {orders.map((o) => (
            <li key={o.id}>
              <Link to={`/orders/${o.id}`} className="card flex flex-wrap items-center justify-between gap-4 p-5 transition hover:-translate-y-0.5 hover:shadow-lift">
                <div>
                  <div className="flex items-center gap-3"><span className="font-bold">Order #{o.id}</span><StatusBadge status={o.status} /></div>
                  <p className="mt-1 text-sm text-slate-500">{formatDate(o.createdAt, true)} · {o.items.reduce((n, i) => n + i.quantity, 0)} items</p>
                  <p className="mt-1 line-clamp-1 max-w-md text-sm text-slate-600 dark:text-slate-400">{o.items.map((i) => i.productName).join(', ')}</p>
                </div>
                <div className="flex items-center gap-3"><span className="text-lg font-extrabold">{formatMoney(o.totalAmount)}</span><ChevronRight size={18} className="text-slate-400" /></div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const STEPS = [
  { key: 'placed', label: 'Order placed', icon: ReceiptText },
  { key: 'paid', label: 'Payment confirmed', icon: CreditCard },
  { key: 'shipped', label: 'Shipped', icon: Truck },
  { key: 'delivered', label: 'Delivered', icon: PackageCheck },
];
const stepIndex = (order, shipment) => {
  if (order.status === 'DELIVERED' || shipment?.status === 'DELIVERED') return 3;
  if (order.status === 'SHIPPED' || ['SHIPPED', 'IN_TRANSIT'].includes(shipment?.status)) return 2;
  if (order.status === 'PAID') return 1;
  return 0;
};

function Timeline({ order, shipment }) {
  const current = stepIndex(order, shipment);
  return (
    <ol className="grid gap-4 sm:grid-cols-4" aria-label="Order progress">
      {STEPS.map((s, i) => {
        const done = i <= current;
        return (
          <li key={s.key} className="relative flex items-center gap-3 sm:flex-col sm:text-center">
            {i > 0 && <span aria-hidden className={`absolute right-1/2 top-5 hidden h-0.5 w-full sm:block ${i <= current ? 'bg-brand-600' : 'bg-slate-200 dark:bg-slate-700'}`} />}
            <span className={`relative z-10 grid h-10 w-10 shrink-0 place-items-center rounded-full ring-4 ring-white dark:ring-slate-900 ${done ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-400 dark:bg-slate-700'}`}>
              {done && i < current ? <Check size={18} /> : <s.icon size={18} />}
            </span>
            <span className={`text-sm font-semibold ${done ? 'text-slate-900 dark:text-white' : 'text-slate-400'}`}>{s.label}</span>
          </li>
        );
      })}
    </ol>
  );
}

export function OrderDetail() {
  const { id } = useParams();
  const { state } = useLocation();
  const inAdmin = useLocation().pathname.startsWith('/admin'); // opened from the admin panel (any customer's order)
  const { data: order, isLoading, error, refetch } = useGetOrderQuery(id);
  const failed = order?.status === 'PAYMENT_FAILED' || order?.status === 'CANCELLED';
  const { data: shipment } = useGetShipmentForOrderQuery(id, { skip: !order || failed });
  const { data: payments } = useGetPaymentsForOrderQuery(id, { skip: !order });

  if (isLoading) return <PageLoader />;
  if (error?.status === 403 || error?.status === 404) return <div className="container-page py-16"><EmptyState title="Order not found" text="It doesn't exist or belongs to another account." action={{ label: 'My orders', to: '/orders' }} /></div>;
  if (error) return <div className="container-page py-16"><ErrorState message={errorMessage(error)} onRetry={refetch} /></div>;

  return (
    <div className="container-page max-w-4xl py-10">
      <Link to={inAdmin ? '/admin/orders' : '/orders'} className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-brand-600">← {inAdmin ? 'Back to orders' : 'All orders'}</Link>

      {state?.placed && (
        <div className="mb-6 flex items-center gap-4 rounded-2xl bg-emerald-50 p-5 text-emerald-900 dark:bg-emerald-500/10 dark:text-emerald-200">
          <PartyPopper size={28} /><div><p className="font-bold">Thank you, your order is confirmed!</p><p className="text-sm opacity-80">We've received your payment and are preparing your parcel.</p></div>
        </div>
      )}
      {(state?.declined || order.status === 'PAYMENT_FAILED') && (
        <div role="alert" className="mb-6 flex items-center gap-4 rounded-2xl bg-rose-50 p-5 text-rose-900 dark:bg-rose-500/10 dark:text-rose-200">
          <CircleAlert size={28} /><div><p className="font-bold">Payment was declined</p><p className="text-sm opacity-80">{order.failureReason || 'The payment could not be processed.'} No money was taken and stock was released.{inAdmin ? '' : ' Your cart is unchanged, so you can try again.'}</p></div>
          {!inAdmin && <Link to="/checkout" className="btn-danger btn-sm ml-auto">Retry</Link>}
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="h-title">Order #{order.id}</h1><StatusBadge status={order.status} />
      </div>
      <p className="mt-1 text-slate-500">Placed {formatDate(order.createdAt, true)}</p>

      {!failed && <section className="card mt-8 p-6"><Timeline order={order} shipment={shipment} /></section>}

      <div className="mt-6 grid gap-6 md:grid-cols-2">
        <section className="card p-6">
          <h2 className="mb-3 flex items-center gap-2 font-bold"><MapPin size={17} className="text-brand-600" /> Delivery</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400">{order.shippingAddress}</p>
          {(shipment?.trackingNumber || order.trackingNumber) && (
            <div className="mt-4 rounded-xl bg-slate-50 p-3 dark:bg-slate-800">
              <p className="text-xs uppercase tracking-wide text-slate-500">Tracking number</p>
              <p className="font-mono text-sm font-bold">{shipment?.trackingNumber || order.trackingNumber}</p>
              {shipment && <p className="mt-1 flex items-center gap-2 text-sm">Status: <StatusBadge status={shipment.status} /></p>}
            </div>
          )}
        </section>
        <section className="card p-6">
          <h2 className="mb-3 flex items-center gap-2 font-bold"><CreditCard size={17} className="text-brand-600" /> Payment</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400">Method: <strong>{order.paymentMethod?.replace(/_/g, ' ')}</strong></p>
          {order.paymentTransactionId && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">Transaction: <span className="font-mono font-semibold">{order.paymentTransactionId}</span></p>}
          {payments?.map((p) => (
            <p key={p.id} className="mt-2 flex items-center gap-2 text-sm"><StatusBadge status={p.status} /> <span className="text-slate-500">{p.message}</span></p>
          ))}
        </section>
      </div>

      <section className="card mt-6 overflow-hidden">
        <h2 className="border-b border-slate-200 px-6 py-4 font-bold dark:border-slate-800">Items</h2>
        <ul className="divide-y divide-slate-100 dark:divide-slate-800">
          {order.items.map((i) => (
            <li key={i.productId} className="flex items-center justify-between gap-4 px-6 py-4 text-sm">
              <Link to={`/products/${i.productId}`} className="font-semibold hover:text-brand-600">{i.productName}</Link>
              <span className="text-slate-500">{i.quantity} × {formatMoney(i.unitPrice)}</span>
              <span className="font-bold tabular-nums">{formatMoney(i.unitPrice * i.quantity)}</span>
            </li>
          ))}
        </ul>
        <div className="flex justify-between border-t border-slate-200 bg-slate-50 px-6 py-4 text-lg font-extrabold dark:border-slate-800 dark:bg-slate-800/50"><span>Total</span><span>{formatMoney(order.totalAmount)}</span></div>
      </section>
    </div>
  );
}
