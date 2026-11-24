import { Link } from 'react-router-dom';
import { ShoppingBag } from 'lucide-react';

export default function Logo({ to = '/', light = false }) {
  return (
    <Link to={to} className="flex items-center gap-2.5" aria-label="Novashop home">
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-fuchsia-500 text-white shadow-lift">
        <ShoppingBag size={19} />
      </span>
      <span className={`text-xl font-extrabold tracking-tight ${light ? 'text-white' : 'text-slate-900 dark:text-white'}`}>
        Nova<span className="text-brand-500">shop</span>
      </span>
    </Link>
  );
}
