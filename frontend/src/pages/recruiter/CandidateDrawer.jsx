import { CalendarPlus, ExternalLink, FileText, MessageSquare, Star } from 'lucide-react';
import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../api/client';
import { openBlob, recruiterApi } from '../../api/services';
import { Avatar, Drawer, PageLoader, ScoreBar, ScoreRing, SkillChip, StageBadge } from '../../components/ui';
import { STAGE_META, dateTime, timeAgo } from '../../utils/format';

const ACTIVITY_TEXT = {
  APPLIED: () => 'applied',
  STAGE_CHANGED: (a) => `moved ${STAGE_META[a.fromStage]?.label} → ${STAGE_META[a.toStage]?.label}`,
  NOTE: () => 'added a note',
  RATING: () => 'rated the candidate',
  INTERVIEW_SCHEDULED: () => 'scheduled an interview',
  WITHDRAWN: () => 'withdrew',
};

export default function CandidateDrawer({ applicationId, onClose, onChanged }) {
  const [app, setApp] = useState(null);
  const [note, setNote] = useState('');
  const [interview, setInterview] = useState(null);

  const load = () => recruiterApi.application(applicationId).then(setApp).catch((e) => toast.error(errorMessage(e)));

  useEffect(() => {
    setApp(null);
    setInterview(null);
    if (applicationId) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applicationId]);

  const run = async (fn, success) => {
    try {
      await fn();
      if (success) toast.success(success);
      await load();
      onChanged();
      return true;
    } catch (e) {
      toast.error(errorMessage(e));
      return false;
    }
  };

  const move = (stage) => run(() => recruiterApi.moveStage(app.id, { stage }), `Moved to ${STAGE_META[stage].label}`);
  const addNote = (e) => {
    e.preventDefault();
    if (note.trim()) run(() => recruiterApi.addNote(app.id, note), 'Note added').then((ok) => ok && setNote(''));
  };
  const schedule = (e) => {
    e.preventDefault();
    run(() => recruiterApi.scheduleInterview(app.id, {
      ...interview, scheduledAt: new Date(interview.scheduledAt).toISOString(), durationMin: Number(interview.durationMin),
    }), 'Interview scheduled — candidate notified').then((ok) => ok && setInterview(null));
  };

  const c = app?.candidate;
  return (
    <Drawer open={!!applicationId} onClose={onClose} title={app ? `TT-${app.id} · ${c.fullName}` : 'Loading…'}>
      {!app ? <PageLoader /> : (
        <div className="space-y-6">
          <div className="flex items-start gap-4">
            <Avatar name={c.fullName} className="h-14 w-14 text-lg" />
            <div className="flex-1">
              <p className="text-lg font-semibold">{c.fullName}</p>
              <p className="text-sm text-slate-500">{c.headline || 'No headline'} · {c.experienceYears} yrs exp</p>
              <p className="text-sm text-slate-500">{c.email}{c.phone ? ` · ${c.phone}` : ''}{c.location ? ` · ${c.location}` : ''}</p>
              <div className="mt-2 flex flex-wrap gap-3 text-sm">
                {c.resumeFileName && (
                  <button className="flex items-center gap-1 text-brand-600" onClick={() => recruiterApi.resumeBlob(app.id).then(openBlob).catch((e) => toast.error(errorMessage(e)))}>
                    <FileText className="h-4 w-4" /> Resume
                  </button>
                )}
                {[['LinkedIn', c.linkedinUrl], ['GitHub', c.githubUrl], ['Portfolio', c.portfolioUrl]].filter(([, u]) => u).map(([l, u]) => (
                  <a key={l} href={u} target="_blank" rel="noreferrer" className="flex items-center gap-1 text-brand-600"><ExternalLink className="h-3.5 w-3.5" />{l}</a>
                ))}
              </div>
            </div>
            <StageBadge stage={app.stage} />
          </div>

          <div className="flex flex-wrap items-center gap-2 rounded-lg bg-slate-50 p-3">
            <span className="text-xs font-semibold uppercase text-slate-500">Move to</span>
            {app.allowedTransitions.length === 0 && <span className="text-sm text-slate-500">Final stage reached</span>}
            {app.allowedTransitions.map((s) => (
              <button key={s} className={s === 'REJECTED' ? 'btn-secondary py-1 text-xs text-rose-600' : 'btn-secondary py-1 text-xs'} onClick={() => move(s)}>
                {STAGE_META[s].label}
              </button>
            ))}
            <div className="ml-auto flex items-center gap-0.5" title="Your rating">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} onClick={() => run(() => recruiterApi.rate(app.id, n))} aria-label={`Rate ${n}`}>
                  <Star className={`h-4 w-4 ${n <= (app.rating || 0) ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`} />
                </button>
              ))}
            </div>
          </div>

          <section className="card p-5">
            <div className="flex items-center gap-5">
              <ScoreRing score={app.ats.overall} size={80} stroke={7} />
              <div className="flex-1 space-y-2">
                <ScoreBar label="Skill match" value={app.ats.skillScore} />
                <ScoreBar label="Semantic similarity" value={app.ats.semanticScore} />
                <ScoreBar label="Experience fit" value={app.ats.experienceScore} />
              </div>
            </div>
            <p className="mt-3 text-sm text-slate-600">{app.ats.summary}</p>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {app.ats.matchedSkills.map((s) => <SkillChip key={s} tone="match">{s}</SkillChip>)}
              {app.ats.missingSkills.map((s) => <SkillChip key={s} tone="missing">{s}</SkillChip>)}
            </div>
          </section>

          {app.coverLetter && (
            <section>
              <h3 className="mb-2 text-sm font-semibold">Cover letter</h3>
              <p className="whitespace-pre-line rounded-lg bg-slate-50 p-4 text-sm text-slate-700">{app.coverLetter}</p>
            </section>
          )}

          <section>
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-sm font-semibold">Interviews</h3>
              {!['HIRED', 'REJECTED'].includes(app.stage) && !interview && (
                <button className="btn-secondary py-1 text-xs" onClick={() => setInterview({ scheduledAt: '', durationMin: 45, mode: 'VIDEO', meetingLink: '', notes: '' })}>
                  <CalendarPlus className="h-3.5 w-3.5" /> Schedule
                </button>
              )}
            </div>
            {interview && (
              <form onSubmit={schedule} className="mb-3 grid gap-3 rounded-lg border p-4 sm:grid-cols-2">
                <input type="datetime-local" required className="input" value={interview.scheduledAt} onChange={(e) => setInterview({ ...interview, scheduledAt: e.target.value })} />
                <select className="input" value={interview.mode} onChange={(e) => setInterview({ ...interview, mode: e.target.value })}>
                  <option value="VIDEO">Video</option><option value="PHONE">Phone</option><option value="ONSITE">On-site</option>
                </select>
                <input type="number" min="15" max="480" className="input" value={interview.durationMin} onChange={(e) => setInterview({ ...interview, durationMin: e.target.value })} />
                <input className="input" placeholder="Meeting link" value={interview.meetingLink} onChange={(e) => setInterview({ ...interview, meetingLink: e.target.value })} />
                <div className="flex justify-end gap-2 sm:col-span-2">
                  <button type="button" className="btn-ghost" onClick={() => setInterview(null)}>Cancel</button>
                  <button className="btn-primary">Schedule & notify</button>
                </div>
              </form>
            )}
            {app.interviews.length === 0 ? <p className="text-sm text-slate-500">None scheduled.</p> : app.interviews.map((i) => (
              <p key={i.id} className="mb-1 rounded-lg bg-violet-50 p-2 text-sm text-violet-800">{dateTime(i.scheduledAt)} · {i.mode.toLowerCase()} · {i.durationMin} min</p>
            ))}
          </section>

          <section>
            <h3 className="mb-2 flex items-center gap-1 text-sm font-semibold"><MessageSquare className="h-4 w-4" /> Activity & notes</h3>
            <form onSubmit={addNote} className="mb-4 flex gap-2">
              <input className="input" placeholder="Add a private note for your team…" value={note} onChange={(e) => setNote(e.target.value)} maxLength={2000} />
              <button className="btn-primary">Add</button>
            </form>
            <ol className="relative space-y-4 border-l border-slate-200 pl-5">
              {app.activities.map((a) => (
                <li key={a.id} className="relative">
                  <span className="absolute -left-[25px] top-1 h-2.5 w-2.5 rounded-full bg-brand-500 ring-4 ring-white" />
                  <p className="text-sm"><span className="font-medium">{a.actorName}</span> {ACTIVITY_TEXT[a.type]?.(a)}</p>
                  {a.message && <p className={`mt-1 text-sm ${a.type === 'NOTE' ? 'rounded-lg bg-amber-50 p-2 text-slate-700' : 'text-slate-500'}`}>{a.message}</p>}
                  <p className="text-xs text-slate-400">{timeAgo(a.createdAt)}</p>
                </li>
              ))}
            </ol>
          </section>
        </div>
      )}
    </Drawer>
  );
}
