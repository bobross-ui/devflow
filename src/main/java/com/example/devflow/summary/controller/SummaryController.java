package com.example.devflow.summary.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.devflow.auth.filter.CustomUserDetails;
import com.example.devflow.summary.dto.SummaryResponse;
import com.example.devflow.summary.service.SummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {

        SummaryResponse summary = summaryService.getSummary(userDetails.getUserId());

        return ResponseEntity.ok(summary);
    }
}
