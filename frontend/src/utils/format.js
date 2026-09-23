export const STAGES = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED'];

export const STAGE_META = {
  APPLIED: { label: 'Applied', color: 'bg-slate-100 text-slate-700', bar: 'border-t-slate-400', hex: '#94a3b8' },
  SCREENING: { label: 'Screening', color: 'bg-sky-100 text-sky-700', bar: 'border-t-sky-500', hex: '#0ea5e9' },
  INTERVIEW: { label: 'Interview', color: 'bg-violet-100 text-violet-700', bar: 'border-t-violet-500', hex: '#8b5cf6' },
  OFFER: { label: 'Offer', color: 'bg-amber-100 text-amber-800', bar: 'border-t-amber-500', hex: '#f59e0b' },
  HIRED: { label: 'Hired', color: 'bg-emerald-100 text-emerald-700', bar: 'border-t-emerald-500', hex: '#10b981' },
  REJECTED: { label: 'Rejected', color: 'bg-rose-100 text-rose-700', bar: 'border-t-rose-500', hex: '#f43f5e' },
};

export const JOB_TYPES = { FULL_TIME: 'Full-time', PART_TIME: 'Part-time', INTERNSHIP: 'Internship', CONTRACT: 'Contract' };
export const WORK_MODES = { ONSITE: 'On-site', REMOTE: 'Remote', HYBRID: 'Hybrid' };

export function salary(job) {
  const fmt = (n) => (n >= 100000 ? `₹${(n / 100000).toFixed(n % 100000 ? 1 : 0)}L` : `₹${(n / 1000).toFixed(0)}k`);
  const suffix = job.jobType === 'INTERNSHIP' ? '/mo' : '/yr';
  if (job.salaryMin && job.salaryMax) return `${fmt(job.salaryMin)} – ${fmt(job.salaryMax)}${suffix}`;
  if (job.salaryMax || job.salaryMin) return `${fmt(job.salaryMax || job.salaryMin)}${suffix}`;
  return 'Not disclosed';
}

export function timeAgo(iso) {
  if (!iso) return '';
  const s = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (s < 60) return 'just now';
  const units = [[86400 * 30, 'mo'], [86400, 'd'], [3600, 'h'], [60, 'm']];
  for (const [secs, u] of units) if (s >= secs) return `${Math.floor(s / secs)}${u} ago`;
  return 'just now';
}

export const dateTime = (iso) =>
  iso ? new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '';

export const date = (iso) => (iso ? new Date(iso).toLocaleDateString(undefined, { dateStyle: 'medium' }) : '');

export function scoreColor(score) {
  if (score >= 75) return { text: 'text-emerald-600', stroke: '#10b981', bg: 'bg-emerald-50' };
  if (score >= 50) return { text: 'text-amber-600', stroke: '#f59e0b', bg: 'bg-amber-50' };
  return { text: 'text-rose-600', stroke: '#f43f5e', bg: 'bg-rose-50' };
}

export const initials = (name = '') =>
  name.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
