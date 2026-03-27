package com.example.devflow.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.devflow.session.entity.CodingSession;

@Repository
public interface CodingSessionRepository extends JpaRepository<CodingSession, Long> {

    List<CodingSession> findByUserId(Long userId);

    Optional<CodingSession> findByIdAndUserId(Long id, Long userId);
}
