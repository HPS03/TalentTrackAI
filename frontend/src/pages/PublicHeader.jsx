import { Link } from 'react-router-dom';
import { Logo } from '../components/Layout';

export default function PublicHeader() {
  return (
    <header className="sticky top-0 z-30 border-b bg-white/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Logo className="text-lg" />
        <nav className="flex items-center gap-2">
          <Link to="/jobs" className="btn-ghost">Browse jobs</Link>
          <Link to="/login" className="btn-secondary">Log in</Link>
          <Link to="/register" className="btn-primary hidden sm:inline-flex">Get started</Link>
        </nav>
      </div>
    </header>
  );
}
