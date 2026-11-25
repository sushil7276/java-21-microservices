import { NavLink, Outlet } from 'react-router-dom';
import { Boxes, Cog, History, LayoutDashboard, ReceiptText, Users } from 'lucide-react';

const LINKS = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/products', label: 'Products', icon: Boxes },
  { to: '/admin/orders', label: 'Orders', icon: ReceiptText },
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/batch', label: 'Batch jobs', icon: Cog },
  { to: '/admin/audit', label: 'Audit log', icon: History },
];

export default function AdminLayout() {
  return (
    <div className="container-page grid gap-8 py-10 lg:grid-cols-[14rem_1fr]">
      <aside>
        <nav className="card flex gap-1 overflow-x-auto p-2 lg:sticky lg:top-24 lg:flex-col" aria-label="Admin">
          {LINKS.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end}
              className={({ isActive }) => `flex shrink-0 items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${isActive ? 'bg-brand-600 text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'}`}>
              <Icon size={17} /> {label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="min-w-0"><Outlet /></div>
    </div>
  );
}
