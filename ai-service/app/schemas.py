from pydantic import BaseModel, Field


class ParsedResume(BaseModel):
    text: str
    skills: list[str]
    experience_years: int | None = None
    email: str | None = None
    phone: str | None = None
    sections_found: list[str]
    quality_score: int
    tips: list[str]


class MatchRequest(BaseModel):
    resume_text: str = ""
    candidate_skills: list[str] = Field(default_factory=list)
    candidate_experience: int = 0
    job_description: str
    job_skills: list[str] = Field(default_factory=list)
    min_experience: int = 0


class MatchResult(BaseModel):
    overall: int
    skill_score: int
    semantic_score: int
    experience_score: int
    matched_skills: list[str]
    missing_skills: list[str]
    summary: str


class JobDocument(BaseModel):
    id: int
    text: str
    skills: list[str] = Field(default_factory=list)
    min_experience: int = 0


class RecommendRequest(BaseModel):
    candidate_text: str = ""
    candidate_skills: list[str] = Field(default_factory=list)
    candidate_experience: int = 0
    jobs: list[JobDocument]
    top_k: int = Field(default=6, ge=1, le=50)


class Recommendation(BaseModel):
    job_id: int
    score: int
    matched_skills: list[str]
    missing_skills: list[str]


class RecommendResponse(BaseModel):
    results: list[Recommendation]


class ExtractRequest(BaseModel):
    text: str


class ExtractResponse(BaseModel):
    skills: list[str]
