from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer, util

app = Flask(__name__)
model = SentenceTransformer("all-MiniLM-L6-v2")

SKILLS = [
    "python",
    "java",
    "spring boot",
    "react",
    "javascript",
    "typescript",
    "sql",
    "mysql",
    "docker",
    "kubernetes",
    "aws",
    "git",
    "rest api",
    "machine learning",
    "data analysis",
]


def _read_text_fields(data):
    resume_text = (
        data.get("resumeText")
        or data.get("resume")
        or data.get("resume_text")
        or ""
    )
    job_description = (
        data.get("jobDescription")
        or data.get("job")
        or data.get("job_description")
        or ""
    )
    return resume_text.strip(), job_description.strip()


def _extract_skill_gaps(resume_text, job_description):
    resume_lower = resume_text.lower()
    job_lower = job_description.lower()

    matched = []
    missing = []

    for skill in SKILLS:
        in_job = skill in job_lower
        in_resume = skill in resume_lower

        if in_job and in_resume:
            matched.append(skill.title())
        elif in_job:
            missing.append(skill.title())

    return matched, missing


@app.route("/", methods=["GET"])
@app.route("/health", methods=["GET"])
def home():
    return jsonify(
        {
            "service": "job-match-ai",
            "status": "running",
            "endpoints": ["/match", "/health"],
        }
    )


@app.route("/match", methods=["POST"])
def match():
    data = request.get_json(silent=True)

    if not isinstance(data, dict):
        return jsonify({"error": "Invalid or missing JSON body"}), 400

    resume_text, job_description = _read_text_fields(data)

    if not resume_text or not job_description:
        return jsonify(
            {
                "error": "Both resume text and job description are required",
                "acceptedFields": [
                    "resumeText + jobDescription",
                    "resume + job",
                ],
            }
        ), 400

    resume_embedding = model.encode(resume_text, convert_to_tensor=True)
    job_embedding = model.encode(job_description, convert_to_tensor=True)
    similarity = util.cos_sim(resume_embedding, job_embedding).item()
    score = round(max(0.0, min(1.0, similarity)) * 100, 2)
    skills_matched, skills_missing = _extract_skill_gaps(resume_text, job_description)

    return jsonify(
        {
            "matchScore": score,
            "skillsMatched": skills_matched,
            "skillsMissing": skills_missing,
        }
    )


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
