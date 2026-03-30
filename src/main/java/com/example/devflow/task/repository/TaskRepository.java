package com.example.devflow.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.devflow.task.entity.Task;
import java.util.List;
import java.util.Optional;


@Repository
public interface TaskRepository extends JpaRepository<Task, Long>{

    List<Task> findBySessionId(Long sessionId);

    Optional<Task> findByIdAndSessionId(Long id, Long sessionId);
}
