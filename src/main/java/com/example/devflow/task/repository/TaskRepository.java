package com.example.devflow.task.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.devflow.task.entity.Task;
import java.util.List;
import java.util.Optional;


@Repository
public interface TaskRepository extends JpaRepository<Task, Long>{

    List<Task> findBySessionId(Long sessionId);

    Page<Task> findBySessionId(Long sessionId, Pageable pageable);

    Optional<Task> findByIdAndSessionId(Long id, Long sessionId);
}
