import { useState } from 'react';
import toast from 'react-hot-toast';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { Logo } from '../components/Layout';
import { homeFor, useAuth } from '../context/AuthContext';

const DEMO = [
  ['Candidate', 'candidate@talenttrack.ai', 'Candidate@123'],
  ['Recruiter', 'recruiter@talenttrack.ai', 'Recruiter@123'],
  ['Admin', 'admin@talenttrack.ai', 'Admin@123'],
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const user = await login(form.email, form.password);
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}!`);
      navigate(location.state?.from || homeFor(user.role), { replace: true });
    } catch (err) {
      toast.error(errorMessage(err, 'Login failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Log in to your account" subtitle={<>New here? <Link className="font-semibold text-brand-600" to="/register">Create an account</Link></>}>
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="label" htmlFor="email">Email</label>
          <input id="email" type="email" required className="input" value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="password">Password</label>
          <input id="password" type="password" required className="input" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </div>
        <button className="btn-primary w-full py-2.5" disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <div className="mt-6 rounded-lg bg-slate-50 p-3">
        <p className="mb-2 text-xs font-semibold uppercase text-slate-500">Demo accounts</p>
        <div className="flex flex-wrap gap-2">
          {DEMO.map(([role, email, password]) => (
            <button key={role} type="button" className="btn-secondary px-3 py-1 text-xs" onClick={() => setForm({ email, password })}>
              {role}
            </button>
          ))}
        </div>
      </div>
    </AuthShell>
  );
}

export function AuthShell({ title, subtitle, children }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 p-4">
      <div className="w-full max-w-md">
        <Logo className="mb-6 justify-center text-xl" />
        <div className="card p-8">
          <h1 className="text-xl font-bold">{title}</h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">{subtitle}</p>
          {children}
        </div>
      </div>
    </div>
  );
}
