import { Search, SearchX } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { jobsApi } from '../api/services';
import JobCard from '../components/JobCard';
import { EmptyState, PageLoader } from '../components/ui';
import { JOB_TYPES, WORK_MODES } from '../utils/format';
import PublicHeader from './PublicHeader';

export default function Jobs({ publicView }) {
  const [params, setParams] = useSearchParams();
  const [result, setResult] = useState(null);
  const [q, setQ] = useState(params.get('q') || '');
  const filters = Object.fromEntries(params.entries());

  useEffect(() => {
    setResult(null);
    jobsApi.search({ size: 12, ...filters }).then(setResult).catch(() => setResult({ content: [], totalPages: 0 }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params]);

  const update = (key, value) => {
    const next = new URLSearchParams(params);
    if (value === '' || value == null) next.delete(key); else next.set(key, value);
    if (key !== 'page') next.delete('page');
    setParams(next);
  };

  const content = (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Jobs & Internships</h1>
        <p className="text-sm text-slate-500">{result ? `${result.totalElements ?? 0} open positions` : 'Loading…'}</p>
      </div>
      <div className="card grid gap-3 p-4 md:grid-cols-6">
        <form className="relative md:col-span-2" onSubmit={(e) => { e.preventDefault(); update('q', q); }}>
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input className="input pl-9" placeholder="Title, company or skill" value={q} onChange={(e) => setQ(e.target.value)} />
        </form>
        <input className="input" placeholder="Location" defaultValue={filters.location}
          onBlur={(e) => update('location', e.target.value)} onKeyDown={(e) => e.key === 'Enter' && update('location', e.currentTarget.value)} />
        <select className="input" value={filters.jobType || ''} onChange={(e) => update('jobType', e.target.value)}>
          <option value="">All types</option>
          {Object.entries(JOB_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="input" value={filters.workMode || ''} onChange={(e) => update('workMode', e.target.value)}>
          <option value="">Any work mode</option>
          {Object.entries(WORK_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="input" value={filters.sort || 'newest'} onChange={(e) => update('sort', e.target.value)}>
          <option value="newest">Newest</option>
          <option value="salary">Highest salary</option>
          <option value="oldest">Oldest</option>
        </select>
      </div>

      {!result ? <PageLoader /> : result.content.length === 0 ? (
        <EmptyState icon={SearchX} title="No jobs match your filters" text="Try a broader keyword or remove some filters." />
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {result.content.map((job) => (
              <JobCard key={job.id} job={job}
                badge={job.alreadyApplied && <span className="chip bg-emerald-50 text-emerald-700">Applied</span>} />
            ))}
          </div>
          {result.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <button className="btn-secondary" disabled={result.page === 0} onClick={() => update('page', result.page - 1)}>Previous</button>
              <span className="text-sm text-slate-500">Page {result.page + 1} of {result.totalPages}</span>
              <button className="btn-secondary" disabled={result.page + 1 >= result.totalPages} onClick={() => update('page', result.page + 1)}>Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );

  if (!publicView) return content;
  return (
    <div className="min-h-screen">
      <PublicHeader />
      <main className="mx-auto max-w-6xl px-4 py-8">{content}</main>
    </div>
  );
}
