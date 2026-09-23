import { AlertCircle, CalendarClock, CheckCircle2, FileText, Send, Sparkles, Trophy, XCircle } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { candidateApi } from '../../api/services';
import JobCard from '../../components/JobCard';
import { EmptyState, PageLoader, ScoreRing, SkillChip, StatCard } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { dateTime } from '../../utils/format';

export default function CandidateDashboard() {
  const { user } = useAuth();
  const [dash, setDash] = useState(null);
  const [recs, setRecs] = useState(null);

  useEffect(() => {
    candidateApi.dashboard().then(setDash);
    candidateApi.recommendations(6).then(setRecs).catch(() => setRecs([]));
  }, []);

  if (!dash) return <PageLoader />;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="page-title">Hi {user.fullName.split(' ')[0]} 👋</h1>
        <p className="text-sm text-slate-500">Here's how your job search is going.</p>
      </div>

      {(dash.profileCompleteness < 100 || !dash.hasResume) && (
        <div className="card flex flex-col items-start justify-between gap-4 border-brand-200 bg-brand-50 p-5 sm:flex-row sm:items-center">
          <div className="flex items-center gap-4">
            <ScoreRing score={dash.profileCompleteness} size={56} />
            <div>
              <p className="font-semibold text-brand-900">Your profile is {dash.profileCompleteness}% complete</p>
              <p className="text-sm text-brand-800/80">
                {dash.hasResume ? 'Fill in the remaining fields to stand out.' : 'Upload your resume so our AI can match you with the best jobs.'}
              </p>
            </div>
          </div>
          <Link to="/candidate/profile" className="btn-primary"><FileText className="h-4 w-4" /> Complete profile</Link>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Send} label="Applications" value={dash.totalApplications} />
        <StatCard icon={CalendarClock} label="In interview" value={dash.inInterview} tone="sky" />
        <StatCard icon={Trophy} label="Offers" value={dash.offers} tone="green" />
        <StatCard icon={XCircle} label="Not selected" value={dash.rejected} tone="rose" />
      </div>

      {dash.upcomingInterviews.length > 0 && (
        <div className="card p-5">
          <h2 className="mb-3 font-semibold">Upcoming interviews</h2>
          <div className="divide-y">
            {dash.upcomingInterviews.map((i) => (
              <div key={i.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm">
                <div>
                  <p className="font-medium">{i.jobTitle} · {i.companyName}</p>
                  <p className="text-slate-500">{dateTime(i.scheduledAt)} · {i.durationMin} min · {i.mode.toLowerCase()}</p>
                </div>
                {i.meetingLink && <a href={i.meetingLink} target="_blank" rel="noreferrer" className="btn-secondary">Join link</a>}
              </div>
            ))}
          </div>
        </div>
      )}

      <div>
        <div className="mb-4 flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-brand-600" />
          <h2 className="text-lg font-semibold">Recommended for you</h2>
        </div>
        {!recs ? <PageLoader /> : recs.length === 0 ? (
          <EmptyState icon={AlertCircle} title="No recommendations yet"
            text="Add skills or upload a resume and we'll rank open jobs by how well they fit you."
            action={<Link to="/candidate/profile" className="btn-primary">Update profile</Link>} />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {recs.map((r) => (
              <JobCard key={r.job.id} job={r.job} badge={<ScoreRing score={r.matchScore} size={44} stroke={4} />}
                footer={
                  <div className="space-y-1.5 text-xs">
                    {r.matchedSkills.length > 0 && (
                      <div className="flex flex-wrap items-center gap-1">
                        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                        {r.matchedSkills.slice(0, 4).map((s) => <SkillChip key={s} tone="match">{s}</SkillChip>)}
                      </div>
                    )}
                    {r.missingSkills.length > 0 && (
                      <div className="flex flex-wrap items-center gap-1">
                        <XCircle className="h-3.5 w-3.5 text-rose-500" />
                        {r.missingSkills.slice(0, 3).map((s) => <SkillChip key={s} tone="missing">{s}</SkillChip>)}
                      </div>
                    )}
                  </div>
                } />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
