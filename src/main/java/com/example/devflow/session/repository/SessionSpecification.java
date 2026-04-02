package com.example.devflow.session.repository;

import org.springframework.data.jpa.domain.Specification;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.session.entity.SessionStatus;

public class SessionSpecification {

    // Filter by owner — always applied, every query is scoped to the logged-in user
    public static Specification<CodingSession> hasUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    // ILIKE search on title and notes — case insensitive
    // "spring" matches "Spring Boot", "spring security", etc.
    public static Specification<CodingSession> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("notes")), pattern)
            );
        };
    }

    // Filter by status — ACTIVE, PAUSED, or COMPLETED
    public static Specification<CodingSession> hasStatus(SessionStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // Filter by a single tag name — session must have this tag
    // We join to the tags collection and match by name
    public static Specification<CodingSession> hasTag(String tagName) {
        return (root, query, cb) -> {
            query.distinct(true); // prevent duplicate sessions when joining tags
            return cb.equal(root.join("tags").get("name"), tagName.toLowerCase().trim());
        };
    }
}