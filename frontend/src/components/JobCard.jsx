import { Briefcase, Clock, IndianRupee, MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';
import { JOB_TYPES, WORK_MODES, salary, timeAgo } from '../utils/format';
import { SkillChip } from './ui';

export default function JobCard({ job, footer, badge }) {
  return (
    <div className="card flex flex-col p-5 transition hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-brand-50 font-bold text-brand-700">
            {job.companyName?.[0]}
          </div>
          <div>
            <Link to={`/jobs/${job.id}`} className="font-semibold text-slate-900 hover:text-brand-700">{job.title}</Link>
            <p className="text-sm text-slate-500">{job.companyName}</p>
          </div>
        </div>
        {badge}
      </div>
      <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
        <span className="flex items-center gap-1"><MapPin className="h-3.5 w-3.5" />{job.location} · {WORK_MODES[job.workMode]}</span>
        <span className="flex items-center gap-1"><Briefcase className="h-3.5 w-3.5" />{JOB_TYPES[job.jobType]}</span>
        <span className="flex items-center gap-1"><IndianRupee className="h-3.5 w-3.5" />{salary(job)}</span>
        <span className="flex items-center gap-1"><Clock className="h-3.5 w-3.5" />{timeAgo(job.createdAt)}</span>
      </div>
      <div className="mt-3 flex flex-wrap gap-1.5">
        {job.skills.slice(0, 5).map((s) => <SkillChip key={s}>{s}</SkillChip>)}
        {job.skills.length > 5 && <SkillChip>+{job.skills.length - 5}</SkillChip>}
      </div>
      {footer && <div className="mt-4 border-t pt-3">{footer}</div>}
    </div>
  );
}
