import { ArrowLeft, Briefcase, CalendarClock, CheckCircle2, IndianRupee, MapPin, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { candidateApi, jobsApi } from '../api/services';
import { Modal, PageLoader, ScoreBar, ScoreRing, SkillChip } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { JOB_TYPES, WORK_MODES, date, salary, timeAgo } from '../utils/format';
import PublicHeader from './PublicHeader';

export default function JobDetail({ publicView }) {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [applyOpen, setApplyOpen] = useState(false);
  const [coverLetter, setCoverLetter] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    jobsApi.get(id).then(setJob).catch((e) => { toast.error(errorMessage(e)); navigate('/jobs'); });
  }, [id, navigate]);

  const apply = async () => {
    setSubmitting(true);
    try {
      const app = await candidateApi.apply(id, coverLetter || null);
      setResult(app.ats);
      setJob({ ...job, alreadyApplied: true });
      toast.success('Application submitted!');
    } catch (e) {
      toast.error(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  if (!job) return <PageLoader />;

  const cta = !user ? (
    <Link to="/login" state={{ from: `/jobs/${id}` }} className="btn-primary w-full">Log in to apply</Link>
  ) : user.role !== 'CANDIDATE' ? null : job.alreadyApplied ? (
    <Link to="/candidate/applications" className="btn-secondary w-full"><CheckCircle2 className="h-4 w-4 text-emerald-600" /> Applied · track status</Link>
  ) : job.status !== 'OPEN' ? (
    <button className="btn-secondary w-full" disabled>Applications closed</button>
  ) : (
    <button className="btn-primary w-full" onClick={() => setApplyOpen(true)}>Apply now</button>
  );

  const content = (
    <div className="mx-auto max-w-5xl space-y-6">
      <button onClick={() => navigate(-1)} className="btn-ghost -ml-3"><ArrowLeft className="h-4 w-4" /> Back</button>
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="card p-6 lg:col-span-2">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-brand-50 text-xl font-bold text-brand-700">{job.companyName[0]}</div>
            <div>
              <h1 className="text-2xl font-bold">{job.title}</h1>
              <p className="text-slate-500">{job.companyName} · posted {timeAgo(job.createdAt)}</p>
            </div>
          </div>
          <h2 className="mb-2 mt-8 font-semibold">About the role</h2>
          <p className="whitespace-pre-line text-sm leading-relaxed text-slate-700">{job.description}</p>
          <h2 className="mb-2 mt-6 font-semibold">Required skills</h2>
          <div className="flex flex-wrap gap-2">{job.skills.map((s) => <SkillChip key={s} tone="brand">{s}</SkillChip>)}</div>
        </div>
        <div className="space-y-4">
          <div className="card space-y-3 p-6 text-sm">
            <Info icon={MapPin} label="Location" value={`${job.location} · ${WORK_MODES[job.workMode]}`} />
            <Info icon={Briefcase} label="Type" value={`${JOB_TYPES[job.jobType]} · ${job.minExperience}+ yrs`} />
            <Info icon={IndianRupee} label="Compensation" value={salary(job)} />
            <Info icon={Users} label="Openings" value={`${job.openings}${job.applicantCount != null ? ` · ${job.applicantCount} applicants` : ''}`} />
            {job.deadline && <Info icon={CalendarClock} label="Apply by" value={date(job.deadline)} />}
            <div className="pt-2">{cta}</div>
          </div>
        </div>
      </div>

      <Modal open={applyOpen} onClose={() => { setApplyOpen(false); setResult(null); }} title={result ? 'Application submitted' : `Apply to ${job.title}`}>
        {result ? (
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <ScoreRing score={result.overall} size={80} />
              <div>
                <p className="font-semibold">Your ATS match score</p>
                <p className="text-sm text-slate-500">{result.summary}</p>
              </div>
            </div>
            <ScoreBar label="Skill match" value={result.skillScore} />
            <ScoreBar label="Semantic similarity" value={result.semanticScore} />
            <ScoreBar label="Experience fit" value={result.experienceScore} />
            <div className="flex justify-end gap-2">
              <Link to="/candidate/applications" className="btn-primary">Track application</Link>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <p className="text-sm text-slate-600">Your profile and resume will be shared with {job.companyName}. We'll compute an AI match score instantly.</p>
            <div>
              <label className="label">Cover letter (optional)</label>
              <textarea className="input min-h-[140px]" value={coverLetter} maxLength={5000}
                onChange={(e) => setCoverLetter(e.target.value)} placeholder="Why are you a great fit?" />
            </div>
            <div className="flex justify-end gap-2">
              <button className="btn-secondary" onClick={() => setApplyOpen(false)}>Cancel</button>
              <button className="btn-primary" disabled={submitting} onClick={apply}>{submitting ? 'Scoring…' : 'Submit application'}</button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );

  if (!publicView) return content;
  return (
    <div className="min-h-screen">
      <PublicHeader />
      <main className="px-4 py-8">{content}</main>
    </div>
  );
}

function Info({ icon: Icon, label, value }) {
  return (
    <div className="flex items-start gap-3">
      <Icon className="mt-0.5 h-4 w-4 text-slate-400" />
      <div>
        <p className="text-xs text-slate-500">{label}</p>
        <p className="font-medium text-slate-800">{value}</p>
      </div>
    </div>
  );
}
