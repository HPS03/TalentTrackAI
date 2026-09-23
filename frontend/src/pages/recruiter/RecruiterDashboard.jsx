import { Briefcase, CalendarClock, Gauge, Trophy, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { recruiterApi } from '../../api/services';
import { Avatar, EmptyState, PageLoader, StageBadge, StatCard } from '../../components/ui';
import { STAGES, STAGE_META, dateTime, scoreColor, timeAgo } from '../../utils/format';

export default function RecruiterDashboard() {
  const [d, setD] = useState(null);
  useEffect(() => { recruiterApi.dashboard().then(setD); }, []);
  if (!d) return <PageLoader />;

  const funnel = STAGES.map((s) => ({ stage: STAGE_META[s].label, count: d.funnel[s] || 0, color: STAGE_META[s].hex }));

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="page-title">Recruitment overview</h1>
          <p className="text-sm text-slate-500">Your hiring pipeline at a glance.</p>
        </div>
        <Link to="/recruiter/jobs" className="btn-primary"><Briefcase className="h-4 w-4" /> Manage jobs</Link>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Briefcase} label="Open jobs" value={d.openJobs} hint={`${d.totalJobs} total`} />
        <StatCard icon={Users} label="Total applicants" value={d.totalApplicants} tone="sky" />
        <StatCard icon={Gauge} label="Avg. ATS score" value={`${d.averageAts}%`} tone="amber" />
        <StatCard icon={Trophy} label="Hired" value={d.hired} tone="green" />
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="card p-6 xl:col-span-2">
          <h2 className="mb-4 font-semibold">Pipeline funnel</h2>
          <div className="h-64">
            <ResponsiveContainer>
              <BarChart data={funnel} margin={{ left: -20 }}>
                <XAxis dataKey="stage" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {funnel.map((f) => <Cell key={f.stage} fill={f.color} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="card p-6">
          <h2 className="mb-4 font-semibold">Top jobs by applicants</h2>
          {d.topJobs.length === 0 ? <p className="text-sm text-slate-500">No jobs posted yet.</p> : (
            <div className="space-y-3">
              {d.topJobs.map((j) => (
                <Link key={j.jobId} to={`/recruiter/jobs/${j.jobId}/board`} className="flex items-center justify-between rounded-lg p-2 hover:bg-slate-50">
                  <span className="truncate text-sm font-medium">{j.title}</span>
                  <span className="chip bg-brand-50 text-brand-700">{j.applicants}</span>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-3">
        <div className="card p-6 xl:col-span-2">
          <h2 className="mb-4 font-semibold">Recent applicants</h2>
          {d.recentApplicants.length === 0 ? (
            <EmptyState icon={Users} title="No applicants yet" text="Share your job postings to start receiving applications." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-xs uppercase text-slate-500">
                  <tr><th className="pb-2">Candidate</th><th className="pb-2">Job</th><th className="pb-2">ATS</th><th className="pb-2">Stage</th><th className="pb-2">Applied</th></tr>
                </thead>
                <tbody className="divide-y">
                  {d.recentApplicants.map((a) => (
                    <tr key={a.applicationId}>
                      <td className="py-2.5"><div className="flex items-center gap-2"><Avatar name={a.candidateName} className="h-7 w-7 text-xs" />{a.candidateName}</div></td>
                      <td className="py-2.5 text-slate-600">{a.jobTitle}</td>
                      <td className={`py-2.5 font-semibold ${scoreColor(a.atsScore).text}`}>{a.atsScore}%</td>
                      <td className="py-2.5"><StageBadge stage={a.stage} /></td>
                      <td className="py-2.5 text-slate-500">{timeAgo(a.appliedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <div className="card p-6">
          <h2 className="mb-4 flex items-center gap-2 font-semibold"><CalendarClock className="h-4 w-4" /> Upcoming interviews</h2>
          {d.upcomingInterviews.length === 0 ? <p className="text-sm text-slate-500">Nothing scheduled.</p> : (
            <div className="space-y-3">
              {d.upcomingInterviews.map((i) => (
                <div key={i.id} className="rounded-lg border p-3 text-sm">
                  <p className="font-medium">{i.candidateName}</p>
                  <p className="text-slate-500">{i.jobTitle}</p>
                  <p className="mt-1 text-xs text-violet-700">{dateTime(i.scheduledAt)} · {i.mode.toLowerCase()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
