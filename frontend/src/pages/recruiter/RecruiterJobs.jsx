import { Briefcase, KanbanSquare, Pencil, Plus, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { recruiterApi } from '../../api/services';
import JobForm from '../../components/JobForm';
import { EmptyState, Modal, PageLoader } from '../../components/ui';
import { JOB_TYPES, WORK_MODES, timeAgo } from '../../utils/format';

export default function RecruiterJobs() {
  const [jobs, setJobs] = useState(null);
  const [editing, setEditing] = useState(null); // null = closed, {} = new, job = edit
  const [saving, setSaving] = useState(false);

  const load = () => recruiterApi.jobs().then(setJobs);
  useEffect(() => { load(); }, []);

  const save = async (body) => {
    setSaving(true);
    try {
      if (editing.id) await recruiterApi.updateJob(editing.id, body);
      else await recruiterApi.createJob(body);
      toast.success(editing.id ? 'Job updated' : 'Job published');
      setEditing(null);
      load();
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (job) => {
    try {
      await recruiterApi.setJobStatus(job.id, job.status === 'OPEN' ? 'CLOSED' : 'OPEN');
      load();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  const remove = async (job) => {
    if (!window.confirm(`Delete "${job.title}" and all of its applications?`)) return;
    try {
      await recruiterApi.deleteJob(job.id);
      toast.success('Job deleted');
      load();
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  if (!jobs) return <PageLoader />;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="page-title">Jobs & Pipelines</h1>
          <p className="text-sm text-slate-500">Open a job's board to move candidates through your hiring workflow.</p>
        </div>
        <button className="btn-primary" onClick={() => setEditing({})}><Plus className="h-4 w-4" /> Post a job</button>
      </div>

      {jobs.length === 0 ? (
        <EmptyState icon={Briefcase} title="No jobs posted yet" text="Publish your first job or internship to start receiving AI-ranked applicants."
          action={<button className="btn-primary" onClick={() => setEditing({})}>Post a job</button>} />
      ) : (
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr>
                <th className="px-5 py-3">Role</th><th className="px-5 py-3">Type</th><th className="px-5 py-3">Applicants</th>
                <th className="px-5 py-3">Status</th><th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {jobs.map((j) => (
                <tr key={j.id} className="hover:bg-slate-50/60">
                  <td className="px-5 py-4">
                    <p className="font-medium text-slate-900">{j.title}</p>
                    <p className="text-xs text-slate-500">{j.location} · {WORK_MODES[j.workMode]} · posted {timeAgo(j.createdAt)}</p>
                  </td>
                  <td className="px-5 py-4">{JOB_TYPES[j.jobType]}</td>
                  <td className="px-5 py-4"><span className="chip bg-brand-50 text-brand-700">{j.applicantCount}</span></td>
                  <td className="px-5 py-4">
                    <button onClick={() => toggle(j)} title="Toggle status"
                      className={`chip cursor-pointer ${j.status === 'OPEN' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>
                      {j.status === 'OPEN' ? '● Open' : 'Closed'}
                    </button>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end gap-1">
                      <Link to={`/recruiter/jobs/${j.id}/board`} className="btn-secondary py-1.5"><KanbanSquare className="h-4 w-4" /> Board</Link>
                      <button className="btn-ghost p-2" title="Edit" onClick={() => setEditing(j)}><Pencil className="h-4 w-4" /></button>
                      <button className="btn-ghost p-2 text-rose-600" title="Delete" onClick={() => remove(j)}><Trash2 className="h-4 w-4" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing?.id ? 'Edit job' : 'Post a new job'} wide>
        {editing && <JobForm initial={editing} onSubmit={save} submitting={saving} />}
      </Modal>
    </div>
  );
}
