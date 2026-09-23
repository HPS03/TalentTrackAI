import { Briefcase, FileCheck2, Gauge, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Area, AreaChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { adminApi } from '../../api/services';
import { PageLoader, StatCard } from '../../components/ui';
import { STAGES, STAGE_META } from '../../utils/format';

export default function AdminDashboard() {
  const [d, setD] = useState(null);
  useEffect(() => { adminApi.dashboard().then(setD); }, []);
  if (!d) return <PageLoader />;

  const stages = STAGES.map((s) => ({ name: STAGE_META[s].label, value: d.applicationsByStage[s] || 0, color: STAGE_META[s].hex }))
    .filter((s) => s.value > 0);
  const users = [
    { name: 'Candidates', value: d.candidates, color: '#6366f1' },
    { name: 'Recruiters', value: d.recruiters, color: '#0ea5e9' },
    { name: 'Admins', value: d.admins, color: '#f59e0b' },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="page-title">Platform analytics</h1>
        <p className="text-sm text-slate-500">Health of the TalentTrack AI marketplace.</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Users} label="Users" value={d.totalUsers} hint={`${d.candidates} candidates · ${d.recruiters} recruiters`} />
        <StatCard icon={Briefcase} label="Jobs" value={d.totalJobs} hint={`${d.openJobs} open`} tone="sky" />
        <StatCard icon={FileCheck2} label="Applications" value={d.totalApplications} tone="green" />
        <StatCard icon={Gauge} label="Avg. ATS score" value={`${d.averageAts}%`} tone="amber" />
      </div>
      <div className="grid gap-6 xl:grid-cols-3">
        <div className="card p-6 xl:col-span-2">
          <h2 className="mb-4 font-semibold">Jobs posted (last 6 months)</h2>
          <div className="h-64">
            <ResponsiveContainer>
              <AreaChart data={d.jobsPerMonth} margin={{ left: -20 }}>
                <defs>
                  <linearGradient id="g" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#6366f1" stopOpacity={0.35} />
                    <stop offset="100%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="month" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                <Tooltip />
                <Area type="linear" dataKey="count" stroke="#6366f1" strokeWidth={2} fill="url(#g)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="card p-6">
          <h2 className="mb-4 font-semibold">User mix</h2>
          <Donut data={users} />
        </div>
      </div>
      <div className="card p-6">
        <h2 className="mb-4 font-semibold">Applications by stage</h2>
        {stages.length === 0 ? <p className="text-sm text-slate-500">No applications yet.</p> : <Donut data={stages} />}
      </div>
    </div>
  );
}

function Donut({ data }) {
  return (
    <div className="flex flex-col items-center gap-4 sm:flex-row">
      <div className="h-48 w-48">
        <ResponsiveContainer>
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
              {data.map((e) => <Cell key={e.name} fill={e.color} />)}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <ul className="space-y-1.5 text-sm">
        {data.map((e) => (
          <li key={e.name} className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-sm" style={{ background: e.color }} />
            {e.name}: <span className="font-semibold">{e.value}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
