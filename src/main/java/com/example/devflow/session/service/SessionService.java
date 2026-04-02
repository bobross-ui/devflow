package com.example.devflow.session.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.devflow.exception.EntityNotFoundException;
import com.example.devflow.exception.InvalidStateTransitionException;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.session.entity.SessionStatus;
import com.example.devflow.session.repository.CodingSessionRepository;
import com.example.devflow.session.repository.SessionSpecification;
import com.example.devflow.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final CodingSessionRepository sessionRepository;

    public CodingSession createSession(User user, String title, String notes) {
        CodingSession session = CodingSession.builder()
                .user(user)
                .title(title)
                .notes(notes)
                .status(SessionStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }

    public CodingSession getSessionByIdAndUser(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found or not owned by user"));
    }

    public CodingSession updateSession(Long sessionId, Long userId, String title, String notes) {
        CodingSession session = getSessionByIdAndUser(sessionId, userId);

        if (title != null) {
            session.setTitle(title);
        }
        if (notes != null) {
            session.setNotes(notes);
        }

        return sessionRepository.save(session);
    }

    public void deleteSession(Long sessionId, Long userId) {
        CodingSession session = getSessionByIdAndUser(sessionId, userId);
        sessionRepository.delete(session);
    }

    public CodingSession pauseSession(Long sessionId, Long userId) {
        CodingSession session = getSessionByIdAndUser(sessionId, userId);

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Cannot pause session that is not in ACTIVE state. Current state: " + session.getStatus());
        }

        session.setStatus(SessionStatus.PAUSED);
        return sessionRepository.save(session);
    }

    public CodingSession resumeSession(Long sessionId, Long userId) {
        CodingSession session = getSessionByIdAndUser(sessionId, userId);

        if (session.getStatus() != SessionStatus.PAUSED) {
            throw new InvalidStateTransitionException(
                    "Cannot resume session that is not PAUSED state. Current state: " + session.getStatus());
        }

        session.setStatus(SessionStatus.ACTIVE);
        return sessionRepository.save(session);
    }

    public CodingSession completeSession(Long sessionId, Long userId) {
        CodingSession session = getSessionByIdAndUser(sessionId, userId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Cannot complete a session that is already completed");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());

        long durationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                session.getStartedAt(),
                session.getEndedAt());
        session.setDurationSeconds(durationSeconds);

        return sessionRepository.save(session);
    }

    public Page<CodingSession> listSessions(Long userId, String keyword,
            String tag, SessionStatus status, Pageable pageable) {

        // Always start with user filter — every query is scoped to the logged-in user
        Specification<CodingSession> spec = Specification.where(SessionSpecification.hasUser(userId));

        // Only add each filter if it was actually provided in the request
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(SessionSpecification.hasKeyword(keyword));
        }
        if (tag != null && !tag.isBlank()) {
            spec = spec.and(SessionSpecification.hasTag(tag));
        }
        if (status != null) {
            spec = spec.and(SessionSpecification.hasStatus(status));
        }

        return sessionRepository.findAll(spec, pageable);
    };
}
