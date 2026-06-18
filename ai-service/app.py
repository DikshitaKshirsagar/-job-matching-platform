import os
import hashlib
import json
import time
import threading
import re
from io import BytesIO

from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer, util
from keybert import KeyBERT
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

# PDF parsing with PyMuPDF
try:
    import fitz  # PyMuPDF
    HAS_PDF = True
except ImportError:
    HAS_PDF = False

# DOCX parsing
try:
    from docx import Document as DocxDocument
    HAS_DOCX = True
except ImportError:
    HAS_DOCX = False

# Prometheus metrics
try:
    from prometheus_flask_exporter import PrometheusMetrics
    HAS_METRICS = True
except ImportError:
    HAS_METRICS = False

app = Flask(__name__)

API_KEY = os.environ.get("AI_SERVICE_API_KEY", "")

# ---------------------------------------------------------------------------
# Model loading - supports fine-tuned models via MODEL_PATH env variable
# ---------------------------------------------------------------------------
DEFAULT_MODEL = os.environ.get("DEFAULT_MODEL", "all-MiniLM-L6-v2")
FINETUNED_MODEL_PATH = os.environ.get("FINETUNED_MODEL_PATH", "")

if FINETUNED_MODEL_PATH and os.path.exists(FINETUNED_MODEL_PATH):
    model = SentenceTransformer(FINETUNED_MODEL_PATH)
    print(f"Loaded fine-tuned model from {FINETUNED_MODEL_PATH}")
else:
    model = SentenceTransformer(DEFAULT_MODEL)
    print(f"Loaded default model: {DEFAULT_MODEL}")

kw_model = KeyBERT(model=model)

# ---------------------------------------------------------------------------
# Embedding cache: LRU using a simple dict with MD5 hash keys
# ---------------------------------------------------------------------------
_EMBEDDING_CACHE: dict = {}
_MAX_CACHE_SIZE = 1000
_cache_lock = threading.Lock()

# ---------------------------------------------------------------------------
# Metrics tracking for model monitoring
# ---------------------------------------------------------------------------
_model_metrics = {
    "total_predictions": 0,
    "avg_score": 0.0,
    "score_distribution": {"0-20": 0, "21-40": 0, "41-60": 0, "61-80": 0, "81-100": 0},
    "model_name": FINETUNED_MODEL_PATH if FINETUNED_MODEL_PATH else DEFAULT_MODEL,
    "drift_warnings": 0,
}

if HAS_METRICS:
    metrics = PrometheusMetrics(app)
    metrics.info("ai_service_info", "AI Service Info", version="2.0.0")


def encode(text: str):
    """Return embedding for text, using an LRU cache keyed by MD5 hash."""
    h = hashlib.md5(text.encode("utf-8")).hexdigest()
    if h not in _EMBEDDING_CACHE:
        with _cache_lock:
            if h not in _EMBEDDING_CACHE:
                _EMBEDDING_CACHE[h] = model.encode(text, convert_to_tensor=True)
                if len(_EMBEDDING_CACHE) > _MAX_CACHE_SIZE:
                    _EMBEDDING_CACHE.pop(next(iter(_EMBEDDING_CACHE)))
    return _EMBEDDING_CACHE[h]


# ---------------------------------------------------------------------------
# PDF / DOCX / TXT Parsing
# ---------------------------------------------------------------------------
def extract_text_from_pdf(file_bytes: bytes) -> str:
    """Extract text from a PDF file using PyMuPDF."""
    if not HAS_PDF:
        raise RuntimeError("PyMuPDF (fitz) is not installed")
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    text_parts = []
    for page in doc:
        text_parts.append(page.get_text())
    doc.close()
    return "\n".join(text_parts)


def extract_text_from_docx(file_bytes: bytes) -> str:
    """Extract text from a DOCX file."""
    if not HAS_DOCX:
        raise RuntimeError("python-docx is not installed")
    doc = DocxDocument(BytesIO(file_bytes))
    return "\n".join([p.text for p in doc.paragraphs if p.text.strip()])


def extract_text_from_txt(file_bytes: bytes) -> str:
    """Extract text from a TXT file."""
    return file_bytes.decode("utf-8", errors="ignore")


# ---------------------------------------------------------------------------
# NLP skill extraction using KeyBERT with enhanced skill set
# ---------------------------------------------------------------------------
TECHNICAL_SKILLS = {
    "python", "java", "javascript", "typescript", "react", "angular", "vue",
    "spring", "django", "flask", "node", "express", "sql", "mysql", "postgresql",
    "mongodb", "redis", "docker", "kubernetes", "aws", "azure", "gcp",
    "git", "jenkins", "terraform", "ansible", "puppet", "chef",
    "machine learning", "deep learning", "nlp", "computer vision", "tensorflow",
    "pytorch", "scikit-learn", "pandas", "numpy", "spark", "hadoop",
    "kafka", "rabbitmq", "elasticsearch", "logstash", "kibana",
    "ci/cd", "devops", "agile", "scrum", "jira", "confluence",
    "rest api", "graphql", "grpc", "webpack", "babel", "eslint",
    "jest", "mocha", "chai", "cypress", "selenium", "junit",
    "c++", "c#", "go", "rust", "swift", "kotlin", "ruby", "php",
    "html", "css", "sass", "less", "bootstrap", "tailwind",
    "microservices", "serverless", "lambda", "ec2", "s3", "rds",
    "oauth", "jwt", "ssl/tls", "owasp", "xss", "csrf", "sql injection",
}

SKILL_SYNONYMS = {
    "js": "javascript", "ts": "typescript", "py": "python",
    "reactjs": "react", "react.js": "react",
    "nodejs": "node", "node.js": "node",
    "expressjs": "express", "express.js": "express",
    "postgres": "postgresql", "psql": "postgresql",
    "ml": "machine learning", "dl": "deep learning",
    "ai": "machine learning",
}


def extract_skills(text: str) -> list:
    """Extract skills from text using KeyBERT + known skills list."""
    text_lower = text.lower()
    found_skills = set()
    for skill in TECHNICAL_SKILLS:
        if skill in text_lower:
            found_skills.add(skill)

    for synonym, canonical in SKILL_SYNONYMS.items():
        if synonym in text_lower:
            found_skills.add(canonical)

    try:
        keywords = kw_model.extract_keywords(
            text,
            keyphrase_ngram_range=(1, 3),
            stop_words="english",
            top_n=20,
        )
        for kw in keywords:
            candidate = kw[0].lower()
            for skill in TECHNICAL_SKILLS:
                if skill in candidate or candidate in skill:
                    found_skills.add(skill)
    except Exception:
        pass

    return sorted(list(found_skills))


# ---------------------------------------------------------------------------
# Experience / Education / Projects extraction from resume text
# ---------------------------------------------------------------------------
def extract_experience(text: str) -> list:
    """Extract work experience entries from resume text."""
    experiences = []
    lines = text.split("\n")
    current_entry = []
    in_experience_section = False
    section_keywords = ["experience", "work history", "employment", "work experience", "professional experience"]

    for line in lines:
        line_lower = line.strip().lower()
        if any(kw in line_lower for kw in section_keywords):
            in_experience_section = True
            continue
        if in_experience_section:
            if any(kw in line_lower for kw in ["education", "skills", "projects", "certification"]):
                if current_entry:
                    experiences.append(" ".join(current_entry).strip())
                in_experience_section = False
                continue
            if line.strip():
                current_entry.append(line.strip())

    if current_entry and in_experience_section:
        experiences.append(" ".join(current_entry).strip())

    return experiences if experiences else ["No experience section found"]


def extract_education(text: str) -> list:
    """Extract education entries from resume text."""
    education = []
    lines = text.split("\n")
    current_entry = []
    in_edu_section = False

    for line in lines:
        line_lower = line.strip().lower()
        if any(kw in line_lower for kw in ["education", "academic", "qualification"]):
            in_edu_section = True
            continue
        if in_edu_section:
            if any(kw in line_lower for kw in ["experience", "skills", "projects", "work"]):
                if current_entry:
                    education.append(" ".join(current_entry).strip())
                in_edu_section = False
                continue
            if line.strip():
                current_entry.append(line.strip())

    if current_entry and in_edu_section:
        education.append(" ".join(current_entry).strip())

    return education if education else ["No education section found"]


def extract_projects(text: str) -> list:
    """Extract project entries from resume text."""
    projects = []
    lines = text.split("\n")
    current_entry = []
    in_project_section = False

    for line in lines:
        line_lower = line.strip().lower()
        if any(kw in line_lower for kw in ["projects", "project experience", "key projects"]):
            in_project_section = True
            continue
        if in_project_section:
            if any(kw in line_lower for kw in ["education", "skills", "experience", "certification"]):
                if current_entry:
                    projects.append(" ".join(current_entry).strip())
                in_project_section = False
                continue
            if line.strip():
                current_entry.append(line.strip())

    if current_entry and in_project_section:
        projects.append(" ".join(current_entry).strip())

    return projects if projects else ["No projects section found"]


# ---------------------------------------------------------------------------
# Skill gap analysis
# ---------------------------------------------------------------------------
def analyze_skill_gap(resume_skills: list, job_skills: list) -> dict:
    """Analyze skill gaps and provide learning suggestions."""
    resume_set = set(s.lower() for s in resume_skills)
    job_set = set(s.lower() for s in job_skills)

    matched = list(resume_set & job_set)
    missing = list(job_set - resume_set)

    learning_suggestions = {}
    for skill in missing:
        suggestions = _get_learning_suggestions(skill)
        if suggestions:
            learning_suggestions[skill] = suggestions

    certs = _get_certification_recommendations(missing)

    match_rate = (len(matched) / len(job_set) * 100) if job_set else 100.0

    return {
        "skillsMatched": sorted(matched),
        "skillsMissing": sorted(missing),
        "matchRate": round(match_rate, 2),
        "totalRequired": len(job_set),
        "totalMatched": len(matched),
        "learningSuggestions": learning_suggestions,
        "certificationRecommendations": certs,
    }


def _get_learning_suggestions(skill: str) -> list:
    """Return learning resources for a given skill."""
    suggestions = {
        "python": ["Python for Everybody (Coursera)", "Automate the Boring Stuff (Book)"],
        "java": ["Java Programming Masterclass (Udemy)", "Effective Java (Book)"],
        "javascript": ["JavaScript: The Good Parts (Book)", "The Modern JavaScript Tutorial"],
        "react": ["React - The Complete Guide (Udemy)", "React Documentation (Official)"],
        "docker": ["Docker Mastery (Udemy)", "Docker Documentation (Official)"],
        "kubernetes": ["Kubernetes in Action (Book)", "CKAD Certification Prep (Udemy)"],
        "aws": ["AWS Certified Solutions Architect (Udemy)", "AWS Documentation"],
        "machine learning": ["Machine Learning by Andrew Ng (Coursera)", "Hands-On ML (Book)"],
        "sql": ["SQL for Data Science (Coursera)", "LeetCode SQL Problems"],
        "spring": ["Spring Boot in Practice (Book)", "Spring Framework Guru (Udemy)"],
        "typescript": ["TypeScript Handbook (Official)", "Understanding TypeScript (Udemy)"],
        "go": ["Go: The Complete Developer's Guide (Udemy)", "The Go Programming Language (Book)"],
        "rust": ["The Rust Programming Language (Book)", "Rust in Action (Book)"],
    }
    return suggestions.get(skill, [f"Consider taking a course on {skill.title()}"])


def _get_certification_recommendations(skills: list) -> list:
    """Return certification recommendations based on missing skills."""
    cert_map = {
        "aws": "AWS Certified Solutions Architect",
        "docker": "Docker Certified Associate (DCA)",
        "kubernetes": "Certified Kubernetes Administrator (CKA)",
        "python": "PCAP - Certified Associate in Python Programming",
        "java": "Oracle Certified Professional Java SE",
        "javascript": "JavaScript Certificate (freeCodeCamp)",
        "sql": "Microsoft Certified: Azure Data Fundamentals",
        "machine learning": "TensorFlow Developer Certificate",
        "cloud": "Google Cloud Professional Cloud Architect",
        "security": "CompTIA Security+",
        "project management": "PMP Certification",
        "gcp": "Google Cloud Professional Data Engineer",
        "azure": "Microsoft Certified: Azure Solutions Architect",
    }
    certs = []
    for skill in skills:
        if skill in cert_map:
            certs.append(cert_map[skill])
    return list(set(certs))


# ---------------------------------------------------------------------------
# Collaborative filtering recommendation
# ---------------------------------------------------------------------------
class CollaborativeFilter:
    """Simple collaborative filtering based on user-job interaction matrix."""

    def __init__(self):
        self.user_job_matrix = {}  # {user_id: {job_id: score}}
        self.job_similarities = {}

    def add_interaction(self, user_id: int, job_id: int, score: float):
        if user_id not in self.user_job_matrix:
            self.user_job_matrix[user_id] = {}
        self.user_job_matrix[user_id][job_id] = score

    def get_recommendations(self, user_id: int, top_n: int = 10) -> list:
        """Get collaborative filtering recommendations."""
        if user_id not in self.user_job_matrix:
            return []

        user_jobs = set(self.user_job_matrix[user_id].keys())
        similar_users = []
        for other_id, other_jobs in self.user_job_matrix.items():
            if other_id == user_id:
                continue
            common_jobs = user_jobs & set(other_jobs.keys())
            if common_jobs:
                similarity = len(common_jobs) / max(len(user_jobs), len(other_jobs))
                if similarity > 0:
                    similar_users.append((other_id, similarity))

        job_scores = {}
        for other_id, similarity in similar_users:
            for job_id, score in self.user_job_matrix[other_id].items():
                if job_id not in user_jobs:
                    if job_id not in job_scores:
                        job_scores[job_id] = 0
                    job_scores[job_id] += similarity * score

        sorted_jobs = sorted(job_scores.items(), key=lambda x: x[1], reverse=True)
        return [{"jobId": jid, "score": round(s, 2)} for jid, s in sorted_jobs[:top_n]]


# Global collaborative filter instance
cf = CollaborativeFilter()


# ---------------------------------------------------------------------------
# Content-based filtering recommendation
# ---------------------------------------------------------------------------
class ContentBasedFilter:
    """Content-based filtering using job descriptions and user profile embeddings."""

    def __init__(self):
        self.job_embeddings = {}  # {job_id: embedding}
        self.job_metadata = {}    # {job_id: {title, description, skills}}

    def add_job(self, job_id: int, title: str, description: str, skills: list):
        """Add a job with its metadata for similarity computation."""
        text = f"{title} {' '.join(skills)} {description}"[:2000]
        self.job_embeddings[job_id] = encode(text)
        self.job_metadata[job_id] = {
            "title": title,
            "description": description,
            "skills": skills,
        }

    def get_recommendations(self, user_profile_text: str, top_n: int = 10) -> list:
        """Get content-based recommendations based on user profile."""
        user_embedding = encode(user_profile_text[:5000])
        scores = {}
        for job_id, job_emb in self.job_embeddings.items():
            sim = util.cos_sim(user_embedding, job_emb).item()
            scores[job_id] = round(max(0.0, min(1.0, sim)) * 100, 2)

        sorted_jobs = sorted(scores.items(), key=lambda x: x[1], reverse=True)
        return [{"jobId": jid, "score": s} for jid, s in sorted_jobs[:top_n]]


# Global content-based filter instance
cbf = ContentBasedFilter()


# ---------------------------------------------------------------------------
# Hybrid recommendation engine
# ---------------------------------------------------------------------------
class HybridRecommender:
    """Combines collaborative and content-based filtering with weighted scoring."""

    def __init__(self, cf_weight: float = 0.4, cbf_weight: float = 0.6):
        self.cf_weight = cf_weight
        self.cbf_weight = cbf_weight

    def get_recommendations(
        self, user_id: int, user_profile_text: str, top_n: int = 10
    ) -> list:
        """Get hybrid recommendations merging CF + CBF."""
        cf_recs = cf.get_recommendations(user_id, top_n=top_n * 2)
        cbf_recs = cbf.get_recommendations(user_profile_text, top_n=top_n * 2)

        # Merge scores with weights
        merged = {}
        for rec in cf_recs:
            merged[rec["jobId"]] = self.cf_weight * rec["score"]
        for rec in cbf_recs:
            if rec["jobId"] in merged:
                merged[rec["jobId"]] += self.cbf_weight * rec["score"]
            else:
                merged[rec["jobId"]] = self.cbf_weight * rec["score"]

        sorted_jobs = sorted(merged.items(), key=lambda x: x[1], reverse=True)
        return [{"jobId": jid, "score": round(s, 2)} for jid, s in sorted_jobs[:top_n]]


# Global hybrid recommender instance
hybrid_rec = HybridRecommender()


# ---------------------------------------------------------------------------
# Feedback learning loop - tracks user feedback to improve recommendations
# ---------------------------------------------------------------------------
class FeedbackLearning:
    """Learns from user feedback to improve future recommendations."""

    def __init__(self):
        self.feedback_log = []   # List of {user_id, job_id, feedback_type, timestamp}
        self.skill_weights = {}  # {skill: weight} - learned importance

    def record_feedback(self, user_id: int, job_id: int, feedback_type: str):
        """Record user feedback on a recommendation."""
        self.feedback_log.append({
            "userId": user_id,
            "jobId": job_id,
            "feedbackType": feedback_type,
            "timestamp": time.time(),
        })
        # Update skill weights based on positive/negative feedback
        weight_delta = 0.1 if feedback_type in ["apply", "save"] else -0.1
        # Check if user has interacted with this job's skills
        job_meta = cbf.job_metadata.get(job_id, {})
        for skill in job_meta.get("skills", []):
            if skill not in self.skill_weights:
                self.skill_weights[skill] = 0.5
            self.skill_weights[skill] = max(0.0, min(1.0,
                self.skill_weights[skill] + weight_delta))

    def get_feedback_stats(self) -> dict:
        """Get aggregated feedback statistics."""
        if not self.feedback_log:
            return {"totalFeedback": 0, "positiveRate": 0.0}

        total = len(self.feedback_log)
        positive = sum(1 for f in self.feedback_log
                       if f["feedbackType"] in ["apply", "save"])
        return {
            "totalFeedback": total,
            "positiveRate": round(positive / total * 100, 2),
            "trackedSkills": len(self.skill_weights),
        }


# Global feedback learning instance
feedback_learning = FeedbackLearning()


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------
@app.before_request
def check_api_key():
    if request.path == "/health" or request.path == "/":
        return
    protected_paths = ["/match", "/match-batch", "/skill-gap", "/parse",
                       "/cf-interact", "/cf-recommend", "/cbf-add", "/cbf-recommend",
                       "/hybrid-recommend", "/feedback", "/feedback-stats",
                       "/analyze-resume"]
    if request.method == "POST" and request.path in protected_paths:
        if not API_KEY:
            return
        key = request.headers.get("X-API-Key")
        if key != API_KEY:
            return jsonify({"error": "Unauthorized"}), 401


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@app.route("/", methods=["GET"])
@app.route("/health", methods=["GET"])
def home():
    return jsonify({
        "service": "job-match-ai",
        "status": "running",
        "version": "3.0.0",
        "model": FINETUNED_MODEL_PATH if FINETUNED_MODEL_PATH else DEFAULT_MODEL,
        "endpoints": [
            "/health", "/match", "/match-batch", "/skill-gap",
            "/parse", "/cf-recommend", "/cf-interact", "/metrics",
            "/model-info", "/drift-check", "/ab-test",
            "/cbf-add", "/cbf-recommend", "/hybrid-recommend",
            "/feedback", "/feedback-stats", "/analyze-resume"
        ],
        "has_pdf_support": HAS_PDF,
        "has_docx_support": HAS_DOCX,
    })


@app.route("/match", methods=["POST"])
def match():
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid or missing JSON body"}), 400

    resume_text = (
        data.get("resumeText") or data.get("resume") or data.get("resume_text") or ""
    )
    job_description = (
        data.get("jobDescription") or data.get("job") or data.get("job_description") or ""
    )

    if not resume_text or not job_description:
        return jsonify({
            "error": "Both resume text and job description are required",
            "acceptedFields": ["resumeText + jobDescription", "resume + job"],
        }), 400

    if len(resume_text) > 50000:
        return jsonify({"error": "Resume input too large"}), 413
    if len(job_description) > 10000:
        return jsonify({"error": "Job description input too large"}), 413

    resume_text = resume_text[:5000]
    job_description = job_description[:2000]

    resume_embedding = encode(resume_text)
    job_embedding = encode(job_description)
    similarity = util.cos_sim(resume_embedding, job_embedding).item()
    score = round(max(0.0, min(1.0, similarity)) * 100, 2)

    job_skills = extract_skills(job_description)
    resume_skills = extract_skills(resume_text)

    gap_analysis = analyze_skill_gap(resume_skills, job_skills)

    _update_metrics(score)

    return jsonify({
        "matchScore": score,
        "skillsMatched": gap_analysis["skillsMatched"],
        "skillsMissing": gap_analysis["skillsMissing"],
        "skillMatchRate": gap_analysis["matchRate"],
        "learningSuggestions": gap_analysis["learningSuggestions"],
        "certificationRecommendations": gap_analysis["certificationRecommendations"],
    })


@app.route("/match-batch", methods=["POST"])
def match_batch():
    """Batch match endpoint for efficient processing of multiple jobs."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid or missing JSON body"}), 400

    resume_text = (
        data.get("resumeText") or data.get("resume") or data.get("resume_text") or ""
    )
    jobs = data.get("jobs") or data.get("jobDescriptions") or []

    if not resume_text:
        return jsonify({"error": "Resume text is required"}), 400
    if not jobs or not isinstance(jobs, list):
        return jsonify({"error": "Jobs array is required"}), 400

    resume_text = resume_text[:5000]
    resume_embedding = encode(resume_text)
    resume_skills = extract_skills(resume_text)

    results = []
    for job in jobs:
        job_desc = job.get("description") or job.get("text") or ""
        job_title = job.get("title") or ""
        job_context = f"{job_title}\n{job_desc}"[:2000]

        job_embedding = encode(job_context)
        similarity = util.cos_sim(resume_embedding, job_embedding).item()
        score = round(max(0.0, min(1.0, similarity)) * 100, 2)

        job_skills = extract_skills(job_context)
        gap = analyze_skill_gap(resume_skills, job_skills)

        results.append({
            "jobId": job.get("id"),
            "jobTitle": job_title,
            "matchScore": score,
            "skillsMatched": gap["skillsMatched"],
            "skillsMissing": gap["skillsMissing"],
            "skillMatchRate": gap["matchRate"],
        })

        _update_metrics(score)

    results.sort(key=lambda x: x["matchScore"], reverse=True)

    return jsonify({
        "results": results,
        "totalProcessed": len(results),
    })


@app.route("/skill-gap", methods=["POST"])
def skill_gap():
    """Dedicated skill gap analysis endpoint."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid or missing JSON body"}), 400

    resume_text = (
        data.get("resumeText") or data.get("resume") or data.get("resume_text") or ""
    )
    job_text = (
        data.get("jobDescription") or data.get("job") or data.get("job_description") or ""
    )

    if not resume_text:
        return jsonify({"error": "Resume text is required"}), 400
    if not job_text:
        return jsonify({"error": "Job description is required"}), 400

    resume_skills = extract_skills(resume_text)
    job_skills = extract_skills(job_text)

    analysis = analyze_skill_gap(resume_skills, job_skills)

    return jsonify({
        "userSkills": resume_skills,
        "requiredSkills": job_skills,
        "skillGap": analysis,
    })


@app.route("/parse", methods=["POST"])
def parse_document():
    """Parse uploaded documents (PDF/DOCX/TXT) and extract text + skills."""
    if "file" not in request.files:
        return jsonify({"error": "No file provided"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "No file selected"}), 400

    filename = file.filename.lower()
    file_bytes = file.read()

    try:
        if filename.endswith(".pdf"):
            if not HAS_PDF:
                return jsonify({"error": "PDF support not available"}), 500
            text = extract_text_from_pdf(file_bytes)
        elif filename.endswith(".docx"):
            if not HAS_DOCX:
                return jsonify({"error": "DOCX support not available"}), 500
            text = extract_text_from_docx(file_bytes)
        elif filename.endswith(".txt"):
            text = extract_text_from_txt(file_bytes)
        else:
            return jsonify({"error": "Unsupported file format. Supported: .pdf, .docx, .txt"}), 400
    except Exception as e:
        return jsonify({"error": f"Failed to parse file: {str(e)}"}), 422

    if not text.strip():
        return jsonify({"error": "No text could be extracted from the file"}), 422

    skills = extract_skills(text)

    return jsonify({
        "text": text[:10000],
        "skills": skills,
        "wordCount": len(text.split()),
        "fileName": file.filename,
    })


@app.route("/cf-interact", methods=["POST"])
def cf_interact():
    """Record user-job interaction for collaborative filtering."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    user_id = data.get("userId")
    job_id = data.get("jobId")
    interaction_type = data.get("type", "view")

    if not user_id or not job_id:
        return jsonify({"error": "userId and jobId required"}), 400

    score_map = {"view": 1, "save": 3, "apply": 5, "ignore": -1}
    score = score_map.get(interaction_type, 1)

    cf.add_interaction(int(user_id), int(job_id), score)

    return jsonify({"status": "recorded", "userId": user_id, "jobId": job_id, "type": interaction_type})


@app.route("/cf-recommend", methods=["POST"])
def cf_recommend():
    """Get collaborative filtering recommendations."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    user_id = data.get("userId")
    if not user_id:
        return jsonify({"error": "userId is required"}), 400

    recs = cf.get_recommendations(int(user_id), top_n=data.get("topN", 10))

    return jsonify({
        "userId": user_id,
        "recommendations": recs,
        "type": "collaborative_filtering",
    })


@app.route("/cbf-add", methods=["POST"])
def cbf_add():
    """Add a job to the content-based filter."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    job_id = data.get("jobId")
    title = data.get("title", "")
    description = data.get("description", "")
    skills = data.get("skills", [])

    if not job_id:
        return jsonify({"error": "jobId is required"}), 400

    cbf.add_job(int(job_id), title, description, skills)

    return jsonify({"status": "added", "jobId": job_id})


@app.route("/cbf-recommend", methods=["POST"])
def cbf_recommend():
    """Get content-based filtering recommendations."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    user_profile_text = data.get("profileText", "")
    if not user_profile_text:
        return jsonify({"error": "profileText is required"}), 400

    recs = cbf.get_recommendations(user_profile_text, top_n=data.get("topN", 10))

    return jsonify({
        "recommendations": recs,
        "type": "content_based_filtering",
    })


@app.route("/hybrid-recommend", methods=["POST"])
def hybrid_recommend():
    """Get hybrid recommendations combining CF and CBF."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    user_id = data.get("userId")
    user_profile_text = data.get("profileText", "")
    cf_weight = data.get("cfWeight", 0.4)
    cbf_weight = data.get("cbfWeight", 0.6)

    if not user_id:
        return jsonify({"error": "userId is required"}), 400
    if not user_profile_text:
        return jsonify({"error": "profileText is required"}), 400

    recs = hybrid_rec.get_recommendations(
        int(user_id), user_profile_text, top_n=data.get("topN", 10)
    )

    return jsonify({
        "userId": user_id,
        "recommendations": recs,
        "type": "hybrid",
        "weights": {"cf": cf_weight, "cbf": cbf_weight},
    })


@app.route("/feedback", methods=["POST"])
def record_feedback():
    """Record user feedback for learning."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    user_id = data.get("userId")
    job_id = data.get("jobId")
    feedback_type = data.get("type", "view")

    if not user_id or not job_id:
        return jsonify({"error": "userId and jobId required"}), 400

    feedback_learning.record_feedback(int(user_id), int(job_id), feedback_type)

    return jsonify({
        "status": "recorded",
        "userId": user_id,
        "jobId": job_id,
        "type": feedback_type,
    })


@app.route("/feedback-stats", methods=["GET"])
def feedback_stats():
    """Get feedback learning statistics."""
    return jsonify(feedback_learning.get_feedback_stats())


@app.route("/analyze-resume", methods=["POST"])
def analyze_resume():
    """Analyze resume and extract experience, education, projects, and skills."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid or missing JSON body"}), 400

    resume_text = (
        data.get("resumeText") or data.get("resume") or data.get("resume_text") or ""
    )

    if not resume_text:
        return jsonify({"error": "Resume text is required"}), 400

    if len(resume_text) > 50000:
        return jsonify({"error": "Resume input too large"}), 413

    resume_text = resume_text[:10000]
    skills = extract_skills(resume_text)
    experience = extract_experience(resume_text)
    education = extract_education(resume_text)
    projects = extract_projects(resume_text)

    return jsonify({
        "skills": skills,
        "experience": experience,
        "education": education,
        "projects": projects,
        "wordCount": len(resume_text.split()),
    })


@app.route("/model-info", methods=["GET"])
def model_info():
    """Get model version and performance metrics."""
    return jsonify({
        "modelName": FINETUNED_MODEL_PATH if FINETUNED_MODEL_PATH else DEFAULT_MODEL,
        "totalPredictions": _model_metrics["total_predictions"],
        "averageScore": round(_model_metrics["avg_score"], 2),
        "scoreDistribution": _model_metrics["score_distribution"],
        "driftWarnings": _model_metrics["drift_warnings"],
        "cacheSize": len(_EMBEDDING_CACHE),
        "cacheMaxSize": _MAX_CACHE_SIZE,
    })


@app.route("/drift-check", methods=["POST"])
def drift_check():
    """Check for model drift by comparing scores over time."""
    data = request.get_json(silent=True) or {}
    recent_scores = data.get("recentScores", [])
    threshold = data.get("threshold", 0.1)

    if not recent_scores:
        return jsonify({"driftDetected": False, "message": "No scores to analyze"})

    avg_recent = np.mean(recent_scores)
    overall_avg = _model_metrics["avg_score"]

    drift = abs(avg_recent - overall_avg) / max(overall_avg, 0.01)
    drift_detected = drift > threshold

    if drift_detected:
        _model_metrics["drift_warnings"] += 1

    return jsonify({
        "driftDetected": drift_detected,
        "driftScore": round(drift, 4),
        "averageRecentScore": round(avg_recent, 2),
        "overallAverageScore": round(overall_avg, 2),
        "totalDriftWarnings": _model_metrics["drift_warnings"],
    })


@app.route("/ab-test", methods=["POST"])
def ab_test():
    """Compare two model versions for A/B testing."""
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Invalid JSON"}), 400

    resume_text = data.get("resumeText", "")
    job_description = data.get("jobDescription", "")
    model_a_name = data.get("modelA", DEFAULT_MODEL)
    model_b_name = data.get("modelB", "")

    if not model_b_name:
        return jsonify({"error": "modelB is required for comparison"}), 400

    try:
        model_a = SentenceTransformer(model_a_name)
        model_b = SentenceTransformer(model_b_name)
    except Exception as e:
        return jsonify({"error": f"Failed to load models: {str(e)}"}), 400

    emb_a = model_a.encode(resume_text[:5000], convert_to_tensor=True)
    emb_b = model_b.encode(resume_text[:5000], convert_to_tensor=True)
    job_emb = model_a.encode(job_description[:2000], convert_to_tensor=True)

    score_a = util.cos_sim(emb_a, job_emb).item()
    score_b = util.cos_sim(emb_b, job_emb).item()

    return jsonify({
        "modelA": {
            "name": model_a_name,
            "score": round(max(0, min(1, score_a)) * 100, 2),
        },
        "modelB": {
            "name": model_b_name,
            "score": round(max(0, min(1, score_b)) * 100, 2),
        },
        "difference": round(abs(score_a - score_b) * 100, 2),
    })


# ---------------------------------------------------------------------------
# Metrics helper
# ---------------------------------------------------------------------------
def _update_metrics(score: float):
    _model_metrics["total_predictions"] += 1
    total = _model_metrics["total_predictions"]
    _model_metrics["avg_score"] = (
        (_model_metrics["avg_score"] * (total - 1)) + score
    ) / total

    if score <= 20:
        _model_metrics["score_distribution"]["0-20"] += 1
    elif score <= 40:
        _model_metrics["score_distribution"]["21-40"] += 1
    elif score <= 60:
        _model_metrics["score_distribution"]["41-60"] += 1
    elif score <= 80:
        _model_metrics["score_distribution"]["61-80"] += 1
    else:
        _model_metrics["score_distribution"]["81-100"] += 1


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    debug = os.environ.get("FLASK_DEBUG", "false").lower() == "true"
    app.run(host="0.0.0.0", port=port, debug=debug)