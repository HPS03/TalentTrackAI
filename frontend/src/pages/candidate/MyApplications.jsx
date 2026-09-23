import clsx from 'clsx';
import { CalendarClock, Check, ChevronDown, Inbox, Video } from 'lucide-react';
import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { candidateApi } from '../../api/services';
import { EmptyState, PageLoader, ScoreBar, ScoreRing, SkillChip, StageBadge } from '../../components/ui';
import { STAGE_META, dateTime, timeAgo } from '../../utils/format';

const TRACK = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED'];

function StageTracker({ stage }) {
  if (stage === 'REJECTED') {
    return <p className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">The company decided not to move forward. Keep applying — the right role is out there!</p>;
  }
  const current = TRACK.indexOf(stage);
  return (
    <div className="flex items-center">
      {TRACK.map((s, i) => (
        <div key={s} className="flex flex-1 items-center last:flex-none">
          <div className="flex flex-col items-center">
            <div className={clsx('flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold',
              i < current ? 'bg-brand-600 text-white' : i === current ? 'bg-brand-600 text-white ring-4 ring-brand-100' : 'bg-slate-200 text-slate-500')}>
              {i < current ? <Check className="h-4 w-4" /> : i + 1}
            </div>
            <span className="mt-1 text-[11px] text-slate-500">{STAGE_META[s].label}</span>
          </div>
          {i < TRACK.length - 1 && <div className={clsx('mx-1 mb-4 h-0.5 flex-1', i < current ? 'bg-brand-600' : 'bg-slate-200')} />}
        </div>
      ))}
    </div>
  );
}

export default function MyApplications() {
  const [apps, setApps] = useState(null);
  const [open, setOpen] = useState(null);

  useEffect(() => { candidateApi.applications().then(setApps); }, []);

  const withdraw = async (id) => {
    if (!window.confirm('Withdraw this application? This cannot be undone.')) return;
    try {
      await candidateApi.withdraw(id);
      setApps(apps.filter((a) => a.id !== id));
      toast.success('Application withdrawn');
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  if (!apps) return <PageLoader />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">My Applications</h1>
        <p className="text-sm text-slate-500">Track every application through the hiring pipeline in real time.</p>
      </div>
      {apps.length === 0 ? (
        <EmptyState icon={Inbox} title="No applications yet" text="Browse open roles and apply with one click."
          action={<Link to="/jobs" className="btn-primary">Find jobs</Link>} />
      ) : apps.map((a) => (
        <div key={a.id} className="card">
          <button className="flex w-full flex-wrap items-center gap-4 p-5 text-left" onClick={() => setOpen(open === a.id ? null : a.id)}>
            <ScoreRing score={a.ats.overall} size={52} stroke={5} />
            <div className="min-w-0 flex-1">
              <p className="font-semibold">{a.job.title}</p>
              <p className="text-sm text-slate-500">{a.job.companyName} · applied {timeAgo(a.appliedAt)}</p>
            </div>
            <StageBadge stage={a.stage} />
            <ChevronDown className={clsx('h-5 w-5 text-slate-400 transition', open === a.id && 'rotate-180')} />
          </button>
          {open === a.id && (
            <div className="space-y-6 border-t p-5">
              <StageTracker stage={a.stage} />
              {a.interviews.length > 0 && (
                <div className="space-y-2">
                  {a.interviews.map((i) => (
                    <div key={i.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-violet-50 p-3 text-sm">
                      <span className="flex items-center gap-2 text-violet-800">
                        <CalendarClock className="h-4 w-4" /> {i.mode.toLowerCase()} interview · {dateTime(i.scheduledAt)} ({i.durationMin} min)
                      </span>
                      {i.meetingLink && <a className="btn-secondary py-1 text-xs" href={i.meetingLink} target="_blank" rel="noreferrer"><Video className="h-3.5 w-3.5" /> Join</a>}
                    </div>
                  ))}
                </div>
              )}
              <div className="grid gap-6 md:grid-cols-2">
                <div className="space-y-3">
                  <p className="text-sm font-semibold">AI match breakdown</p>
                  <ScoreBar label="Skill match" value={a.ats.skillScore} />
                  <ScoreBar label="Semantic similarity" value={a.ats.semanticScore} />
                  <ScoreBar label="Experience fit" value={a.ats.experienceScore} />
                  <p className="text-xs text-slate-500">{a.ats.summary}</p>
                </div>
                <div className="space-y-3">
                  <div>
                    <p className="mb-1.5 text-sm font-semibold">Matched skills</p>
                    <div className="flex flex-wrap gap-1.5">{a.ats.matchedSkills.map((s) => <SkillChip key={s} tone="match">{s}</SkillChip>)}</div>
                  </div>
                  {a.ats.missingSkills.length > 0 && (
                    <div>
                      <p className="mb-1.5 text-sm font-semibold">Skills to improve</p>
                      <div className="flex flex-wrap gap-1.5">{a.ats.missingSkills.map((s) => <SkillChip key={s} tone="missing">{s}</SkillChip>)}</div>
                    </div>
                  )}
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Link to={`/jobs/${a.job.id}`} className="btn-secondary">View job</Link>
                {a.stage !== 'HIRED' && <button className="btn-ghost text-rose-600" onClick={() => withdraw(a.id)}>Withdraw</button>}
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
