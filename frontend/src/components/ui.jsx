import clsx from 'clsx';
import { Loader2, X } from 'lucide-react';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { STAGE_META, initials, scoreColor } from '../utils/format';

export function Spinner({ className }) {
  return <Loader2 className={clsx('animate-spin text-brand-600', className || 'h-6 w-6')} />;
}

export function PageLoader() {
  return (
    <div className="flex h-64 items-center justify-center">
      <Spinner className="h-8 w-8" />
    </div>
  );
}

export function StageBadge({ stage }) {
  const meta = STAGE_META[stage] || STAGE_META.APPLIED;
  return <span className={clsx('chip', meta.color)}>{meta.label}</span>;
}

export function SkillChip({ children, tone = 'default' }) {
  const tones = {
    default: 'bg-slate-100 text-slate-700',
    match: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200',
    missing: 'bg-rose-50 text-rose-700 ring-1 ring-rose-200',
    brand: 'bg-brand-50 text-brand-700',
  };
  return <span className={clsx('chip', tones[tone])}>{children}</span>;
}

export function ScoreRing({ score = 0, size = 64, stroke = 6, label }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const { text, stroke: color } = scoreColor(score);
  return (
    <div className="relative inline-flex flex-col items-center" style={{ width: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} stroke="#e2e8f0" strokeWidth={stroke} fill="none" />
        <circle cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth={stroke} fill="none"
          strokeDasharray={c} strokeDashoffset={c - (c * Math.min(score, 100)) / 100} strokeLinecap="round"
          style={{ transition: 'stroke-dashoffset .6s ease' }} />
      </svg>
      <span className={clsx('absolute font-bold', text)} style={{ top: size / 2 - 10, fontSize: size / 4 }}>
        {score}
      </span>
      {label && <span className="mt-1 text-xs text-slate-500">{label}</span>}
    </div>
  );
}

export function ScoreBar({ label, value }) {
  const { stroke } = scoreColor(value);
  return (
    <div>
      <div className="mb-1 flex justify-between text-xs">
        <span className="text-slate-600">{label}</span>
        <span className="font-semibold">{value}%</span>
      </div>
      <div className="h-2 rounded-full bg-slate-100">
        <div className="h-2 rounded-full transition-all" style={{ width: `${value}%`, background: stroke }} />
      </div>
    </div>
  );
}

export function Avatar({ name, className }) {
  return (
    <div className={clsx('flex shrink-0 items-center justify-center rounded-full bg-brand-100 font-semibold text-brand-700',
      className || 'h-9 w-9 text-sm')}>
      {initials(name)}
    </div>
  );
}

export function StatCard({ icon: Icon, label, value, hint, tone = 'brand' }) {
  const tones = {
    brand: 'bg-brand-50 text-brand-600', green: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600', rose: 'bg-rose-50 text-rose-600', sky: 'bg-sky-50 text-sky-600',
  };
  return (
    <div className="card flex items-center gap-4 p-5">
      <div className={clsx('rounded-xl p-3', tones[tone])}><Icon className="h-6 w-6" /></div>
      <div>
        <p className="text-sm text-slate-500">{label}</p>
        <p className="text-2xl font-bold text-slate-900">{value}</p>
        {hint && <p className="text-xs text-slate-400">{hint}</p>}
      </div>
    </div>
  );
}

export function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <div className="card flex flex-col items-center justify-center px-6 py-14 text-center">
      {Icon && <Icon className="mb-3 h-10 w-10 text-slate-300" />}
      <h3 className="font-semibold text-slate-800">{title}</h3>
      {text && <p className="mt-1 max-w-md text-sm text-slate-500">{text}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

function useEscape(onClose) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
}

export function Modal({ open, onClose, title, children, wide }) {
  useEscape(onClose);
  if (!open) return null;
  // Portal to <body> so overlays escape parent layout styles (e.g. space-y margins)
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 p-4 pt-16"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={clsx('card w-full p-6', wide ? 'max-w-3xl' : 'max-w-lg')}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button className="btn-ghost p-1" onClick={onClose} aria-label="Close"><X className="h-5 w-5" /></button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  );
}

export function Drawer({ open, onClose, title, children }) {
  useEscape(onClose);
  if (!open) return null;
  return createPortal(
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-900/40" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="flex h-full w-full max-w-2xl flex-col bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button className="btn-ghost p-1" onClick={onClose} aria-label="Close"><X className="h-5 w-5" /></button>
        </div>
        <div className="flex-1 overflow-y-auto p-6">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

export function SkillInput({ value = [], onChange, placeholder = 'Type a skill and press Enter' }) {
  const add = (raw) => {
    const s = raw.trim().replace(/,$/, '');
    if (s && !value.some((v) => v.toLowerCase() === s.toLowerCase())) onChange([...value, s]);
  };
  return (
    <div className="input flex flex-wrap gap-1.5">
      {value.map((s) => (
        <span key={s} className="chip gap-1 bg-brand-50 text-brand-700">
          {s}
          <button type="button" onClick={() => onChange(value.filter((v) => v !== s))} aria-label={`Remove ${s}`}>
            <X className="h-3 w-3" />
          </button>
        </span>
      ))}
      <input className="min-w-[10rem] flex-1 border-none p-0 text-sm focus:outline-none focus:ring-0" placeholder={placeholder}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            add(e.currentTarget.value);
            e.currentTarget.value = '';
          } else if (e.key === 'Backspace' && !e.currentTarget.value && value.length) {
            onChange(value.slice(0, -1));
          }
        }}
        onBlur={(e) => { add(e.currentTarget.value); e.currentTarget.value = ''; }} />
    </div>
  );
}
