import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, Lock, ShieldCheck } from 'lucide-react';
import { api, useGetMeQuery, useUpdateUserMutation } from '../services/api';
import { logout, setUser } from '../features/auth/authSlice';
import PasswordInput from '../components/PasswordInput';
import { PageHeader, PageLoader, Spinner, StatusBadge } from '../components/ui';
import { errorMessage, formatDate } from '../utils/format';

export default function Profile() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { data: me, isLoading } = useGetMeQuery();
  const [update, { isLoading: saving }] = useUpdateUserMutation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => { if (me) setEmail(me.email); }, [me]);
  if (isLoading) return <PageLoader />;
  const locked = me.role === 'ADMIN'; // the admin login is fixed; the backend refuses changes too

  const submit = async (e) => {
    e.preventDefault();
    if (locked) return;
    setError('');
    setSuccess('');
    const body = { id: me.id };
    if (email !== me.email) body.email = email;
    if (password) body.password = password;
    if (!body.email && !body.password) { setError('You have not changed anything yet.'); return; }
    try {
      const updated = await update(body).unwrap();
      if (body.password) {
        // Changing the password revokes every login token of this user (in Redis), including this one:
        // sign out here and ask for the new password.
        dispatch(logout());
        dispatch(api.util.resetApiState());
        navigate('/login', { replace: true, state: { notice: 'Your password was changed. Please sign in with your new password.' } });
        return;
      }
      dispatch(setUser(updated));
      setPassword('');
      // say exactly what changed
      setSuccess(body.password && body.email
        ? 'Your email and password were changed successfully.'
        : body.password
          ? 'Your password was changed successfully. Use the new password next time you sign in.'
          : `Your email is now ${updated.email}.`);
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div className="container-page max-w-2xl py-10">
      <PageHeader title="My profile" subtitle="Manage your account details." />
      <div className="card p-6 sm:p-8">
        <div className="mb-8 flex items-center gap-4">
          <span className="grid h-16 w-16 place-items-center rounded-2xl bg-gradient-to-br from-brand-500 to-fuchsia-500 text-2xl font-bold uppercase text-white">{me.username[0]}</span>
          <div>
            <p className="text-xl font-bold">{me.username}</p>
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-500"><StatusBadge status={me.role} /> Member since {formatDate(me.createdAt)}</div>
          </div>
        </div>
        <form onSubmit={submit} className="space-y-5">
          {locked && (
            <div role="note" className="flex items-start gap-3 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:bg-amber-500/10 dark:text-amber-200">
              <Lock size={18} className="mt-0.5 shrink-0" />
              <span>This is the administrator account. Its username, email and password are fixed and cannot be changed. You can reset other users' email and password under Admin panel → Users.</span>
            </div>
          )}
          {success && (
            <div role="status" className="flex items-start gap-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:bg-emerald-500/10 dark:text-emerald-300">
              <CheckCircle2 size={18} className="mt-0.5 shrink-0" /> <span>{success}</span>
            </div>
          )}
          {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{error}</div>}
          <div><label className="label" htmlFor="pu">Username</label><input id="pu" className="input opacity-60" value={me.username} disabled /></div>
          <div><label className="label" htmlFor="pe">Email</label><input id="pe" type="email" className="input disabled:cursor-not-allowed disabled:opacity-60" value={email} onChange={(e) => setEmail(e.target.value)} required disabled={locked} /></div>
          <div><label className="label" htmlFor="pp">New password</label><PasswordInput id="pp" className="disabled:cursor-not-allowed disabled:opacity-60" value={password} minLength={6} onChange={(e) => setPassword(e.target.value)} placeholder={locked ? 'Password cannot be changed' : 'Leave blank to keep current'} autoComplete="new-password" disabled={locked} /></div>
          {!locked && <button className="btn-primary" disabled={saving}>{saving && <Spinner size={16} />} Save changes</button>}
        </form>
        <p className="mt-6 flex items-center gap-2 border-t border-slate-200 pt-5 text-xs text-slate-500 dark:border-slate-800"><ShieldCheck size={14} /> Your session expires automatically after one hour.</p>
      </div>
    </div>
  );
}
