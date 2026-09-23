import { useState } from 'react';
import { JOB_TYPES, WORK_MODES } from '../utils/format';
import { SkillInput } from './ui';

const EMPTY = {
  title: '', companyName: '', location: '', jobType: 'FULL_TIME', workMode: 'ONSITE', minExperience: 0,
  salaryMin: '', salaryMax: '', description: '', skills: [], openings: 1, deadline: '',
};

export default function JobForm({ initial, onSubmit, submitting }) {
  const [form, setForm] = useState(() => ({ ...EMPTY, ...initial, salaryMin: initial?.salaryMin ?? '',
    salaryMax: initial?.salaryMax ?? '', deadline: initial?.deadline ?? '' }));
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      minExperience: Number(form.minExperience),
      openings: Number(form.openings),
      salaryMin: form.salaryMin === '' ? null : Number(form.salaryMin),
      salaryMax: form.salaryMax === '' ? null : Number(form.salaryMax),
      deadline: form.deadline || null,
      companyName: form.companyName || null,
    });
  };

  return (
    <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
      <div className="sm:col-span-2">
        <label className="label">Job title *</label>
        <input className="input" required value={form.title} onChange={set('title')} placeholder="e.g. Java Backend Developer" />
      </div>
      <div>
        <label className="label">Company (defaults to yours)</label>
        <input className="input" value={form.companyName || ''} onChange={set('companyName')} />
      </div>
      <div>
        <label className="label">Location *</label>
        <input className="input" required value={form.location} onChange={set('location')} placeholder="Bengaluru / Remote" />
      </div>
      <div>
        <label className="label">Type</label>
        <select className="input" value={form.jobType} onChange={set('jobType')}>
          {Object.entries(JOB_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      <div>
        <label className="label">Work mode</label>
        <select className="input" value={form.workMode} onChange={set('workMode')}>
          {Object.entries(WORK_MODES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      <div>
        <label className="label">Min. experience (years)</label>
        <input type="number" min="0" max="30" className="input" value={form.minExperience} onChange={set('minExperience')} />
      </div>
      <div>
        <label className="label">Openings</label>
        <input type="number" min="1" className="input" value={form.openings} onChange={set('openings')} />
      </div>
      <div>
        <label className="label">Salary min (₹, yearly / monthly for internships)</label>
        <input type="number" min="0" className="input" value={form.salaryMin} onChange={set('salaryMin')} />
      </div>
      <div>
        <label className="label">Salary max (₹)</label>
        <input type="number" min="0" className="input" value={form.salaryMax} onChange={set('salaryMax')} />
      </div>
      <div className="sm:col-span-2">
        <label className="label">Required skills * (used for AI matching)</label>
        <SkillInput value={form.skills} onChange={(skills) => setForm({ ...form, skills })} />
      </div>
      <div className="sm:col-span-2">
        <label className="label">Description * (min 30 characters)</label>
        <textarea className="input min-h-[140px]" required minLength={30} value={form.description} onChange={set('description')}
          placeholder="Responsibilities, requirements, what makes the role exciting..." />
      </div>
      <div>
        <label className="label">Application deadline</label>
        <input type="date" className="input" value={form.deadline} onChange={set('deadline')} />
      </div>
      <div className="flex items-end justify-end">
        <button className="btn-primary" disabled={submitting || form.skills.length === 0}>
          {submitting ? 'Saving…' : initial?.id ? 'Save changes' : 'Publish job'}
        </button>
      </div>
    </form>
  );
}
