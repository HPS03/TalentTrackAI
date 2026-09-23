import io

from docx import Document
from fastapi.testclient import TestClient

from app.main import app
from app.skills import extract_skills

client = TestClient(app)

RESUME = """Rahul Verma
rahul@example.com | +91 98765 43210

Summary
Backend developer with 3 years of experience building Java and Spring Boot microservices.

Experience
Software Engineer, NimbusTech (2021 - Present)
- Built REST APIs with Spring Boot and MySQL serving 50k users
- Reduced API latency by 40% using Redis caching
- Deployed services with Docker and Jenkins CI/CD

Education
B.Tech Computer Science, 2021

Skills
Java, Spring Boot, Hibernate, MySQL, Redis, Docker, Git, JUnit, React.js

Projects
TalentTrack - job portal built with React and Spring Security (JWT)
"""

JD = ("We need a Java developer to design REST APIs using Spring Boot, MySQL and Docker. "
      "Experience with Kubernetes and AWS is a plus.")


def make_docx(text: str) -> bytes:
    doc = Document()
    for line in text.splitlines():
        doc.add_paragraph(line)
    buf = io.BytesIO()
    doc.save(buf)
    return buf.getvalue()


def test_health():
    assert client.get("/health").json() == {"status": "UP"}


def test_skill_extraction_uses_aliases_and_word_boundaries():
    skills = extract_skills("Worked with JavaScript, ReactJS, k8s, Postgres and C++ daily")
    assert {"JavaScript", "React", "Kubernetes", "PostgreSQL", "C++"} <= set(skills)
    assert "Java" not in skills  # must not match inside "JavaScript"


def test_common_words_are_not_skills_unless_capitalised():
    assert "REST APIs" not in extract_skills("please take a rest after lunch")
    assert "REST APIs" in extract_skills("Designed REST endpoints")


def test_parse_docx_resume():
    files = {"file": ("resume.docx", make_docx(RESUME),
                      "application/vnd.openxmlformats-officedocument.wordprocessingml.document")}
    res = client.post("/api/v1/resume/parse", files=files)
    assert res.status_code == 200
    body = res.json()
    assert {"Java", "Spring Boot", "MySQL", "Docker", "Redis", "React", "JWT"} <= set(body["skills"])
    assert body["experience_years"] == 3
    assert body["email"] == "rahul@example.com"
    assert {"Experience", "Education", "Skills", "Projects", "Summary"} <= set(body["sections_found"])
    assert 0 < body["quality_score"] <= 100


def test_parse_rejects_unsupported_files():
    res = client.post("/api/v1/resume/parse", files={"file": ("photo.png", b"\x89PNG....", "image/png")})
    assert res.status_code == 415


def test_match_scores_relevant_resume_higher():
    good = client.post("/api/v1/match", json={
        "resume_text": RESUME, "candidate_skills": ["Java", "Spring Boot"], "candidate_experience": 3,
        "job_description": JD, "job_skills": ["Java", "Spring Boot", "MySQL", "Docker", "Kubernetes"],
        "min_experience": 2}).json()
    bad = client.post("/api/v1/match", json={
        "resume_text": "Graphic designer skilled in Photoshop and Illustrator.", "candidate_skills": ["Photoshop"],
        "candidate_experience": 0, "job_description": JD,
        "job_skills": ["Java", "Spring Boot", "MySQL", "Docker", "Kubernetes"], "min_experience": 2}).json()

    assert good["overall"] > bad["overall"] + 30
    assert good["matched_skills"] == ["Java", "Spring Boot", "MySQL", "Docker"]
    assert good["missing_skills"] == ["Kubernetes"]
    assert good["skill_score"] == 80
    assert "Kubernetes" in good["summary"]
    assert bad["experience_score"] == 0


def test_match_extracts_skills_from_description_when_none_given():
    res = client.post("/api/v1/match", json={"resume_text": RESUME, "job_description": JD}).json()
    assert "Kubernetes" in res["missing_skills"]
    assert "Java" in res["matched_skills"]


def test_recommend_ranks_best_fitting_job_first():
    jobs = [
        {"id": 1, "text": "Data analyst with Excel, Power BI and statistics", "skills": ["Excel", "Power BI"]},
        {"id": 2, "text": "Java Spring Boot backend engineer building REST APIs on MySQL",
         "skills": ["Java", "Spring Boot", "MySQL"], "min_experience": 1},
        {"id": 3, "text": "iOS developer using Swift", "skills": ["Swift"]},
    ]
    res = client.post("/api/v1/recommend", json={
        "candidate_text": RESUME, "candidate_skills": ["Java", "Spring Boot", "MySQL"],
        "candidate_experience": 3, "jobs": jobs, "top_k": 2}).json()["results"]
    assert len(res) == 2
    assert res[0]["job_id"] == 2
    assert res[0]["score"] > res[1]["score"]
