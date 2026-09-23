"""
Skill taxonomy and extraction.

Every canonical skill maps to the different ways people write it
("JS", "Javascript", "ECMAScript" -> "JavaScript"). Extraction scans text
with word-boundary regexes so "Java" does not match inside "JavaScript".
"""
import re
from functools import lru_cache

SKILL_ALIASES: dict[str, list[str]] = {
    # Languages
    "Java": ["java", "core java", "java 8", "java 11", "java 17", "java 21"],
    "Python": ["python", "python3"],
    "JavaScript": ["javascript", "js", "ecmascript", "es6"],
    "TypeScript": ["typescript", "ts"],
    "C++": ["c++", "cpp"],
    "C#": ["c#", "csharp"],
    "C": ["c programming", "ansi c"],
    "Go": ["golang"],
    "Kotlin": ["kotlin"],
    "Rust": ["rust"],
    "PHP": ["php"],
    "Ruby": ["ruby"],
    "Swift": ["swift"],
    "Scala": ["scala"],
    "SQL": ["sql"],
    "HTML": ["html", "html5"],
    "CSS": ["css", "css3"],
    "Bash": ["bash", "shell scripting", "shell script"],
    # Backend / frameworks
    "Spring Boot": ["spring boot", "springboot", "spring-boot"],
    "Spring": ["spring framework", "spring mvc", "spring"],
    "Spring Security": ["spring security"],
    "Hibernate": ["hibernate", "jpa", "spring data jpa"],
    "Microservices": ["microservices", "microservice", "micro-services"],
    "REST APIs": ["rest api", "rest apis", "restful", "restful api", "restful apis", "rest"],
    "GraphQL": ["graphql"],
    "Node.js": ["node.js", "nodejs", "node"],
    "Express": ["express", "express.js", "expressjs"],
    "Django": ["django"],
    "Flask": ["flask"],
    "FastAPI": ["fastapi"],
    ".NET": [".net", "dotnet", "asp.net"],
    "JWT": ["jwt", "json web token", "json web tokens"],
    "OAuth": ["oauth", "oauth2", "oauth 2.0"],
    "Kafka": ["kafka", "apache kafka"],
    "RabbitMQ": ["rabbitmq"],
    "Maven": ["maven"],
    "Gradle": ["gradle"],
    # Frontend
    "React": ["react", "react.js", "reactjs"],
    "Redux": ["redux", "redux toolkit"],
    "Next.js": ["next.js", "nextjs"],
    "Angular": ["angular", "angularjs"],
    "Vue": ["vue", "vue.js", "vuejs"],
    "Tailwind CSS": ["tailwind", "tailwind css", "tailwindcss"],
    "Bootstrap": ["bootstrap"],
    # Data stores
    "MySQL": ["mysql"],
    "PostgreSQL": ["postgresql", "postgres"],
    "MongoDB": ["mongodb", "mongo"],
    "Redis": ["redis"],
    "Oracle": ["oracle db", "oracle database", "pl/sql"],
    "Elasticsearch": ["elasticsearch", "elastic search"],
    # Cloud / DevOps
    "Docker": ["docker", "dockerfile", "docker compose", "docker-compose"],
    "Kubernetes": ["kubernetes", "k8s"],
    "AWS": ["aws", "amazon web services", "ec2", "s3", "lambda"],
    "Azure": ["azure", "microsoft azure"],
    "GCP": ["gcp", "google cloud"],
    "Terraform": ["terraform"],
    "Jenkins": ["jenkins"],
    "GitHub Actions": ["github actions"],
    "CI/CD": ["ci/cd", "cicd", "continuous integration", "continuous delivery", "continuous deployment"],
    "Linux": ["linux", "unix", "ubuntu"],
    "Nginx": ["nginx"],
    "Git": ["git", "github", "gitlab", "bitbucket"],
    # Testing
    "JUnit": ["junit", "junit5"],
    "Mockito": ["mockito"],
    "Jest": ["jest"],
    "Selenium": ["selenium"],
    "Unit Testing": ["unit testing", "unit tests", "tdd", "test driven development"],
    # Data / ML
    "Machine Learning": ["machine learning", "ml"],
    "Deep Learning": ["deep learning"],
    "NLP": ["nlp", "natural language processing"],
    "TensorFlow": ["tensorflow"],
    "PyTorch": ["pytorch"],
    "Scikit-learn": ["scikit-learn", "sklearn", "scikit learn"],
    "Pandas": ["pandas"],
    "NumPy": ["numpy"],
    "Data Analysis": ["data analysis", "data analytics"],
    "Statistics": ["statistics", "statistical analysis"],
    "Power BI": ["power bi", "powerbi"],
    "Tableau": ["tableau"],
    "Excel": ["excel", "ms excel", "advanced excel"],
    "Spark": ["spark", "apache spark", "pyspark"],
    # Practices & soft skills
    "Agile": ["agile", "scrum", "kanban"],
    "Jira": ["jira"],
    "System Design": ["system design", "low level design", "high level design"],
    "Data Structures": ["data structures", "dsa", "algorithms"],
    "OOP": ["oop", "object oriented programming", "object-oriented"],
    "Communication": ["communication", "communication skills"],
    "Leadership": ["leadership", "team lead", "led a team"],
    "Problem Solving": ["problem solving", "problem-solving"],
    "Figma": ["figma"],
}

# Aliases that are also common English words: only count them when written with
# their usual capitalisation in the original text.
CASE_SENSITIVE_ALIASES = {"rest": "REST", "node": "Node", "spring": "Spring", "go": "Go", "ts": "TS",
                          "ml": "ML", "express": "Express", "rust": "Rust", "swift": "Swift",
                          "communication": "Communication", "leadership": "Leadership"}


@lru_cache(maxsize=1)
def _alias_index() -> dict[str, str]:
    index: dict[str, str] = {}
    for canonical, aliases in SKILL_ALIASES.items():
        index[canonical.lower()] = canonical
        for alias in aliases:
            index[alias.lower()] = canonical
    return index


def _pattern(alias: str) -> str:
    # (?<![\w+#.]) / (?![\w+#]) act like \b but also treat '+', '#', '.' as part of a word
    return r"(?<![\w+#.])" + re.escape(alias) + r"(?![\w+#]|\.\w)"


@lru_cache(maxsize=1)
def _compiled() -> list[tuple[re.Pattern, str, bool]]:
    patterns = []
    for alias, canonical in sorted(_alias_index().items(), key=lambda kv: -len(kv[0])):
        if alias in CASE_SENSITIVE_ALIASES:
            patterns.append((re.compile(_pattern(CASE_SENSITIVE_ALIASES[alias])), canonical, True))
        else:
            patterns.append((re.compile(_pattern(alias), re.IGNORECASE), canonical, False))
    return patterns


def canonicalize(skill: str) -> str:
    """Map any spelling to its canonical name; unknown skills are returned trimmed."""
    cleaned = skill.strip()
    return _alias_index().get(cleaned.lower(), cleaned)


def extract_skills(text: str) -> list[str]:
    """Return canonical skills found in text, in order of first appearance."""
    if not text:
        return []
    found: dict[str, int] = {}
    for pattern, canonical, _ in _compiled():
        match = pattern.search(text)
        if match and (canonical not in found or match.start() < found[canonical]):
            found[canonical] = match.start()
    return [skill for skill, _ in sorted(found.items(), key=lambda kv: kv[1])]


def mentions(text: str, skill: str) -> bool:
    """True if text mentions the skill under any of its known aliases (or literally)."""
    if not text:
        return False
    canonical = canonicalize(skill)
    if canonical in extract_skills(text):
        return True
    return re.search(_pattern(skill.strip()), text, re.IGNORECASE) is not None
