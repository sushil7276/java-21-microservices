import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

/** Password field with an eye button to show / hide what you typed. Accepts all normal <input> props. */
export default function PasswordInput({ className = '', ...props }) {
  const [show, setShow] = useState(false);
  return (
    <div className="relative">
      <input {...props} type={show ? 'text' : 'password'} className={`input pr-11 ${className}`} />
      <button
        type="button"
        onClick={() => setShow((s) => !s)}
        disabled={props.disabled}
        aria-label={show ? 'Hide password' : 'Show password'}
        aria-pressed={show}
        title={show ? 'Hide password' : 'Show password'}
        className="absolute right-3 top-1/2 -translate-y-1/2 rounded p-0.5 text-slate-400 transition hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-40 dark:hover:text-slate-200"
      >
        {show ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </div>
  );
}
