"""
ATS scoring and job recommendation.

overall ATS score = 50% skill match + 30% semantic (TF-IDF cosine) similarity + 20% experience fit
"""
import math

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from .skills import canonicalize, extract_skills, mentions

SKILL_WEIGHT, SEMANTIC_WEIGHT, EXPERIENCE_WEIGHT = 0.5, 0.3, 0.2


def _vectorizer(use_idf: bool) -> TfidfVectorizer:
    return TfidfVectorizer(stop_words="english", ngram_range=(1, 2), sublinear_tf=True, use_idf=use_idf,
                           token_pattern=r"(?u)\b[\w+#.]{2,}\b")


def _calibrate(cosine: float) -> int:
    """Raw cosine similarity of real documents rarely exceeds 0.5, so stretch it onto 0-100."""
    return round(100 * (1 - math.exp(-5 * max(cosine, 0.0))))


def semantic_similarity(a: str, b: str) -> int:
    if not a.strip() or not b.strip():
        return 0
    try:
        matrix = _vectorizer(use_idf=False).fit_transform([a, b])
    except ValueError:  # only stop words
        return 0
    return _calibrate(float(cosine_similarity(matrix[0], matrix[1])[0][0]))


def experience_score(candidate_years: int, required_years: int) -> int:
    if required_years <= 0 or candidate_years >= required_years:
        return 100
    return round(100 * candidate_years / required_years)


def skill_match(candidate_skills: list[str], resume_text: str, required: list[str]) -> tuple[int, list[str], list[str]]:
    have = {canonicalize(s).lower() for s in candidate_skills}
    matched, missing = [], []
    for skill in required:
        if canonicalize(skill).lower() in have or mentions(resume_text, skill):
            matched.append(skill)
        else:
            missing.append(skill)
    score = round(100 * len(matched) / len(required)) if required else 50
    return score, matched, missing


def _summary(overall: int, matched: list[str], missing: list[str], exp: int) -> str:
    total = len(matched) + len(missing)
    if overall >= 80:
        verdict = "Excellent match"
    elif overall >= 65:
        verdict = "Good match"
    elif overall >= 45:
        verdict = "Partial match"
    else:
        verdict = "Low match"
    parts = [f"{verdict}: {len(matched)}/{total} required skills found."]
    if missing:
        parts.append("Consider building or highlighting: " + ", ".join(missing[:5]) + ".")
    if exp < 100:
        parts.append("Experience is below the stated requirement.")
    return " ".join(parts)


def match(resume_text: str, candidate_skills: list[str], candidate_experience: int, job_description: str,
          job_skills: list[str], min_experience: int) -> dict:
    required = [s for s in job_skills if s.strip()] or extract_skills(job_description)
    candidate_blob = resume_text + "\n" + " ".join(candidate_skills)

    skill_score, matched, missing = skill_match(candidate_skills, resume_text, required)
    semantic = semantic_similarity(candidate_blob, job_description + "\n" + " ".join(required))
    exp = experience_score(candidate_experience, min_experience)
    overall = round(SKILL_WEIGHT * skill_score + SEMANTIC_WEIGHT * semantic + EXPERIENCE_WEIGHT * exp)
    return {
        "overall": overall,
        "skill_score": skill_score,
        "semantic_score": semantic,
        "experience_score": exp,
        "matched_skills": matched,
        "missing_skills": missing,
        "summary": _summary(overall, matched, missing, exp),
    }


def recommend(candidate_text: str, candidate_skills: list[str], candidate_experience: int, jobs: list[dict],
              top_k: int) -> list[dict]:
    """Rank jobs for a candidate. IDF is fitted on the job corpus so rare, specific terms weigh more."""
    if not jobs:
        return []
    candidate_blob = (candidate_text + "\n" + " ".join(candidate_skills)).strip()
    job_texts = [j["text"] + "\n" + " ".join(j["skills"]) for j in jobs]
    semantic = [0.0] * len(jobs)
    if candidate_blob:
        try:
            matrix = _vectorizer(use_idf=True).fit_transform(job_texts + [candidate_blob])
            semantic = cosine_similarity(matrix[-1], matrix[:-1])[0].tolist()
        except ValueError:
            pass

    results = []
    for job, cos in zip(jobs, semantic):
        skill_score, matched, missing = skill_match(candidate_skills, candidate_text, job["skills"])
        exp = experience_score(candidate_experience, job.get("min_experience", 0))
        score = round(0.5 * skill_score + 0.35 * _calibrate(cos) + 0.15 * exp)
        results.append({"job_id": job["id"], "score": score, "matched_skills": matched, "missing_skills": missing})
    results.sort(key=lambda r: r["score"], reverse=True)
    return results[:top_k]
