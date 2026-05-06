package com.jobmatch.backend.specification;

import com.jobmatch.backend.entity.Job;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class JobSpecification {

    private JobSpecification() {
    }

    public static Specification<Job> filter(String location, String salary, String skills, String query) {
        return (Root<Job> root, CriteriaQuery<?> queryRoot, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(location)) {
                predicates.add(builder.like(
                        builder.lower(root.get("location")),
                        "%%%s%%".formatted(location.trim().toLowerCase())
                ));
            }

            if (StringUtils.hasText(salary)) {
                predicates.add(builder.like(
                        builder.lower(root.get("salary")),
                        "%%%s%%".formatted(salary.trim().toLowerCase())
                ));
            }

            if (StringUtils.hasText(skills)) {
                String[] parts = skills.split(",");
                List<Predicate> skillPredicates = new ArrayList<>();
                for (String part : parts) {
                    String value = part.trim().toLowerCase();
                    if (!value.isEmpty()) {
                        skillPredicates.add(builder.like(
                                builder.lower(root.get("skills")),
                                "%%%s%%".formatted(value)
                        ));
                    }
                }
                if (!skillPredicates.isEmpty()) {
                    predicates.add(builder.or(skillPredicates.toArray(new Predicate[0])));
                }
            }

            if (StringUtils.hasText(query)) {
                String normalized = "%%%s%%".formatted(query.trim().toLowerCase());
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), normalized),
                        builder.like(builder.lower(root.get("company")), normalized),
                        builder.like(builder.lower(root.get("description")), normalized),
                        builder.like(builder.lower(root.get("skills")), normalized)
                ));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
