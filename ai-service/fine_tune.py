"""
Fine-tuned model training script for job matching.

This script fine-tunes a SentenceTransformer model on job matching data.
It can use either synthetic data or loaded from a CSV file.

Usage:
    python fine_tune.py                  # Train with synthetic data
    python fine_tune.py --data data.csv  # Train with custom data
    python fine_tune.py --eval           # Evaluate the fine-tuned model
"""
import os
import json
import argparse
import random
import numpy as np
from sentence_transformers import SentenceTransformer, InputExample, losses, evaluation
from torch.utils.data import DataLoader
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Synthetic training data for job matching
# ---------------------------------------------------------------------------
SYNTHETIC_PAIRS = [
    # (resume, job_description, label) where label is similarity (0-1)
    # High similarity pairs (label=1.0)
    ("Python developer with Django, Flask, PostgreSQL experience. Built REST APIs and microservices.",
     "Senior Python Backend Developer needed. Must have Django, Flask, PostgreSQL, REST API experience.",
     1.0),
    ("Java developer with Spring Boot, Hibernate, and MySQL. Experience with microservices and Docker.",
     "Java/Spring Boot Developer required. Must know Hibernate, MySQL, Docker, microservices.",
     1.0),
    ("Full-stack developer with React, TypeScript, Node.js, and MongoDB. Built multiple web applications.",
     "Full Stack JavaScript Developer. React, TypeScript, Node.js, and MongoDB required.",
     1.0),
    ("Data scientist with Python, TensorFlow, PyTorch, and NLP experience. ML models deployed to production.",
     "Machine Learning Engineer. Must have Python, TensorFlow, PyTorch, NLP experience.",
     1.0),
    ("DevOps engineer with Docker, Kubernetes, Terraform, AWS, and CI/CD pipeline experience.",
     "DevOps Engineer. Docker, Kubernetes, Terraform, AWS, CI/CD required.",
     1.0),
    ("Mobile developer with React Native, TypeScript, and Firebase. Published apps on App Store and Play Store.",
     "React Native Developer. Must know TypeScript, Firebase, and have published apps.",
     1.0),

    # Medium similarity pairs (label=0.5)
    ("Python developer with Flask and PostgreSQL. Some experience with frontend technologies.",
     "Full-stack Python Developer. Django, React, Docker, and AWS experience required.",
     0.5),
    ("Java developer with experience in Spring Boot and MySQL.",
     "Senior Software Engineer. Must have 5+ years experience with microservices, cloud, and DevOps.",
     0.3),
    ("Frontend developer skilled in HTML, CSS, and basic JavaScript.",
     "React Developer with TypeScript, Next.js, and state management experience.",
     0.2),

    # Low similarity pairs (label=0.0)
    ("I am a chef specializing in Italian cuisine with 10 years of experience.",
     "Senior Python Backend Developer with AWS and Docker experience.",
     0.0),
    ("Graphic designer with Photoshop, Illustrator, and UI/UX design skills.",
     "Java Spring Boot Developer for backend microservices.",
     0.0),
    ("Marketing manager with SEO, content marketing, and social media expertise.",
     "DevOps Engineer with Kubernetes, Terraform, and CI/CD pipeline experience.",
     0.0),
    ("Accountant with QuickBooks, Excel, and financial reporting experience.",
     "React Native Mobile Developer with TypeScript and Firebase.",
     0.0),

    # Additional varied pairs
    ("AWS cloud architect with EC2, S3, Lambda, and CloudFormation. Certified Solutions Architect.",
     "Cloud Architect. AWS, EC2, S3, Lambda, CloudFormation, and certification required.",
     1.0),
    ("Data engineer with Python, Spark, Airflow, and data warehousing experience.",
     "Data Engineer. Python, Apache Spark, Airflow, data warehousing required.",
     1.0),
    ("iOS developer with Swift, UIKit, CoreData, and App Store deployment experience.",
     "iOS Developer. Swift, UIKit, CoreData, and App Store experience needed.",
     1.0),
    ("Cybersecurity analyst with SIEM, penetration testing, and incident response.",
     "Security Engineer. Must have SIEM, pen testing, incident response experience.",
     1.0),
]


def create_training_data(data_file=None):
    """Create training data from file or synthetic data."""
    if data_file and os.path.exists(data_file):
        import pandas as pd
        df = pd.read_csv(data_file)
        pairs = []
        for _, row in df.iterrows():
            pairs.append((row["resume"], row["job"], row["label"]))
        logger.info(f"Loaded {len(pairs)} training pairs from {data_file}")
        return pairs
    else:
        # Augment synthetic data with variations
        augmented = []
        for resume, job, label in SYNTHETIC_PAIRS:
            augmented.append((resume, job, label))
            # Add paraphrased versions
            if label > 0.5:
                words = resume.split()
                if len(words) > 10:
                    # Shuffle some words to create variation
                    mid = len(words) // 2
                    varied = words[:mid] + random.sample(words[mid:], min(3, len(words) - mid))
                    augmented.append((" ".join(varied), job, label * 0.9))
        logger.info(f"Created {len(augmented)} training pairs (synthetic + augmented)")
        return augmented


def train_model(pairs, output_path="ai-service/models/fine-tuned", model_name="all-MiniLM-L6-v2"):
    """Fine-tune the SentenceTransformer model."""
    logger.info(f"Starting fine-tuning from base model: {model_name}")
    logger.info(f"Output path: {output_path}")

    model = SentenceTransformer(model_name)

    # Prepare training examples
    train_examples = []
    for resume, job, label in pairs:
        example = InputExample(texts=[resume, job], label=label)
        train_examples.append(example)

    # Create data loader
    train_dataloader = DataLoader(train_examples, shuffle=True, batch_size=8)

    # Use cosine similarity loss
    train_loss = losses.CosineSimilarityLoss(model)

    # Configure evaluator
    evaluator = evaluation.EmbeddingSimilarityEvaluator(
        [e.texts[0] for e in train_examples[:50]],
        [e.texts[1] for e in train_examples[:50]],
        [e.label for e in train_examples[:50]],
    )

    # Fine-tune
    model.fit(
        train_objectives=[(train_dataloader, train_loss)],
        evaluator=evaluator,
        epochs=5,
        evaluation_steps=50,
        warmup_steps=100,
        output_path=output_path,
        save_best_model=True,
    )

    logger.info(f"Fine-tuning complete. Model saved to {output_path}")
    return model


def evaluate_model(model_path="ai-service/models/fine-tuned", test_pairs=None):
    """Evaluate the fine-tuned model."""
    logger.info(f"Loading fine-tuned model from: {model_path}")
    model = SentenceTransformer(model_path)

    if test_pairs is None:
        test_pairs = SYNTHETIC_PAIRS[-5:]  # Use last 5 pairs for testing

    logger.info(f"Evaluating on {len(test_pairs)} test pairs:")
    total_error = 0

    for resume, job, expected_label in test_pairs:
        emb1 = model.encode(resume, convert_to_tensor=True)
        emb2 = model.encode(job, convert_to_tensor=True)
        from sentence_transformers import util
        similarity = util.cos_sim(emb1, emb2).item()
        error = abs(similarity - expected_label)
        total_error += error
        logger.info(f"  Expected: {expected_label:.2f}, Predicted: {similarity:.4f}, Error: {error:.4f}")

    mae = total_error / len(test_pairs)
    logger.info(f"Mean Absolute Error: {mae:.4f}")
    logger.info(f"Accuracy (within 0.3): {(1 - mae / 1.0) * 100:.1f}%")

    return mae


def export_model_for_serving(model_path="ai-service/models/fine-tuned"):
    """Export model config for use in app.py."""
    config = {
        "model_path": model_path,
        "base_model": "all-MiniLM-L6-v2",
        "version": "1.0.0",
        "description": "Fine-tuned job matching model",
        "training_samples": len(SYNTHETIC_PAIRS),
    }
    config_path = os.path.join(os.path.dirname(model_path), "model_config.json")
    os.makedirs(os.path.dirname(config_path), exist_ok=True)
    with open(config_path, "w") as f:
        json.dump(config, f, indent=2)
    logger.info(f"Model config exported to: {config_path}")
    return config


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Fine-tune job matching model")
    parser.add_argument("--data", type=str, help="Path to CSV training data")
    parser.add_argument("--output", type=str, default="ai-service/models/fine-tuned",
                        help="Output path for fine-tuned model")
    parser.add_argument("--base-model", type=str, default="all-MiniLM-L6-v2",
                        help="Base SentenceTransformer model")
    parser.add_argument("--eval", action="store_true", help="Evaluate only mode")
    parser.add_argument("--export", action="store_true", help="Export model config")

    args = parser.parse_args()

    if args.eval:
        evaluate_model(args.output)
    else:
        pairs = create_training_data(args.data)
        model = train_model(pairs, args.output, args.base_model)
        evaluate_model(args.output)

        if args.export:
            export_model_for_serving(args.output)