import { ExternalLink, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { adminApi } from '../../api/services';
import { PageLoader } from '../../components/ui';
import { JOB_TYPES, date } from '../../utils/format';

export default function AdminJobs() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);

  const load = useCallback(() => adminApi.jobs({ page, size: 15 }).then(setData), [page]);
  useEffect(() => { load(); }, [load]);

  const remove = async (job) => {
    if (!window.confirm(`Remove "${job.title}" by ${job.companyName}? This deletes all its applications.`)) return;
    try {
      await adminApi.deleteJob(job.id);
      toast.success('Job removed');
      load();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  if (!data) return <PageLoader />;
  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Job moderation</h1>
        <p className="text-sm text-slate-500">Review every posting on the platform and remove spam or fake listings.</p>
      </div>
      <div className="card overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="border-b bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th className="px-5 py-3">Job</th><th className="px-5 py-3">Recruiter</th><th className="px-5 py-3">Type</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Posted</th><th className="px-5 py-3" /></tr>
          </thead>
          <tbody className="divide-y">
            {data.content.map((j) => (
              <tr key={j.id}>
                <td className="px-5 py-3"><p className="font-medium">{j.title}</p><p className="text-xs text-slate-500">{j.companyName} · {j.location}</p></td>
                <td className="px-5 py-3 text-slate-600">{j.recruiterName}</td>
                <td className="px-5 py-3">{JOB_TYPES[j.jobType]}</td>
                <td className="px-5 py-3"><span className={`chip ${j.status === 'OPEN' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{j.status.toLowerCase()}</span></td>
                <td className="px-5 py-3 text-slate-500">{date(j.createdAt)}</td>
                <td className="px-5 py-3">
                  <div className="flex justify-end gap-1">
                    <Link to={`/jobs/${j.id}`} className="btn-ghost p-2" title="View"><ExternalLink className="h-4 w-4" /></Link>
                    <button className="btn-ghost p-2 text-rose-600" title="Remove" onClick={() => remove(j)}><Trash2 className="h-4 w-4" /></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="flex items-center justify-between border-t px-5 py-3 text-sm text-slate-500">
          <span>{data.totalElements} jobs</span>
          <div className="flex gap-2">
            <button className="btn-secondary py-1" disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
            <button className="btn-secondary py-1" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </div>
      </div>
    </div>
  );
}
