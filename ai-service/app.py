from flask import Flask, request, jsonify
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

app = Flask(__name__)


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


@app.route("/", methods=["GET"])
def home():
    return jsonify(
        {
            "service": "job-match-ai",
            "status": "running",
            "endpoints": ["/match"],
        }
    )


@app.route('/match', methods=['POST'])
def match():
    data = request.get_json(silent=True)

    if not isinstance(data, dict):
        return jsonify({
            "error": "Invalid or missing JSON body"
        }), 400

    resume_text, job_description = _read_text_fields(data)

    if not resume_text or not job_description:
        return jsonify({
            "error": "Both resume text and job description are required",
            "acceptedFields": [
                "resumeText + jobDescription",
                "resume + job",
            ],
        }), 400

    documents = [resume_text, job_description]

    vectorizer = TfidfVectorizer()
    vectors = vectorizer.fit_transform(documents)

    similarity = cosine_similarity(vectors[0:1], vectors[1:2])[0][0]

    score = round(similarity * 100, 2)

    return jsonify({
        "matchScore": score
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
