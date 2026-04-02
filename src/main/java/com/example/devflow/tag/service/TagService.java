package com.example.devflow.tag.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.devflow.exception.EntityNotFoundException;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.session.repository.CodingSessionRepository;
import com.example.devflow.tag.entity.Tag;
import com.example.devflow.tag.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final CodingSessionRepository sessionRepository;

    public CodingSession addTagToSession(Long sessionId, Long userId, String tagName) {
        CodingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found or not owned by user"));

        String normalizedName = tagName.toLowerCase().trim();

        // Reuse existing tag or create new one — this keeps tags unique in the tag table
        Tag tag = tagRepository.findByName(normalizedName)
            .orElseGet(() -> tagRepository.save(Tag.builder().name(normalizedName).build()));

        // HashSet handles duplicates automatically
        session.getTags().add(tag);

        return sessionRepository.save(session);
    }

    public CodingSession removeTagFromSession(Long sessionId, Long userId, String tagName) {
        CodingSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found or not owned by user"));

        String normalizedName = tagName.toLowerCase().trim();

        // Remove from the set — JPA will delete the row from coding_session_tags join table
        // The tag entity itself is NOT deleted, other sessions may still use it
        session.getTags().removeIf(tag -> tag.getName().equals(normalizedName));

        return sessionRepository.save(session);
    }

    public Set<Tag> getTagsForUser(Long userId) {

        return sessionRepository.findByUserId(userId).stream()
                .flatMap(session -> session.getTags().stream())
                .collect(Collectors.toSet());
    }
}
