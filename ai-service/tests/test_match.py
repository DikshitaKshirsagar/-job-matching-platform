"""Comprehensive test suite for the AI matching service."""
import json
import pytest
from app import app, cf, TECHNICAL_SKILLS


@pytest.fixture
def client():
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["status"] == "running"
    assert data["service"] == "job-match-ai"
    assert data["version"] == "3.0.0"
    assert data["has_docx_support"] is True or data["has_docx_support"] is False
    assert data["has_pdf_support"] is True or data["has_pdf_support"] is False


def test_model_info(client):
    resp = client.get("/model-info")
    assert resp.status_code == 200
    data = resp.get_json()
    assert "modelName" in data
    assert "totalPredictions" in data
    assert "scoreDistribution" in data


def test_match_success(client):
    resp = client.post("/match", json={
        "resumeText": "Python developer with experience in Django, Flask, and PostgreSQL. "
                      "Built REST APIs and microservices. Used Docker and AWS.",
        "jobDescription": "Senior Python Developer needed. Must know Django, PostgreSQL, "
                          "Docker, and AWS. Experience with REST APIs required."
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert "matchScore" in data
    assert "skillsMatched" in data
    assert "skillsMissing" in data
    assert "skillMatchRate" in data
    assert "learningSuggestions" in data
    assert "certificationRecommendations" in data
    assert 0 <= data["matchScore"] <= 100
    assert "python" in [s.lower() for s in data["skillsMatched"]]


def test_match_with_resume_and_job_fields(client):
    resp = client.post("/match", json={
        "resume": "Experienced Java developer with Spring Boot and MySQL.",
        "job": "Java developer needed with Spring Boot experience."
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert "matchScore" in data


def test_match_missing_resume(client):
    resp = client.post("/match", json={"job": "Some job description"})
    assert resp.status_code == 400


def test_match_missing_job(client):
    resp = client.post("/match", json={"resume": "Some resume text"})
    assert resp.status_code == 400


def test_match_empty_body(client):
    resp = client.post("/match", json={})
    assert resp.status_code == 400


def test_match_invalid_json(client):
    resp = client.post("/match", data="not json", content_type="application/json")
    assert resp.status_code == 400


def test_match_input_too_large(client):
    huge_resume = "A" * 60000
    resp = client.post("/match", json={
        "resumeText": huge_resume,
        "jobDescription": "A normal job description"
    })
    assert resp.status_code == 413


def test_match_consistency(client):
    """Same input should produce same output (deterministic)."""
    payload = {
        "resumeText": "Data scientist with Python, TensorFlow, and ML experience.",
        "jobDescription": "Looking for a data scientist with TensorFlow and ML skills."
    }
    resp1 = client.post("/match", json=payload)
    resp2 = client.post("/match", json=payload)
    data1 = resp1.get_json()
    data2 = resp2.get_json()
    assert data1["matchScore"] == data2["matchScore"]
    assert data1["skillsMatched"] == data2["skillsMatched"]


def test_skill_extraction_comprehensive(client):
    """Test that technical skills are properly extracted."""
    resume = ("Expert in Python, Java, Spring Boot, Docker, Kubernetes, "
              "AWS, React, PostgreSQL, Redis, Kafka, and Jenkins.")
    resp = client.post("/match", json={
        "resumeText": resume,
        "jobDescription": "Need Python, Java, Docker, and AWS skills."
    })
    data = resp.get_json()
    matched_skills = [s.lower() for s in data["skillsMatched"]]
    assert "python" in matched_skills
    assert "java" in matched_skills
    assert "docker" in matched_skills
    assert "aws" in matched_skills


def test_skill_synonyms(client):
    """Test that skill synonyms are resolved correctly."""
    resp = client.post("/match", json={
        "resumeText": "Expert in JS, TS, React, Node, and ML.",
        "jobDescription": "Need JavaScript, TypeScript, React, Node.js, and Machine Learning."
    })
    data = resp.get_json()
    matched_skills = [s.lower() for s in data["skillsMatched"]]
    assert "javascript" in matched_skills
    assert "typescript" in matched_skills
    assert "react" in matched_skills
    assert "node" in matched_skills
    assert "machine learning" in matched_skills


def test_skill_gap_analysis(client):
    """Test skill gap endpoint."""
    resp = client.post("/skill-gap", json={
        "resumeText": "Python developer with Flask experience.",
        "jobDescription": "Python developer needed with Django, Docker, and AWS."
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert "userSkills" in data
    assert "requiredSkills" in data
    assert "skillGap" in data
    assert "python" in [s.lower() for s in data["skillGap"]["skillsMatched"]]


def test_batch_match(client):
    """Test batch matching endpoint."""
    resp = client.post("/match-batch", json={
        "resumeText": "Python developer with Django and PostgreSQL.",
        "jobs": [
            {"id": 1, "title": "Python Developer", "description": "Python, Django, PostgreSQL"},
            {"id": 2, "title": "Java Developer", "description": "Java, Spring Boot, MySQL"},
        ]
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert len(data["results"]) == 2
    assert data["results"][0]["matchScore"] >= data["results"][1]["matchScore"]


def test_collaborative_filtering(client):
    """Test collaborative filtering interactions and recommendations."""
    resp1 = client.post("/cf-interact", json={
        "userId": 1, "jobId": 101, "type": "apply"
    })
    assert resp1.status_code == 200

    resp2 = client.post("/cf-interact", json={
        "userId": 1, "jobId": 102, "type": "save"
    })
    assert resp2.status_code == 200

    client.post("/cf-interact", json={"userId": 2, "jobId": 101, "type": "apply"})
    client.post("/cf-interact", json={"userId": 2, "jobId": 103, "type": "apply"})

    resp3 = client.post("/cf-recommend", json={"userId": 1, "topN": 5})
    assert resp3.status_code == 200
    data = resp3.get_json()
    assert "recommendations" in data
    assert data["type"] == "collaborative_filtering"


def test_content_based_filtering(client):
    """Test content-based filtering add and recommend."""
    client.post("/cbf-add", json={
        "jobId": 201, "title": "Python Developer",
        "description": "Need Python, Django, PostgreSQL",
        "skills": ["python", "django", "postgresql"]
    })
    client.post("/cbf-add", json={
        "jobId": 202, "title": "Java Developer",
        "description": "Need Java, Spring Boot",
        "skills": ["java", "spring"]
    })
    resp = client.post("/cbf-recommend", json={
        "profileText": "Python developer with Django experience"
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert "recommendations" in data
    assert data["type"] == "content_based_filtering"


def test_hybrid_recommend(client):
    """Test hybrid recommendation endpoint."""
    client.post("/cf-interact", json={"userId": 3, "jobId": 301, "type": "apply"})
    client.post("/cbf-add", json={
        "jobId": 301, "title": "ML Engineer",
        "description": "Machine Learning, Python, TensorFlow",
        "skills": ["machine learning", "python", "tensorflow"]
    })
    resp = client.post("/hybrid-recommend", json={
        "userId": 3,
        "profileText": "ML engineer with Python and TensorFlow"
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert "recommendations" in data
    assert data["type"] == "hybrid"


def test_feedback_learning(client):
    """Test feedback recording and stats."""
    resp = client.post("/feedback", json={
        "userId": 1, "jobId": 101, "type": "apply"
    })
    assert resp.status_code == 200

    resp = client.post("/feedback", json={
        "userId": 1, "jobId": 102, "type": "ignore"
    })
    assert resp.status_code == 200

    resp = client.get("/feedback-stats")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["totalFeedback"] >= 2
    assert data["positiveRate"] > 0


def test_analyze_resume(client):
    """Test resume analysis endpoint."""
    resume = """Experience
Senior Developer at Tech Corp 2020-2023
Led development of REST APIs

Education
MS Computer Science, MIT 2020

Projects
Built job matching platform using ML

Skills: Python, Django, Docker"""
    resp = client.post("/analyze-resume", json={"resumeText": resume})
    assert resp.status_code == 200
    data = resp.get_json()
    assert "skills" in data
    assert "experience" in data
    assert "education" in data
    assert "projects" in data
    assert "python" in [s.lower() for s in data["skills"]]


def test_drift_check(client):
    """Test drift detection endpoint."""
    resp = client.post("/drift-check", json={
        "recentScores": [85, 90, 78, 88, 92],
        "threshold": 0.5
    })
    assert resp.status_code == 200
    data = resp.get_json()
    assert "driftDetected" in data
    assert "driftScore" in data


def test_drift_check_empty(client):
    """Test drift check with no scores."""
    resp = client.post("/drift-check", json={"recentScores": []})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["driftDetected"] is False


def test_ab_test(client):
    """Test A/B testing endpoint."""
    resp = client.post("/ab-test", json={
        "resumeText": "Python developer",
        "jobDescription": "Need Python developer",
        "modelA": "all-MiniLM-L6-v2",
        "modelB": "all-MiniLM-L6-v2"
    })
    assert resp.status_code in [200, 400]


def test_parse_txt(client):
    """Test TXT file parsing."""
    data = {"file": (b"Python developer with Django experience", "resume.txt")}
    resp = client.post("/parse", data=data, content_type="multipart/form-data")
    assert resp.status_code == 200
    result = resp.get_json()
    assert "skills" in result
    assert "python" in [s.lower() for s in result["skills"]]


def test_parse_empty_file(client):
    """Test parsing an empty file."""
    data = {"file": (b"", "empty.txt")}
    resp = client.post("/parse", data=data, content_type="multipart/form-data")
    assert resp.status_code == 422


def test_parse_no_file(client):
    """Test parse with no file."""
    resp = client.post("/parse", data={}, content_type="multipart/form-data")
    assert resp.status_code == 400


def test_learning_suggestions(client):
    """Test that learning suggestions are returned for missing skills."""
    resp = client.post("/match", json={
        "resumeText": "I know basic HTML",
        "jobDescription": "Need Python, Docker, Kubernetes, TensorFlow, and Spring Boot."
    })
    data = resp.get_json()
    assert len(data["learningSuggestions"]) > 0
    assert any("Python" in str(v) for v in data["learningSuggestions"].values())


def test_certification_recommendations(client):
    """Test certification recommendations."""
    resp = client.post("/match", json={
        "resumeText": "I know HTML and CSS",
        "jobDescription": "Need AWS, Docker, Kubernetes, Python, and SQL expertise."
    })
    data = resp.get_json()
    assert len(data["certificationRecommendations"]) > 0


def test_score_range(client):
    """Match score should always be between 0 and 100."""
    resp = client.post("/match", json={
        "resumeText": "A" * 100,
        "jobDescription": "B" * 100
    })
    data = resp.get_json()
    assert 0 <= data["matchScore"] <= 100


def test_perfect_match(client):
    """Test that identical texts produce high match score."""
    text = "Python developer with extensive experience in Django and PostgreSQL."
    resp = client.post("/match", json={
        "resumeText": text,
        "jobDescription": text
    })
    data = resp.get_json()
    assert data["matchScore"] > 90


def test_no_match(client):
    """Test that completely unrelated texts produce low match score."""
    resp = client.post("/match", json={
        "resumeText": "I am a chef specializing in Italian cuisine.",
        "jobDescription": "Senior Java Developer with Spring Boot experience."
    })
    data = resp.get_json()
    assert data["matchScore"] < 50


def test_skill_gap_analysis_no_gap(client):
    """Test skill gap when all skills are matched."""
    resp = client.post("/skill-gap", json={
        "resumeText": "Python developer with Django, PostgreSQL, and Docker.",
        "jobDescription": "Need Python, Django, PostgreSQL, and Docker."
    })
    data = resp.get_json()
    gap = data["skillGap"]
    assert gap["matchRate"] == 100.0
    assert len(gap["skillsMissing"]) == 0


def test_technical_skills_coverage(client):
    """Test that TECHNICAL_SKILLS set contains major technologies."""
    major_skills = ["python", "java", "javascript", "react", "docker",
                    "kubernetes", "aws", "sql", "machine learning"]
    for skill in major_skills:
        assert skill in TECHNICAL_SKILLS, f"Missing skill: {skill}"


def test_model_info_has_predictions(client):
    """Test that model info endpoint works after predictions."""
    client.post("/match", json={
        "resumeText": "Test resume",
        "jobDescription": "Test job"
    })

    resp = client.get("/model-info")
    data = resp.get_json()
    assert data["totalPredictions"] > 0
    assert data["averageScore"] >= 0


def test_cf_interact_invalid(client):
    """Test CF interaction with missing data."""
    resp = client.post("/cf-interact", json={"userId": 1})
    assert resp.status_code == 400


def test_cf_recommend_no_user(client):
    """Test CF recommend for non-existent user."""
    resp = client.post("/cf-recommend", json={"userId": 99999})
    assert resp.status_code == 200
    assert resp.get_json()["recommendations"] == []


def test_cbf_add_invalid(client):
    """Test CBF add with missing jobId."""
    resp = client.post("/cbf-add", json={"title": "Test"})
    assert resp.status_code == 400


def test_cbf_recommend_no_profile(client):
    """Test CBF recommend with no profile text."""
    resp = client.post("/cbf-recommend", json={})
    assert resp.status_code == 400


def test_hybrid_recommend_no_user(client):
    """Test hybrid recommend with no userId."""
    resp = client.post("/hybrid-recommend", json={"profileText": "test"})
    assert resp.status_code == 400


def test_analyze_resume_too_large(client):
    """Test analyze resume with oversized input."""
    huge = "A" * 60000
    resp = client.post("/analyze-resume", json={"resumeText": huge})
    assert resp.status_code == 413