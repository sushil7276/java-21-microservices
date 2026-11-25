import { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { CalendarClock, Download, FileUp, Play, Trash2 } from 'lucide-react';
import {
  api, useGetBatchJobQuery, useGetBatchJobsQuery, useGetSalesReportsQuery, useImportProductsMutation,
  useRunCleanupMutation, useRunSalesReportMutation,
} from '../../services/api';
import { ErrorState, NoData, PageHeader, Spinner, StatusBadge } from '../../components/ui';
import { errorMessage, formatDate, formatMoney } from '../../utils/format';

const JOB_NAMES = { importProductsJob: 'Product import', dailySalesReportJob: 'Daily sales report', cleanupJob: 'Clean-up' };
const isRunning = (status) => status === 'STARTING' || status === 'STARTED';

const TEMPLATE = `name,description,price,stock,category,imageUrl
Wireless Mouse,"Quiet, comfortable mouse",1499,40,Accessories,https://example.com/mouse.jpg
USB-C Cable,1 metre braided cable,399,120,Accessories,
`;

/** One line that sums up what a finished job did. */
function resultText(job) {
  const step = (name) => job.steps.find((s) => s.name === name);
  if (job.jobName === 'importProductsJob') {
    const s = job.steps[0];
    if (!s) return '';
    const created = s.details?.created ?? 0;
    return `${created} created, ${s.skipped + (s.details?.refusedByProductService ?? 0)} rejected`;
  }
  if (job.jobName === 'dailySalesReportJob') {
    const d = job.steps[0]?.details;
    return d?.orders != null ? `${d.reportDate}: ${d.orders} orders, ${formatMoney(d.revenue)}` : '';
  }
  return `carts ${step('cleanCartsStep')?.details?.deleted ?? '–'}, audit events ${step('cleanAuditStep')?.details?.deleted ?? '–'}, files ${step('cleanFilesStep')?.details?.deleted ?? '–'}`;
}

/** Follows ONE job run: asks the server for its status every second until it has finished. */
function useJobProgress(executionId) {
  const dispatch = useDispatch();
  const { data: job } = useGetBatchJobQuery(executionId, { skip: !executionId, pollingInterval: 1000 });
  const running = !job || isRunning(job.status);
  const finishedFor = useRef(null);
  useEffect(() => {
    if (job && !isRunning(job.status) && finishedFor.current !== job.executionId) {
      finishedFor.current = job.executionId;
      // the job changed data: refresh the lists that show it
      dispatch(api.util.invalidateTags(['Batch', 'Product']));
    }
  }, [job, dispatch]);
  return { job, running: Boolean(executionId) && running };
}

const Card = ({ icon: Icon, title, text, children }) => (
  <section className="card p-6">
    <h2 className="flex items-center gap-2 text-lg font-bold"><Icon size={19} className="text-brand-600" />{title}</h2>
    {text && <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{text}</p>}
    <div className="mt-5">{children}</div>
  </section>
);

function JobResult({ job }) {
  if (!job) return null;
  return (
    <div className="mt-4 rounded-xl bg-slate-50 p-4 text-sm dark:bg-slate-800/60">
      <div className="flex flex-wrap items-center gap-3">
        <StatusBadge status={job.status} />
        {isRunning(job.status) && <Spinner size={16} className="text-brand-500" />}
        <span className="text-slate-600 dark:text-slate-300">{resultText(job)}</span>
        {job.durationMs != null && <span className="text-xs text-slate-400">{job.durationMs} ms</span>}
      </div>
      {job.message && <p className="mt-2 text-rose-600">Failed: {job.message}</p>}
      {job.rejectedRows?.length > 0 && (
        <div className="mt-4 overflow-x-auto">
          <p className="mb-2 font-semibold">Lines that were not imported</p>
          <table className="w-full text-left text-xs">
            <thead><tr className="text-slate-500"><th className="py-1 pr-3">Line</th><th className="py-1 pr-3">Product</th><th className="py-1">Reason</th></tr></thead>
            <tbody>
              {job.rejectedRows.map((r) => (
                <tr key={`${r.lineNumber}-${r.reason}`} className="border-t border-slate-200 dark:border-slate-700">
                  <td className="py-1.5 pr-3 tabular-nums">{r.lineNumber}</td>
                  <td className="py-1.5 pr-3">{r.productName || '–'}</td>
                  <td className="py-1.5 text-rose-700 dark:text-rose-300">{r.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function ImportCard() {
  const [file, setFile] = useState(null);
  const [executionId, setExecutionId] = useState(null);
  const [error, setError] = useState('');
  const [start, { isLoading }] = useImportProductsMutation();
  const { job, running } = useJobProgress(executionId);
  const input = useRef(null);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setExecutionId(null);
    try {
      const res = await start(file).unwrap();
      setExecutionId(res.executionId);
      setFile(null);
      if (input.current) input.current.value = '';
    } catch (err) { setError(errorMessage(err)); }
  };
  const downloadTemplate = () => {
    const url = URL.createObjectURL(new Blob([TEMPLATE], { type: 'text/csv' }));
    const a = Object.assign(document.createElement('a'), { href: url, download: 'products-template.csv' });
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Card icon={FileUp} title="Import products from a CSV file"
      text="Upload a file with the columns name, description, price, stock, category, imageUrl. Good lines are added to the shop; bad lines are listed with the reason and never stop the import.">
      <form onSubmit={submit} className="flex flex-wrap items-center gap-3">
        <input ref={input} type="file" accept=".csv,text/csv" onChange={(e) => setFile(e.target.files?.[0] ?? null)} aria-label="CSV file"
          className="block max-w-full text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-brand-50 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-brand-700 hover:file:bg-brand-100" />
        <button className="btn-primary" disabled={!file || isLoading || running}>{isLoading || running ? <Spinner size={16} /> : <Play size={16} />} Start import</button>
        <button type="button" className="btn-outline" onClick={downloadTemplate}><Download size={16} /> CSV template</button>
      </form>
      {error && <p role="alert" className="mt-3 text-sm text-rose-600">{error}</p>}
      <JobResult job={job} />
    </Card>
  );
}

function ReportCard() {
  const today = new Date().toLocaleDateString('en-CA');                       // yyyy-mm-dd in the browser's time zone
  const [date, setDate] = useState(today);
  const [executionId, setExecutionId] = useState(null);
  const [error, setError] = useState('');
  const [run, { isLoading }] = useRunSalesReportMutation();
  const { data: reports, isLoading: loading, error: loadError, refetch } = useGetSalesReportsQuery(30);
  const { job, running } = useJobProgress(executionId);

  const start = async () => {
    setError('');
    try { setExecutionId((await run(date).unwrap()).executionId); } catch (err) { setError(errorMessage(err)); }
  };

  return (
    <Card icon={CalendarClock} title="Daily sales reports"
      text="Made automatically every night for the day that just ended. You can also make one for any day (for today it only counts the orders so far). Making a report again replaces the old one.">
      <div className="flex flex-wrap items-center gap-3">
        <input type="date" value={date} max={today} onChange={(e) => setDate(e.target.value)} className="input w-auto" aria-label="Report date" />
        <button className="btn-primary" onClick={start} disabled={!date || isLoading || running}>{isLoading || running ? <Spinner size={16} /> : <Play size={16} />} Make report</button>
      </div>
      {error && <p role="alert" className="mt-3 text-sm text-rose-600">{error}</p>}
      <JobResult job={job} />

      <div className="mt-6 overflow-x-auto">
        {loadError ? <ErrorState message={errorMessage(loadError)} onRetry={refetch} /> : loading ? <Spinner className="text-brand-500" /> : reports?.length === 0 ? (
          <NoData compact icon={CalendarClock} title="No reports yet" text="The first one is made tonight, or make one now." />
        ) : (
          <table className="w-full">
            <thead className="border-b border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">Day</th><th className="th text-right">Orders</th><th className="th text-right">Items</th><th className="th text-right">Revenue</th><th className="th">Best sellers</th></tr></thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {reports.map((r) => (
                <tr key={r.date}>
                  <td className="td font-semibold">{r.date}</td>
                  <td className="td text-right tabular-nums">{r.orders}</td>
                  <td className="td text-right tabular-nums">{r.itemsSold}</td>
                  <td className="td text-right font-semibold tabular-nums">{formatMoney(r.revenue)}</td>
                  <td className="td text-xs text-slate-500">{r.topProducts.slice(0, 3).map((p) => `${p.productName} ×${p.quantity}`).join(', ') || '–'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </Card>
  );
}

function CleanupCard() {
  const [executionId, setExecutionId] = useState(null);
  const [error, setError] = useState('');
  const [run, { isLoading }] = useRunCleanupMutation();
  const { job, running } = useJobProgress(executionId);
  const start = async () => {
    setError('');
    try { setExecutionId((await run().unwrap()).executionId); } catch (err) { setError(errorMessage(err)); }
  };
  return (
    <Card icon={Trash2} title="Clean-up"
      text="Runs every night. Deletes shopping carts nobody changed for 14 days, audit events older than 90 days and uploaded CSV files older than 7 days.">
      <button className="btn-outline" onClick={start} disabled={isLoading || running}>{isLoading || running ? <Spinner size={16} /> : <Play size={16} />} Run clean-up now</button>
      {error && <p role="alert" className="mt-3 text-sm text-rose-600">{error}</p>}
      <JobResult job={job} />
    </Card>
  );
}

function History() {
  const { data, isLoading, error, refetch } = useGetBatchJobsQuery(20, { pollingInterval: 5000 });
  return (
    <section className="card overflow-hidden">
      <h2 className="px-6 py-4 text-lg font-bold">Job history</h2>
      {error ? <div className="p-6"><ErrorState message={errorMessage(error)} onRetry={refetch} /></div> : isLoading ? <div className="p-6"><Spinner className="text-brand-500" /></div> : data.length === 0 ? (
        <NoData title="No jobs have run yet" text="Every run of an import, report or clean-up is listed here." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="border-y border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/40"><tr><th className="th">#</th><th className="th">Job</th><th className="th">Status</th><th className="th">Started</th><th className="th text-right">Duration</th><th className="th">Result</th></tr></thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {data.map((j) => (
                <tr key={j.executionId}>
                  <td className="td text-slate-500">{j.executionId}</td>
                  <td className="td font-semibold">{JOB_NAMES[j.jobName] ?? j.jobName}</td>
                  <td className="td"><StatusBadge status={j.status} /></td>
                  <td className="td text-slate-500">{formatDate(j.startTime, true)}</td>
                  <td className="td text-right tabular-nums">{j.durationMs != null ? `${j.durationMs} ms` : '–'}</td>
                  <td className="td text-xs text-slate-600 dark:text-slate-400">{j.message ? <span className="text-rose-600">{j.message}</span> : resultText(j)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default function AdminBatch() {
  return (
    <>
      <PageHeader title="Batch jobs" subtitle="Work that is done in bulk: importing products, daily reports and clean-up." />
      <div className="space-y-6">
        <ImportCard />
        <ReportCard />
        <CleanupCard />
        <History />
      </div>
    </>
  );
}
