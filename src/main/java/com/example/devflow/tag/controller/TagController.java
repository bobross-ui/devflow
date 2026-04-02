package com.example.devflow.tag.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.devflow.auth.filter.CustomUserDetails;
import com.example.devflow.session.dto.SessionResponse;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.tag.dto.AddTagRequest;
import com.example.devflow.tag.entity.Tag;
import com.example.devflow.tag.service.TagService;

import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    private SessionResponse toResponse(CodingSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .notes(session.getNotes())
                .status(session.getStatus().toString())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(session.getDurationSeconds())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .tags(session.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @PostMapping("/api/v1/sessions/{id}/tags")
    public ResponseEntity<SessionResponse> addTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody AddTagRequest request) {

        CodingSession session = tagService.addTagToSession(id, userDetails.getUserId(), request.getName());
        return ResponseEntity.ok(toResponse(session));
    }

    @DeleteMapping("/api/v1/sessions/{id}/tags/{tagName}")
    public ResponseEntity<Void> removeTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @PathVariable String tagName) {

        tagService.removeTagFromSession(id, userDetails.getUserId(), tagName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/tags")
    public ResponseEntity<Set<Tag>> getUserTags(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Set<Tag> tags = tagService.getTagsForUser(userDetails.getUserId());
        return ResponseEntity.ok(tags);
    }

}
