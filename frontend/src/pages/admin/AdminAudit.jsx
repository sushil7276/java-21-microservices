import { useState } from 'react';
import { useGetAuditQuery } from '../../services/api';
import { History } from 'lucide-react';
import { ErrorState, NoData, PageHeader, PageLoader } from '../../components/ui';
import { errorMessage, formatDate } from '../../utils/format';

export default function AdminAudit() {
  const [entityId, setEntityId] = useState('');
  const [applied, setApplied] = useState('');
  const { data, isLoading, isFetching, error, refetch } = useGetAuditQuery({ entityId: applied });

  return (
    <>
      <PageHeader title="Audit log" subtitle="Events published by the services over Kafka, stored in MongoDB." />
      <form onSubmit={(e) => { e.preventDefault(); setApplied(entityId.trim()); }} className="mb-4 flex max-w-md gap-2">
        <input className="input" placeholder="Filter by entity (order) ID" value={entityId} onChange={(e) => setEntityId(e.target.value)} aria-label="Entity ID" />
        <button className="btn-primary">Apply</button>
        {applied && <button type="button" className="btn-outline" onClick={() => { setEntityId(''); setApplied(''); }}>Clear</button>}
      </form>
      {isLoading ? <PageLoader /> : error ? <ErrorState message={errorMessage(error)} onRetry={refetch} /> : (
        <div className={`card overflow-x-auto ${isFetching ? 'opacity-60' : ''}`}>
          <table className="w-full">
            <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">Time</th><th className="th">Event</th><th className="th">Source</th><th className="th">Entity</th><th className="th">User</th><th className="th">Payload</th></tr></thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {data.map((a) => (
                <tr key={a.id}>
                  <td className="td whitespace-nowrap text-slate-500">{formatDate(a.receivedAt, true)}</td>
                  <td className="td"><span className="badge bg-brand-50 text-brand-700 dark:bg-brand-900/40 dark:text-brand-300">{a.eventType}</span></td>
                  <td className="td">{a.source}</td><td className="td font-mono text-xs">{a.entityId}</td><td className="td">{a.userId ?? '–'}</td>
                  <td className="td max-w-xs truncate font-mono text-xs text-slate-500" title={JSON.stringify(a.payload)}>{JSON.stringify(a.payload)}</td>
                </tr>
              ))}
              {data.length === 0 && <tr><td colSpan={6}><NoData icon={History} title="No audit events" text={applied ? 'Nothing matches that ID.' : 'Events appear here when customers place and pay for orders.'} /></td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
