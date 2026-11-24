import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ShoppingBag } from 'lucide-react';
import { useLoginMutation, useRegisterMutation } from '../services/api';
import { setCredentials } from '../features/auth/authSlice';
import PasswordInput from '../components/PasswordInput';
import Logo from '../components/Logo';
import { Spinner } from '../components/ui';
import { errorMessage } from '../utils/format';

function Shell({ title, subtitle, children, footer }) {
  return (
    <div className="grid min-h-[calc(100vh-7rem)] lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-slate-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -left-24 -top-24 h-80 w-80 rounded-full bg-brand-600/40 blur-3xl" />
        <div className="absolute -bottom-24 -right-10 h-96 w-96 rounded-full bg-fuchsia-600/30 blur-3xl" />
        <div className="relative"><Logo light /></div>
        <div className="relative">
          <ShoppingBag size={56} strokeWidth={1.2} className="mb-6 text-brand-300" />
          <h2 className="max-w-md text-4xl font-extrabold leading-tight tracking-tight">Everything you love, one tap from your door.</h2>
          <p className="mt-4 max-w-sm text-slate-300">Sign in to sync your cart, save favourites and track every order.</p>
        </div>
        <p className="relative text-sm text-slate-400">Secure sign-in with token authentication.</p>
      </div>
      <div className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md animate-fade-up">
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">{title}</h1>
          <p className="mt-2 text-slate-500 dark:text-slate-400">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <p className="mt-6 text-center text-sm text-slate-500">{footer}</p>
        </div>
      </div>
    </div>
  );
}

const useAfterAuth = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  return (data) => {
    dispatch(setCredentials({ token: data.token, user: data.user }));
    navigate(location.state?.from || '/', { replace: true });
  };
};

export function Login() {
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [login, { isLoading }] = useLoginMutation();
  const afterAuth = useAfterAuth();
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try { afterAuth(await login(form).unwrap()); } catch (err) { setError(errorMessage(err, 'Invalid username or password')); }
  };

  return (
    <Shell title="Welcome back" subtitle="Sign in to your account" footer={<>New here? <Link to="/register" className="font-semibold text-brand-600 hover:underline">Create an account</Link></>}>
      <form onSubmit={submit} className="space-y-5" noValidate={false}>
        {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{error}</div>}
        <div><label htmlFor="u" className="label">Username</label><input id="u" className="input" value={form.username} onChange={set('username')} required autoComplete="username" autoFocus /></div>
        <div><label htmlFor="p" className="label">Password</label><PasswordInput id="p" value={form.password} onChange={set('password')} autoComplete="current-password" required /></div>
        <button className="btn-primary btn-lg w-full" disabled={isLoading}>{isLoading && <Spinner size={18} />} Sign in</button>
      </form>
    </Shell>
  );
}

export function Register() {
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [register, { isLoading }] = useRegisterMutation();
  const [login] = useLoginMutation();
  const afterAuth = useAfterAuth();
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await register(form).unwrap();
      afterAuth(await login({ username: form.username, password: form.password }).unwrap());
    } catch (err) { setError(errorMessage(err)); }
  };

  return (
    <Shell title="Create your account" subtitle="It takes less than a minute" footer={<>Already registered? <Link to="/login" className="font-semibold text-brand-600 hover:underline">Sign in</Link></>}>
      <form onSubmit={submit} className="space-y-5">
        {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{error}</div>}
        <div><label htmlFor="ru" className="label">Username</label><input id="ru" className="input" value={form.username} onChange={set('username')} required minLength={3} maxLength={50} autoComplete="username" autoFocus /></div>
        <div><label htmlFor="re" className="label">Email</label><input id="re" type="email" className="input" value={form.email} onChange={set('email')} required autoComplete="email" /></div>
        <div><label htmlFor="rp" className="label">Password</label><PasswordInput id="rp" value={form.password} onChange={set('password')} autoComplete="new-password" required minLength={6} /><p className="mt-1.5 text-xs text-slate-500">At least 6 characters.</p></div>
        <button className="btn-primary btn-lg w-full" disabled={isLoading}>{isLoading && <Spinner size={18} />} Create account</button>
      </form>
    </Shell>
  );
}
