import clsx from 'clsx';
import {
  BarChart3, Briefcase, FileSearch, KanbanSquare, LayoutDashboard, LogOut, Menu, Search, User, Users, X,
} from 'lucide-react';
import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import { Avatar } from './ui';

const NAV = {
  CANDIDATE: [
    { to: '/candidate', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/jobs', label: 'Find Jobs', icon: Search },
    { to: '/candidate/applications', label: 'My Applications', icon: KanbanSquare },
    { to: '/candidate/ats-checker', label: 'ATS Checker', icon: FileSearch },
    { to: '/candidate/profile', label: 'Profile & Resume', icon: User },
  ],
  RECRUITER: [
    { to: '/recruiter', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/recruiter/jobs', label: 'Jobs & Pipelines', icon: Briefcase },
  ],
  ADMIN: [
    { to: '/admin', label: 'Analytics', icon: BarChart3, end: true },
    { to: '/admin/users', label: 'Users', icon: Users },
    { to: '/admin/jobs', label: 'Job Moderation', icon: Briefcase },
  ],
};

export function Logo({ className }) {
  return (
    <Link to="/" className={clsx('flex items-center gap-2 font-bold', className)}>
      <img src="/logo.svg" alt="" className="h-8 w-8" />
      <span>TalentTrack <span className="text-brand-600">AI</span></span>
    </Link>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const items = NAV[user.role] || [];

  const nav = (
    <nav className="flex flex-col gap-1">
      {items.map(({ to, label, icon: Icon, end }) => (
        <NavLink key={to} to={to} end={end} onClick={() => setMobileOpen(false)}
          className={({ isActive }) => clsx('flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition',
            isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100')}>
          <Icon className="h-[18px] w-[18px]" />{label}
        </NavLink>
      ))}
    </nav>
  );

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-64 shrink-0 flex-col border-r bg-white p-4 lg:flex">
        <Logo className="mb-8 px-2 text-lg" />
        {nav}
        <div className="mt-auto rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          Signed in as <span className="font-semibold text-slate-700">{user.role.toLowerCase()}</span>
        </div>
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 bg-slate-900/40 lg:hidden" onClick={() => setMobileOpen(false)}>
          <aside className="h-full w-64 bg-white p-4" onClick={(e) => e.stopPropagation()}>
            <div className="mb-8 flex items-center justify-between">
              <Logo className="text-lg" />
              <button onClick={() => setMobileOpen(false)}><X className="h-5 w-5" /></button>
            </div>
            {nav}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b bg-white/80 px-4 backdrop-blur lg:px-8">
          <button className="btn-ghost p-2 lg:hidden" onClick={() => setMobileOpen(true)} aria-label="Menu">
            <Menu className="h-5 w-5" />
          </button>
          <div className="hidden text-sm text-slate-500 lg:block">
            {user.companyName ? `${user.companyName} workspace` : 'Welcome back'}
          </div>
          <div className="flex items-center gap-3">
            <NotificationBell />
            <div className="flex items-center gap-2">
              <Avatar name={user.fullName} />
              <div className="hidden text-sm sm:block">
                <p className="font-medium leading-tight">{user.fullName}</p>
                <p className="text-xs text-slate-500">{user.email}</p>
              </div>
            </div>
            <button onClick={logout} className="btn-ghost p-2" title="Log out" aria-label="Log out">
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </header>
        <main className="flex-1 p-4 lg:p-8"><Outlet /></main>
      </div>
    </div>
  );
}
