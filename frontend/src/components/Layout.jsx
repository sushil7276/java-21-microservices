import { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Heart, LayoutDashboard, LogOut, Menu, Moon, Package, Search, ShoppingCart, Sun, Truck, User, X, ShieldCheck, Headset,
} from 'lucide-react';
import Logo from './Logo';
import { logout, selectIsAdmin, selectIsAuthed, selectUser } from '../features/auth/authSlice';
import { toggleTheme } from '../features/ui/uiSlice';
import { api, useGetCartQuery, useGetFavouritesQuery } from '../services/api';
import { useCategories } from '../utils/hooks';

const shopLink = (category) => `/shop?category=${encodeURIComponent(category)}`;

function IconLink({ to, label, count, children }) {
  return (
    <Link to={to} aria-label={label} className="btn-ghost relative p-2.5">
      {children}
      {count > 0 && (
        <span className="absolute -right-0.5 -top-0.5 grid h-5 min-w-[1.25rem] place-items-center rounded-full bg-brand-600 px-1 text-[11px] font-bold text-white ring-2 ring-white dark:ring-slate-950">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  );
}

function UserMenu() {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const user = useSelector(selectUser);
  const isAdmin = useSelector(selectIsAdmin);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    const close = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const signOut = () => {
    setOpen(false);
    dispatch(logout());
    dispatch(api.util.resetApiState()); // drop the previous user's cached cart/orders
    navigate('/');
  };
  const item = 'flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800';

  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} aria-haspopup="menu" aria-expanded={open}
        className="flex items-center gap-2 rounded-xl p-1.5 pr-3 transition hover:bg-slate-100 dark:hover:bg-slate-800">
        <span className="grid h-8 w-8 place-items-center rounded-lg bg-gradient-to-br from-brand-500 to-fuchsia-500 text-sm font-bold uppercase text-white">{user?.username?.[0]}</span>
        <span className="hidden text-sm font-semibold sm:block">{user?.username}</span>
      </button>
      {open && (
        <div role="menu" className="card absolute right-0 z-40 mt-2 w-56 animate-fade-up p-1.5">
          <div className="border-b border-slate-100 px-3 pb-2 pt-1.5 dark:border-slate-800">
            <p className="truncate text-sm font-semibold">{user?.username}</p>
            <p className="truncate text-xs text-slate-500">{user?.email}</p>
          </div>
          <div className="pt-1.5" onClick={() => setOpen(false)}>
            {isAdmin && <Link to="/admin" className={item} role="menuitem"><LayoutDashboard size={16} /> Admin panel</Link>}
            <Link to="/profile" className={item} role="menuitem"><User size={16} /> My profile</Link>
            <Link to="/orders" className={item} role="menuitem"><Package size={16} /> My orders</Link>
            <Link to="/favourites" className={item} role="menuitem"><Heart size={16} /> Favourites</Link>
          </div>
          <button onClick={signOut} className={`${item} mt-1 border-t border-slate-100 text-rose-600 dark:border-slate-800 dark:text-rose-400`} role="menuitem">
            <LogOut size={16} /> Sign out
          </button>
        </div>
      )}
    </div>
  );
}

function Header() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const authed = useSelector(selectIsAuthed);
  const theme = useSelector((s) => s.ui.theme);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [q, setQ] = useState('');

  const { data: cart } = useGetCartQuery(undefined, { skip: !authed });
  const { data: favs } = useGetFavouritesQuery(undefined, { skip: !authed });
  const cartCount = cart?.items?.reduce((n, i) => n + i.quantity, 0) ?? 0;
  const categories = useCategories();
  const isAdmin = useSelector(selectIsAdmin);
  const NAV = [
    { to: '/', label: 'Home', end: true },
    { to: '/shop', label: 'Shop' },
    ...categories.slice(0, 3).map((c) => ({ to: shopLink(c.name), label: c.name })),
    ...(isAdmin ? [{ to: '/admin', label: 'Admin panel', admin: true }] : []),
  ];

  useEffect(() => { setMobileOpen(false); }, [location.pathname, location.search]);

  const submit = (e) => {
    e.preventDefault();
    navigate(q.trim() ? `/shop?name=${encodeURIComponent(q.trim())}` : '/shop');
  };

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200/70 bg-white/80 backdrop-blur-lg dark:border-slate-800 dark:bg-slate-950/80">
      <div className="container-page flex h-16 items-center gap-4">
        <button className="btn-ghost p-2 lg:hidden" onClick={() => setMobileOpen((o) => !o)} aria-label="Toggle menu" aria-expanded={mobileOpen}>
          {mobileOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
        <Logo />
        <nav className="ml-6 hidden items-center gap-1 lg:flex" aria-label="Main">
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end}
              className={({ isActive }) => `rounded-lg px-3 py-2 text-sm font-medium transition ${n.admin ? 'bg-fuchsia-100 text-fuchsia-800 hover:bg-fuchsia-200 dark:bg-fuchsia-500/15 dark:text-fuchsia-300' : isActive && !n.to.includes('?') ? 'text-brand-600' : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>

        <form onSubmit={submit} role="search" className="relative ml-auto hidden max-w-md flex-1 md:block">
          <Search size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search phones, laptops, audio…" aria-label="Search products"
            className="w-full rounded-full border border-slate-200 bg-slate-100 py-2.5 pl-10 pr-4 text-sm transition focus:border-brand-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-brand-500/15 dark:border-slate-800 dark:bg-slate-900 dark:focus:bg-slate-900" />
        </form>

        <div className="ml-auto flex items-center gap-1 md:ml-0">
          <button onClick={() => dispatch(toggleTheme())} className="btn-ghost p-2.5" aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
            {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
          </button>
          {authed && <IconLink to="/favourites" label="Favourites" count={favs?.length ?? 0}><Heart size={20} /></IconLink>}
          <IconLink to="/cart" label="Cart" count={cartCount}><ShoppingCart size={20} /></IconLink>
          {authed ? <UserMenu /> : (
            <div className="ml-1 flex items-center gap-2">
              <Link to="/login" className="btn-ghost hidden sm:inline-flex">Sign in</Link>
              <Link to="/register" className="btn-primary">Sign up</Link>
            </div>
          )}
        </div>
      </div>

      {mobileOpen && (
        <div className="animate-fade-up border-t border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950 lg:hidden">
          <form onSubmit={submit} role="search" className="relative mb-3">
            <Search size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search products…" aria-label="Search products" className="input rounded-full pl-10" />
          </form>
          <nav className="grid gap-1" aria-label="Mobile">
            {NAV.map((n) => <NavLink key={n.to} to={n.to} end={n.end} className="rounded-lg px-3 py-2.5 font-medium hover:bg-slate-100 dark:hover:bg-slate-800">{n.label}</NavLink>)}
          </nav>
        </div>
      )}
    </header>
  );
}

function Footer() {
  const categories = useCategories();
  const col = 'space-y-2.5 text-sm text-slate-500 dark:text-slate-400';
  const h = 'mb-3 text-sm font-bold text-slate-900 dark:text-white';
  return (
    <footer className="mt-24 border-t border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950">
      <div className="container-page grid gap-6 border-b border-slate-200 py-10 dark:border-slate-800 sm:grid-cols-3">
        {[[Truck, 'Free delivery', 'No shipping fee on any order'], [Package, 'Live order tracking', 'Follow every parcel with its tracking number'], [ShieldCheck, 'Secure checkout', 'Token-secured account and checkout']].map(([I, t, d]) => (
          <div key={t} className="flex items-center gap-4">
            <span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-900/40 dark:text-brand-300"><I size={22} /></span>
            <div><p className="font-semibold text-slate-900 dark:text-white">{t}</p><p className="text-sm text-slate-500 dark:text-slate-400">{d}</p></div>
          </div>
        ))}
      </div>
      <div className="container-page grid gap-10 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-4">
          <Logo />
          <p className="max-w-xs text-sm text-slate-500 dark:text-slate-400">Your one-stop shop for electronics, built on a Spring Boot microservices backend.</p>
        </div>
        <div><p className={h}>Shop</p><ul className={col}>
          <li><Link className="hover:text-brand-600" to="/shop">All products</Link></li>
          {categories.slice(0, 5).map((c) => <li key={c.name}><Link className="hover:text-brand-600" to={shopLink(c.name)}>{c.name}</Link></li>)}
        </ul></div>
        <div><p className={h}>Account</p><ul className={col}>
          <li><Link className="hover:text-brand-600" to="/profile">My profile</Link></li>
          <li><Link className="hover:text-brand-600" to="/orders">Orders & tracking</Link></li>
          <li><Link className="hover:text-brand-600" to="/favourites">Favourites</Link></li>
          <li><Link className="hover:text-brand-600" to="/cart">Cart</Link></li>
        </ul></div>
        <div><p className={h}>Support</p><ul className={col}>
          <li className="flex items-center gap-2"><Headset size={15} /> help@novashop.local</li>
          <li>Mon–Sat, 9:00–18:00</li>
        </ul></div>
      </div>
      <div className="border-t border-slate-200 py-5 text-center text-xs text-slate-500 dark:border-slate-800">© {new Date().getFullYear()} Novashop. All rights reserved.</div>
    </footer>
  );
}

export default function Layout() {
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo(0, 0); }, [pathname]);
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-brand-600 focus:px-4 focus:py-2 focus:text-white">Skip to content</a>
      <div className="bg-gradient-to-r from-brand-700 via-violet-700 to-fuchsia-700 px-4 py-2 text-center text-xs font-medium text-white sm:text-sm">
        🚚 Free delivery on every order · Track your parcel from checkout to doorstep
      </div>
      <Header />
      <main id="main" className="flex-1"><Outlet /></main>
      <Footer />
    </div>
  );
}
