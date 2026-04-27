package tn.esprit.smartjobboard.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA {@link Specification} helpers for filtered job search.
 */
public final class JobOfferSpecifications {

    private JobOfferSpecifications() {
    }

    public static Specification<JobOffer> visibility(Long userId, String role, Boolean mine) {
        return (root, query, cb) -> {
            boolean clientMine = Boolean.TRUE.equals(mine)
                    && role != null
                    && "CLIENT".equalsIgnoreCase(role.trim())
                    && userId != null;
            if (clientMine) {
                return cb.equal(root.get("clientId"), userId);
            }
            return cb.equal(root.get("status"), JobOfferStatus.PUBLISHED);
        };
    }

    public static Specification<JobOffer> categoryEquals(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase());
        };
    }

    public static Specification<JobOffer> remoteEquals(Boolean remote) {
        return (root, query, cb) -> {
            if (remote == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("remote"), remote);
        };
    }

    public static Specification<JobOffer> locationContains(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%");
        };
    }

    public static Specification<JobOffer> budgetOverlap(BigDecimal filterMin, BigDecimal filterMax) {
        return (root, query, cb) -> {
            if (filterMin == null && filterMax == null) {
                return cb.conjunction();
            }
            List<Predicate> parts = new ArrayList<>();
            if (filterMin != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("budgetMax"), filterMin));
            }
            if (filterMax != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("budgetMin"), filterMax));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
    }

    public static Specification<JobOffer> anySkillMatches(List<String> skills) {
        return (root, query, cb) -> {
            if (skills == null || skills.isEmpty()) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<JobOffer, String> req = root.join("requiredSkills", JoinType.LEFT);
            Join<JobOffer, String> ext = root.join("extractedSkills", JoinType.LEFT);
            List<Predicate> ors = new ArrayList<>();
            for (String raw : skills) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String s = raw.trim();
                ors.add(cb.equal(cb.lower(req), cb.literal(s.toLowerCase())));
                ors.add(cb.equal(cb.lower(ext), cb.literal(s.toLowerCase())));
            }
            if (ors.isEmpty()) {
                return cb.conjunction();
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }
}
