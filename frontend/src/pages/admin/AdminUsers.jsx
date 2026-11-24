import { useState } from 'react';
import { useSelector } from 'react-redux';
import { CheckCircle2, Eye, EyeOff, KeyRound, Lock, Trash2, Wand2 } from 'lucide-react';
import { useDeleteUserMutation, useGetUsersQuery, useUpdateUserMutation } from '../../services/api';
import { selectUser } from '../../features/auth/authSlice';
import { ConfirmModal, ErrorState, Modal, PageHeader, PageLoader, Spinner, StatusBadge } from '../../components/ui';
import { errorMessage, formatDate } from '../../utils/format';

/** random 12-character password (no look-alike characters) for handing a temporary password to a user */
function generatePassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';
  const bytes = crypto.getRandomValues(new Uint32Array(12));
  return Array.from(bytes, (b) => chars[b % chars.length]).join('');
}

/** Admin edits another user's email and/or password (the backend refuses to change admin accounts). */
function EditUserForm({ user, onClose }) {
  const [update, { isLoading }] = useUpdateUserMutation();
  const [email, setEmail] = useState(user.email);
  const [password, setPassword] = useState('');
  const [show, setShow] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    const body = { id: user.id };
    if (email.trim() !== user.email) body.email = email.trim();
    if (password) body.password = password;
    if (!body.email && !body.password) { setError('Change the email or enter a new password first.'); return; }
    try {
      await update(body).unwrap();
      const what = body.email && body.password ? 'email and password were' : body.password ? 'password was' : 'email was';
      setDone(`The ${what} changed for ${user.username}.`);
    } catch (err) { setError(errorMessage(err)); }
  };

  if (done) {
    return (
      <div className="space-y-5">
        <div role="status" className="flex items-start gap-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800 dark:bg-emerald-500/10 dark:text-emerald-300">
          <CheckCircle2 size={18} className="mt-0.5 shrink-0" /> <span>{done}</span>
        </div>
        {password && <p className="text-sm text-slate-600 dark:text-slate-400">New password: <span className="select-all rounded bg-slate-100 px-2 py-0.5 font-mono font-semibold dark:bg-slate-800">{password}</span></p>}
        <div className="flex justify-end"><button className="btn-primary" onClick={onClose}>Done</button></div>
      </div>
    );
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{error}</div>}
      <div><label className="label" htmlFor="eu">Username</label><input id="eu" className="input opacity-60" value={user.username} disabled /></div>
      <div><label className="label" htmlFor="ee">Email</label><input id="ee" type="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
      <div>
        <label className="label" htmlFor="ep">New password</label>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <input id="ep" type={show ? 'text' : 'password'} className="input pr-11 font-mono" value={password} minLength={6} maxLength={100} placeholder="Leave blank to keep the current password" autoComplete="new-password" onChange={(e) => setPassword(e.target.value)} />
            <button type="button" onClick={() => setShow((v) => !v)} aria-label={show ? 'Hide password' : 'Show password'} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">{show ? <EyeOff size={18} /> : <Eye size={18} />}</button>
          </div>
          <button type="button" className="btn-outline" onClick={() => { setPassword(generatePassword()); setShow(true); }} title="Fill in a random password"><Wand2 size={16} /> Generate</button>
        </div>
        <p className="mt-1.5 text-xs text-slate-500">At least 6 characters. Tell the user the new password; they can sign in with it straight away. Sessions they already have stay valid until they expire (1 hour).</p>
      </div>
      <div className="flex justify-end gap-3 pt-2">
        <button type="button" onClick={onClose} className="btn-outline">Cancel</button>
        <button className="btn-primary" disabled={isLoading}>{isLoading && <Spinner size={16} />} Save changes</button>
      </div>
    </form>
  );
}

export default function AdminUsers() {
  const me = useSelector(selectUser);
  const { data, isLoading, error, refetch } = useGetUsersQuery();
  const [del, { isLoading: deleting }] = useDeleteUserMutation();
  const [target, setTarget] = useState(null);
  const [editing, setEditing] = useState(null);
  const [actionError, setActionError] = useState('');

  if (isLoading) return <PageLoader />;
  if (error) return <ErrorState message={errorMessage(error)} onRetry={refetch} />;

  const confirm = async () => {
    setActionError('');
    try { await del(target.id).unwrap(); }
    catch (e) { setActionError(errorMessage(e)); }
    setTarget(null);
  };

  return (
    <>
      <PageHeader title="Users" subtitle={`${data.length} registered accounts`} />
      {actionError && <div role="alert" className="mb-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">{actionError}</div>}
      <div className="card overflow-x-auto">
        <table className="w-full">
          <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">ID</th><th className="th">User</th><th className="th">Role</th><th className="th">Joined</th><th className="th" /></tr></thead>
          <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
            {data.map((u) => (
              <tr key={u.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/30">
                <td className="td text-slate-500">#{u.id}</td>
                <td className="td"><p className="font-semibold">{u.username}</p><p className="text-xs text-slate-500">{u.email}</p></td>
                <td className="td"><StatusBadge status={u.role} /></td><td className="td text-slate-500">{formatDate(u.createdAt)}</td>
                <td className="td">
                  <div className="flex items-center justify-end gap-1">
                    {u.role === 'ADMIN'
                      ? <span className="inline-flex items-center gap-1.5 px-2 text-xs text-slate-400" title="The admin account is protected: it cannot be edited or deleted"><Lock size={14} /> Protected</span>
                      : <>
                          <button className="btn-ghost btn-sm" onClick={() => setEditing(u)} aria-label={`Edit ${u.username}`}><KeyRound size={15} /> Edit</button>
                          {u.id !== me?.id && <button className="btn-ghost btn-sm text-rose-600" onClick={() => setTarget(u)} aria-label={`Delete ${u.username}`}><Trash2 size={15} /></button>}
                        </>}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={`Edit ${editing?.username ?? 'user'}`}>
        {editing && <EditUserForm key={editing.id} user={editing} onClose={() => setEditing(null)} />}
      </Modal>
      <ConfirmModal open={Boolean(target)} onClose={() => setTarget(null)} onConfirm={confirm} busy={deleting} title="Delete user?" text={`The account “${target?.username}” will be permanently removed.`} />
    </>
  );
}
