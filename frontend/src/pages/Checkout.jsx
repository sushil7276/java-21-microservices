import { useState } from 'react';
import { useSelector } from 'react-redux';
import { Navigate, useNavigate } from 'react-router-dom';
import { Banknote, CreditCard, Landmark, Lock, MapPin, TriangleAlert } from 'lucide-react';
import { useCheckoutMutation, useGetCartQuery } from '../services/api';
import { selectUser } from '../features/auth/authSlice';
import { OrderSummary } from './Cart';
import { PageHeader, PageLoader, Spinner } from '../components/ui';
import { errorMessage } from '../utils/format';

const METHODS = [
  { id: 'CARD', label: 'Credit / debit card', icon: CreditCard },
  { id: 'NET_BANKING', label: 'Net banking', icon: Landmark },
  { id: 'COD', label: 'Cash on delivery', icon: Banknote },
  { id: 'CARD_DECLINED', label: 'Card that gets declined', icon: TriangleAlert, demo: true },
];

export default function Checkout() {
  const user = useSelector(selectUser);
  const navigate = useNavigate();
  const { data: cart, isLoading } = useGetCartQuery();
  const [checkout, { isLoading: placing }] = useCheckoutMutation();
  const [address, setAddress] = useState('');
  const [method, setMethod] = useState('CARD');
  const [error, setError] = useState('');

  if (isLoading) return <PageLoader />;
  if (!cart?.items?.length && !placing) return <Navigate to="/cart" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const order = await checkout({ shippingAddress: address.trim(), paymentMethod: method }).unwrap();
      navigate(`/orders/${order.id}`, { replace: true, state: { placed: true } });
    } catch (err) {
      // 402: the order exists but the payment was declined; the response body is that order
      if (err?.status === 402 && err.data?.id) {
        navigate(`/orders/${err.data.id}`, { replace: true, state: { declined: true } });
        return;
      }
      setError(errorMessage(err));
    }
  };

  return (
    <div className="container-page py-10">
      <PageHeader title="Checkout" subtitle={`Ordering as ${user?.username}`} />
      <form onSubmit={submit} className="grid gap-8 lg:grid-cols-[1fr_22rem]">
        <div className="space-y-6">
          {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{error}</div>}

          <section className="card p-6">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold"><MapPin size={19} className="text-brand-600" /> Delivery address</h2>
            <label htmlFor="addr" className="label">Full address</label>
            <textarea id="addr" rows={3} required className="input resize-none" placeholder="House no., street, city, postal code" value={address} onChange={(e) => setAddress(e.target.value)} />
          </section>

          <section className="card p-6">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-bold"><CreditCard size={19} className="text-brand-600" /> Payment method</h2>
            <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Payment method">
              {METHODS.map(({ id, label, icon: Icon, demo }) => (
                <label key={id} className={`flex cursor-pointer items-center gap-3 rounded-xl border-2 p-4 transition ${method === id ? 'border-brand-600 bg-brand-50 dark:bg-brand-900/30' : 'border-slate-200 hover:border-slate-300 dark:border-slate-700'}`}>
                  <input type="radio" name="method" value={id} checked={method === id} onChange={() => setMethod(id)} className="sr-only" />
                  <Icon size={20} className={method === id ? 'text-brand-600' : 'text-slate-400'} />
                  <span className="text-sm font-semibold">{label}</span>
                  {demo && <span className="badge ml-auto bg-amber-100 text-amber-800">Demo</span>}
                </label>
              ))}
            </div>
            <p className="mt-4 flex items-center gap-2 text-xs text-slate-500"><Lock size={13} /> Payments are simulated by the backend. No real charge is made.</p>
          </section>
        </div>

        <OrderSummary items={cart.items} total={cart.total}>
          <button className="btn-primary btn-lg mt-6 w-full" disabled={placing || !address.trim()}>
            {placing ? <><Spinner size={18} /> Placing order…</> : <><Lock size={17} /> Place order</>}
          </button>
        </OrderSummary>
      </form>
    </div>
  );
}
