import { ArrowRight, Brain, FileSearch, KanbanSquare, ShieldCheck, Sparkles, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { STAGES, STAGE_META } from '../utils/format';
import PublicHeader from './PublicHeader';

const FEATURES = [
  { icon: Brain, title: 'AI resume parsing', text: 'Upload a PDF/DOCX. Our NLP service extracts skills, experience and rates ATS readiness.' },
  { icon: FileSearch, title: 'ATS match scoring', text: 'Every application gets a skill, semantic (TF-IDF) and experience score against the job.' },
  { icon: KanbanSquare, title: 'Jira-style pipeline', text: 'Recruiters drag candidates across Applied → Screening → Interview → Offer → Hired.' },
  { icon: Sparkles, title: 'Smart recommendations', text: 'Candidates see jobs ranked by how well they match their resume.' },
  { icon: ShieldCheck, title: 'Secure by design', text: 'JWT access + rotating refresh tokens, BCrypt, role-based access control.' },
  { icon: Users, title: 'Three portals', text: 'Dedicated experiences for candidates, recruiters and platform admins.' },
];

export default function Landing() {
  return (
    <div className="min-h-screen bg-white">
      <PublicHeader />
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-b from-brand-50 via-white to-white" />
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-20 lg:grid-cols-2">
          <div>
            <span className="chip bg-brand-100 text-brand-700"><Sparkles className="mr-1 h-3 w-3" /> AI-powered hiring</span>
            <h1 className="mt-4 text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl">
              Find the right job. <br /><span className="text-brand-600">Hire the right talent.</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg text-slate-600">
              TalentTrack AI matches resumes to jobs & internships with explainable ATS scores, and gives recruiters a
              Kanban board to run their entire hiring pipeline.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/register" className="btn-primary px-6 py-3 text-base">Create free account <ArrowRight className="h-4 w-4" /></Link>
              <Link to="/jobs" className="btn-secondary px-6 py-3 text-base">Browse jobs</Link>
            </div>
            <p className="mt-6 text-sm text-slate-500">
              Demo: <code className="rounded bg-slate-100 px-1">candidate@talenttrack.ai / Candidate@123</code> ·{' '}
              <code className="rounded bg-slate-100 px-1">recruiter@talenttrack.ai / Recruiter@123</code>
            </p>
          </div>
          <div className="card overflow-hidden p-4 shadow-xl">
            <p className="mb-3 text-sm font-semibold text-slate-700">Java Backend Developer · Pipeline</p>
            <div className="grid grid-cols-3 gap-3">
              {STAGES.slice(0, 3).map((s, i) => (
                <div key={s} className={`rounded-lg border-t-4 bg-slate-50 p-2 ${STAGE_META[s].bar}`}>
                  <p className="mb-2 text-xs font-semibold text-slate-600">{STAGE_META[s].label}</p>
                  {Array.from({ length: 3 - i }).map((_, j) => (
                    <div key={j} className="mb-2 rounded-md bg-white p-2 shadow-sm">
                      <div className="mb-1 h-2 w-3/4 rounded bg-slate-200" />
                      <div className="flex items-center justify-between">
                        <div className="h-2 w-1/3 rounded bg-slate-100" />
                        <span className="chip bg-emerald-50 text-[10px] text-emerald-700">{92 - i * 9 - j * 7}%</span>
                      </div>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
      <section className="mx-auto max-w-6xl px-4 pb-24">
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, text }) => (
            <div key={title} className="card p-6">
              <div className="mb-3 inline-flex rounded-lg bg-brand-50 p-2.5 text-brand-600"><Icon className="h-5 w-5" /></div>
              <h3 className="font-semibold">{title}</h3>
              <p className="mt-1 text-sm text-slate-600">{text}</p>
            </div>
          ))}
        </div>
      </section>
      <footer className="border-t py-6 text-center text-sm text-slate-500">
        TalentTrack AI · Spring Boot · React · FastAPI · MySQL · Docker
      </footer>
    </div>
  );
}
