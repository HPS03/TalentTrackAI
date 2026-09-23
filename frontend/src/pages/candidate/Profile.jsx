import { Eye, FileUp, Lightbulb, UploadCloud } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../api/client';
import { candidateApi, openBlob } from '../../api/services';
import { PageLoader, ScoreRing, SkillChip, SkillInput } from '../../components/ui';
import { date } from '../../utils/format';

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const fileRef = useRef(null);

  const load = (p) => {
    setProfile(p);
    setForm({
      headline: p.headline || '', bio: p.bio || '', phone: p.phone || '', location: p.location || '',
      experienceYears: p.experienceYears || 0, education: p.education || '', skills: p.skills || [],
      linkedinUrl: p.linkedinUrl || '', githubUrl: p.githubUrl || '', portfolioUrl: p.portfolioUrl || '',
    });
  };

  useEffect(() => { candidateApi.profile().then(load); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      load(await candidateApi.updateProfile({ ...form, experienceYears: Number(form.experienceYears) }));
      toast.success('Profile saved');
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const upload = async (file) => {
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) return toast.error('File must be under 5 MB');
    setUploading(true);
    try {
      const res = await candidateApi.uploadResume(file);
      load(res.profile);
      setAnalysis(res);
      toast.success(res.extractedSkills.length ? `Resume parsed: ${res.extractedSkills.length} skills found` : 'Resume uploaded');
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  if (!form) return <PageLoader />;

  return (
    <div className="grid gap-6 xl:grid-cols-3">
      <div className="space-y-6 xl:col-span-2">
        <div>
          <h1 className="page-title">Profile & Resume</h1>
          <p className="text-sm text-slate-500">Recruiters see this profile when you apply. Skills drive your AI match score.</p>
        </div>
        <form onSubmit={save} className="card grid gap-4 p-6 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="label">Headline</label>
            <input className="input" value={form.headline} onChange={set('headline')} placeholder="Java Developer | Spring Boot | React" />
          </div>
          <div className="sm:col-span-2">
            <label className="label">About you</label>
            <textarea className="input min-h-[100px]" value={form.bio} onChange={set('bio')} />
          </div>
          <div><label className="label">Phone</label><input className="input" value={form.phone} onChange={set('phone')} /></div>
          <div><label className="label">Location</label><input className="input" value={form.location} onChange={set('location')} /></div>
          <div><label className="label">Years of experience</label>
            <input type="number" min="0" max="50" className="input" value={form.experienceYears} onChange={set('experienceYears')} /></div>
          <div><label className="label">Education</label><input className="input" value={form.education} onChange={set('education')} placeholder="B.Tech CSE, 2025" /></div>
          <div className="sm:col-span-2">
            <label className="label">Skills</label>
            <SkillInput value={form.skills} onChange={(skills) => setForm({ ...form, skills })} />
          </div>
          <div><label className="label">LinkedIn</label><input className="input" value={form.linkedinUrl} onChange={set('linkedinUrl')} /></div>
          <div><label className="label">GitHub</label><input className="input" value={form.githubUrl} onChange={set('githubUrl')} /></div>
          <div className="sm:col-span-2"><label className="label">Portfolio</label><input className="input" value={form.portfolioUrl} onChange={set('portfolioUrl')} /></div>
          <div className="flex justify-end sm:col-span-2">
            <button className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</button>
          </div>
        </form>
      </div>

      <div className="space-y-6">
        <div className="card p-6 text-center">
          <ScoreRing score={profile.profileCompleteness} size={96} label="Profile strength" />
        </div>
        <div className="card p-6">
          <h2 className="mb-1 font-semibold">Resume</h2>
          <p className="mb-4 text-sm text-slate-500">PDF or DOCX, max 5 MB. Our AI extracts your skills automatically.</p>
          <label className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 text-center transition hover:border-brand-400 hover:bg-brand-50/50 ${uploading ? 'pointer-events-none opacity-60' : ''}`}
            onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); upload(e.dataTransfer.files[0]); }}>
            <UploadCloud className="mb-2 h-8 w-8 text-brand-500" />
            <span className="text-sm font-medium">{uploading ? 'Analysing resume…' : 'Drop file or click to upload'}</span>
            <input ref={fileRef} type="file" accept=".pdf,.docx" className="hidden" onChange={(e) => upload(e.target.files[0])} />
          </label>
          {profile.resumeFileName && (
            <div className="mt-4 flex items-center justify-between rounded-lg bg-slate-50 p-3 text-sm">
              <div className="flex min-w-0 items-center gap-2">
                <FileUp className="h-4 w-4 shrink-0 text-slate-500" />
                <div className="min-w-0">
                  <p className="truncate font-medium">{profile.resumeFileName}</p>
                  <p className="text-xs text-slate-500">Uploaded {date(profile.resumeUploadedAt)}</p>
                </div>
              </div>
              <button className="btn-ghost p-2" title="View" onClick={() => candidateApi.resumeBlob().then(openBlob)}>
                <Eye className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>

        {analysis && (
          <div className="card space-y-4 p-6">
            <div className="flex items-center gap-4">
              <ScoreRing score={analysis.quality.score} size={64} />
              <div>
                <p className="font-semibold">Resume ATS readiness</p>
                <p className="text-xs text-slate-500">Sections: {analysis.quality.sectionsFound.join(', ') || 'none detected'}</p>
              </div>
            </div>
            {analysis.extractedSkills.length > 0 && (
              <div>
                <p className="mb-2 text-sm font-medium">Skills detected</p>
                <div className="flex flex-wrap gap-1.5">{analysis.extractedSkills.map((s) => <SkillChip key={s} tone="match">{s}</SkillChip>)}</div>
              </div>
            )}
            {analysis.quality.tips.length > 0 && (
              <div>
                <p className="mb-2 flex items-center gap-1 text-sm font-medium"><Lightbulb className="h-4 w-4 text-amber-500" /> Suggestions</p>
                <ul className="list-disc space-y-1 pl-5 text-sm text-slate-600">
                  {analysis.quality.tips.map((t) => <li key={t}>{t}</li>)}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
