import { Bell } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { notificationApi } from '../api/services';
import { timeAgo } from '../utils/format';

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);
  const ref = useRef(null);
  const navigate = useNavigate();

  const refreshCount = useCallback(() => notificationApi.unreadCount().then((r) => setCount(r.count)).catch(() => {}), []);

  useEffect(() => {
    refreshCount();
    const id = setInterval(refreshCount, 30_000); // lightweight polling
    return () => clearInterval(id);
  }, [refreshCount]);

  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const toggle = async () => {
    if (!open) setItems(await notificationApi.list().catch(() => []));
    setOpen(!open);
  };

  const openItem = async (n) => {
    if (!n.read) await notificationApi.markRead(n.id).catch(() => {});
    setOpen(false);
    refreshCount();
    if (n.link) navigate(n.link);
  };

  const readAll = async () => {
    await notificationApi.markAllRead();
    setItems(items.map((i) => ({ ...i, read: true })));
    setCount(0);
  };

  return (
    <div className="relative" ref={ref}>
      <button onClick={toggle} className="btn-ghost relative p-2" aria-label="Notifications">
        <Bell className="h-5 w-5" />
        {count > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
            {count > 9 ? '9+' : count}
          </span>
        )}
      </button>
      {open && (
        <div className="card absolute right-0 z-40 mt-2 w-80 overflow-hidden">
          <div className="flex items-center justify-between border-b px-4 py-3">
            <span className="font-semibold">Notifications</span>
            {count > 0 && <button onClick={readAll} className="text-xs font-medium text-brand-600">Mark all read</button>}
          </div>
          <div className="max-h-96 overflow-y-auto">
            {items.length === 0 && <p className="p-6 text-center text-sm text-slate-500">You're all caught up</p>}
            {items.map((n) => (
              <button key={n.id} onClick={() => openItem(n)}
                className={`block w-full border-b px-4 py-3 text-left hover:bg-slate-50 ${n.read ? '' : 'bg-brand-50/50'}`}>
                <p className="text-sm font-medium text-slate-800">{n.title}</p>
                <p className="text-xs text-slate-500">{n.message}</p>
                <p className="mt-1 text-[11px] text-slate-400">{timeAgo(n.createdAt)}</p>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
