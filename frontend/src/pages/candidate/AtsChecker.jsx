import { FileSearch, Lightbulb } from 'lucide-react';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../../api/client';
import { candidateApi } from '../../api/services';
import { ScoreBar, ScoreRing, SkillChip, SkillInput } from '../../components/ui';

export default function AtsChecker() {
  const [jd, setJd] = useState('');
  const [skills, setSkills] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const run = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      setResult(await candidateApi.atsCheck({ jobDescription: jd, requiredSkills: skills }));
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">ATS Resume Checker</h1>
        <p className="text-sm text-slate-500">Paste any job description to see how your current resume & profile would score — before you apply.</p>
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={run} className="card space-y-4 p-6">
          <div>
            <label className="label">Job description</label>
            <textarea className="input min-h-[260px]" required minLength={30} value={jd} onChange={(e) => setJd(e.target.value)}
              placeholder="Paste the full job description here…" />
          </div>
          <div>
            <label className="label">Required skills (optional — auto-detected if empty)</label>
            <SkillInput value={skills} onChange={setSkills} />
          </div>
          <button className="btn-primary w-full" disabled={loading}><FileSearch className="h-4 w-4" />{loading ? 'Analysing…' : 'Check my score'}</button>
        </form>

        <div className="card p-6">
          {!result ? (
            <div className="flex h-full flex-col items-center justify-center text-center text-slate-400">
              <FileSearch className="mb-3 h-12 w-12" />
              <p>Your ATS report will appear here</p>
            </div>
          ) : (
            <div className="space-y-5">
              <div className="flex items-center gap-5">
                <ScoreRing score={result.overall} size={96} stroke={8} />
                <div>
                  <p className="text-lg font-semibold">Overall ATS score</p>
                  <p className="text-sm text-slate-500">{result.summary}</p>
                </div>
              </div>
              <ScoreBar label="Skill match (50%)" value={result.skillScore} />
              <ScoreBar label="Semantic similarity (30%)" value={result.semanticScore} />
              <ScoreBar label="Experience fit (20%)" value={result.experienceScore} />
              <div>
                <p className="mb-2 text-sm font-semibold">Matched</p>
                <div className="flex flex-wrap gap-1.5">{result.matchedSkills.map((s) => <SkillChip key={s} tone="match">{s}</SkillChip>)}</div>
              </div>
              {result.missingSkills.length > 0 && (
                <div className="rounded-lg bg-amber-50 p-4">
                  <p className="mb-2 flex items-center gap-1 text-sm font-semibold text-amber-800"><Lightbulb className="h-4 w-4" /> Add these to your resume if you have them</p>
                  <div className="flex flex-wrap gap-1.5">{result.missingSkills.map((s) => <SkillChip key={s} tone="missing">{s}</SkillChip>)}</div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
