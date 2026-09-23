import clsx from 'clsx';
import { Briefcase, GraduationCap } from 'lucide-react';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { homeFor, useAuth } from '../context/AuthContext';
import { AuthShell } from './Login';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'CANDIDATE', companyName: '' });
  const [loading, setLoading] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const user = await register({ ...form, companyName: form.role === 'RECRUITER' ? form.companyName : null });
      toast.success('Account created!');
      navigate(user.role === 'CANDIDATE' ? '/candidate/profile' : homeFor(user.role), { replace: true });
    } catch (err) {
      toast.error(errorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  const RoleOption = ({ value, icon: Icon, title, text }) => (
    <button type="button" onClick={() => setForm({ ...form, role: value })}
      className={clsx('rounded-lg border-2 p-3 text-left transition',
        form.role === value ? 'border-brand-600 bg-brand-50' : 'border-slate-200 hover:border-slate-300')}>
      <Icon className={clsx('mb-1 h-5 w-5', form.role === value ? 'text-brand-600' : 'text-slate-400')} />
      <p className="text-sm font-semibold">{title}</p>
      <p className="text-xs text-slate-500">{text}</p>
    </button>
  );

  return (
    <AuthShell title="Create your account" subtitle={<>Already registered? <Link className="font-semibold text-brand-600" to="/login">Log in</Link></>}>
      <form onSubmit={submit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <RoleOption value="CANDIDATE" icon={GraduationCap} title="I'm a candidate" text="Find jobs & internships" />
          <RoleOption value="RECRUITER" icon={Briefcase} title="I'm hiring" text="Post jobs, track pipeline" />
        </div>
        <div>
          <label className="label">Full name</label>
          <input required className="input" value={form.fullName} onChange={set('fullName')} />
        </div>
        {form.role === 'RECRUITER' && (
          <div>
            <label className="label">Company name</label>
            <input required className="input" value={form.companyName} onChange={set('companyName')} />
          </div>
        )}
        <div>
          <label className="label">Email</label>
          <input type="email" required className="input" value={form.email} onChange={set('email')} />
        </div>
        <div>
          <label className="label">Password</label>
          <input type="password" required minLength={8} className="input" value={form.password} onChange={set('password')} />
          <p className="mt-1 text-xs text-slate-500">At least 8 characters with a letter and a number.</p>
        </div>
        <button className="btn-primary w-full py-2.5" disabled={loading}>{loading ? 'Creating…' : 'Create account'}</button>
      </form>
    </AuthShell>
  );
}
