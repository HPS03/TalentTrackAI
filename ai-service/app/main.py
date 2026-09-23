"""TalentTrack AI - resume parsing, ATS scoring and job recommendation microservice."""
import logging
import os

from fastapi import FastAPI, File, HTTPException, UploadFile

from . import matcher
from .resume_parser import UnsupportedFileError, parse_resume
from .schemas import (ExtractRequest, ExtractResponse, MatchRequest, MatchResult, ParsedResume,
                      RecommendRequest, RecommendResponse)
from .skills import extract_skills

MAX_UPLOAD_BYTES = int(os.getenv("MAX_UPLOAD_MB", "5")) * 1024 * 1024

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
log = logging.getLogger("ai-service")

app = FastAPI(title="TalentTrack AI Service", version="1.0.0",
              description="NLP resume parsing, ATS scoring and job recommendations")


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.post("/api/v1/resume/parse", response_model=ParsedResume)
async def parse(file: UploadFile = File(...)) -> ParsedResume:
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty file")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="File too large")
    try:
        result = parse_resume(content, file.filename or "")
    except UnsupportedFileError as exc:
        raise HTTPException(status_code=415, detail=str(exc)) from exc
    log.info("Parsed %s: %d skills, quality %d", file.filename, len(result["skills"]), result["quality_score"])
    return ParsedResume(**result)


@app.post("/api/v1/match", response_model=MatchResult)
def match(req: MatchRequest) -> MatchResult:
    return MatchResult(**matcher.match(req.resume_text, req.candidate_skills, req.candidate_experience,
                                       req.job_description, req.job_skills, req.min_experience))


@app.post("/api/v1/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest) -> RecommendResponse:
    results = matcher.recommend(req.candidate_text, req.candidate_skills, req.candidate_experience,
                                [j.model_dump() for j in req.jobs], req.top_k)
    return RecommendResponse(results=results)


@app.post("/api/v1/skills/extract", response_model=ExtractResponse)
def extract(req: ExtractRequest) -> ExtractResponse:
    return ExtractResponse(skills=extract_skills(req.text))
