import { Search } from 'lucide-react';
import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../api/client';
import { adminApi } from '../../api/services';
import { Avatar, PageLoader } from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { date } from '../../utils/format';

export default function AdminUsers() {
  const { user: me } = useAuth();
  const [filters, setFilters] = useState({ role: '', q: '', page: 0 });
  const [data, setData] = useState(null);

  useEffect(() => {
    const t = setTimeout(() => adminApi.users({ ...filters, role: filters.role || undefined, q: filters.q || undefined, size: 15 })
      .then(setData), 250);
    return () => clearTimeout(t);
  }, [filters]);

  const toggle = async (u) => {
    try {
      const updated = await adminApi.setUserStatus(u.id, !u.enabled);
      setData({ ...data, content: data.content.map((x) => (x.id === u.id ? updated : x)) });
      toast.success(`${u.fullName} ${updated.enabled ? 'enabled' : 'disabled'}`);
    } catch (e) {
      toast.error(errorMessage(e));
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Users</h1>
        <p className="text-sm text-slate-500">Disabling an account revokes its sessions immediately.</p>
      </div>
      <div className="flex flex-wrap gap-3">
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input className="input w-64 pl-9" placeholder="Search name or email" value={filters.q}
            onChange={(e) => setFilters({ ...filters, q: e.target.value, page: 0 })} />
        </div>
        <select className="input w-44" value={filters.role} onChange={(e) => setFilters({ ...filters, role: e.target.value, page: 0 })}>
          <option value="">All roles</option>
          <option value="CANDIDATE">Candidates</option>
          <option value="RECRUITER">Recruiters</option>
          <option value="ADMIN">Admins</option>
        </select>
      </div>
      {!data ? <PageLoader /> : (
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b bg-slate-50 text-left text-xs uppercase text-slate-500">
              <tr><th className="px-5 py-3">User</th><th className="px-5 py-3">Role</th><th className="px-5 py-3">Company</th><th className="px-5 py-3">Joined</th><th className="px-5 py-3 text-right">Status</th></tr>
            </thead>
            <tbody className="divide-y">
              {data.content.map((u) => (
                <tr key={u.id}>
                  <td className="px-5 py-3"><div className="flex items-center gap-3"><Avatar name={u.fullName} className="h-8 w-8 text-xs" />
                    <div><p className="font-medium">{u.fullName}</p><p className="text-xs text-slate-500">{u.email}</p></div></div></td>
                  <td className="px-5 py-3"><span className="chip bg-slate-100 text-slate-700">{u.role.toLowerCase()}</span></td>
                  <td className="px-5 py-3 text-slate-600">{u.companyName || '—'}</td>
                  <td className="px-5 py-3 text-slate-500">{date(u.createdAt)}</td>
                  <td className="px-5 py-3 text-right">
                    <button disabled={u.id === me.id} onClick={() => toggle(u)}
                      className={`chip cursor-pointer disabled:cursor-not-allowed disabled:opacity-50 ${u.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
                      {u.enabled ? 'Active' : 'Disabled'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex items-center justify-between border-t px-5 py-3 text-sm text-slate-500">
            <span>{data.totalElements} users</span>
            <div className="flex gap-2">
              <button className="btn-secondary py-1" disabled={data.page === 0} onClick={() => setFilters({ ...filters, page: data.page - 1 })}>Prev</button>
              <button className="btn-secondary py-1" disabled={data.page + 1 >= data.totalPages} onClick={() => setFilters({ ...filters, page: data.page + 1 })}>Next</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
