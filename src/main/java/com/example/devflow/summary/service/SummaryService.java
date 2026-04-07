package com.example.devflow.summary.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.devflow.exception.EntityNotFoundException;
import com.example.devflow.session.entity.CodingSession;
import com.example.devflow.session.entity.SessionStatus;
import com.example.devflow.session.repository.CodingSessionRepository;
import com.example.devflow.summary.dto.DaySummary;
import com.example.devflow.summary.dto.SummaryResponse;
import com.example.devflow.summary.dto.TagSummary;
import com.example.devflow.tag.entity.Tag;
import com.example.devflow.user.entity.User;
import com.example.devflow.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {

        private final CodingSessionRepository sessionRepository;
        private final UserService userService;

        public SummaryResponse getSummary(Long userId) {
                User user = userService.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found"));

                List<CodingSession> completedSessions = sessionRepository.findByUserId(userId)
                                .stream()
                                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                                .collect(Collectors.toList());

                LocalDate today = LocalDate.now();

                // Start of this week - Monday
                LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
                // Start of last week - Monday of previous week
                LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
                LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

                // Sessions completed this week
                List<CodingSession> thisWeekSessions = completedSessions.stream()
                                .filter(s -> !s.getEndedAt().toLocalDate().isBefore(thisWeekStart))
                                .collect(Collectors.toList());

                // Sessions completed last week
                List<CodingSession> lastWeekSessions = completedSessions.stream()
                                .filter(s -> {
                                        LocalDate endDate = s.getEndedAt().toLocalDate();
                                        return !endDate.isBefore(lastWeekStart) && !endDate.isAfter(lastWeekEnd);
                                })
                                .collect(Collectors.toList());

                double thisWeekHours = totalHours(thisWeekSessions);
                double lastWeekHours = totalHours(lastWeekSessions);

                int goalHours = user.getWeeklyGoalHours();
                int goalPercent = goalHours > 0
                                ? (int) Math.min(100, (thisWeekHours / goalHours) * 100)
                                : 0;

                return SummaryResponse.builder()
                                .weeklyGoalHours(goalHours)
                                .thisWeekHours(round(thisWeekHours))
                                .lastWeekHours(round(lastWeekHours))
                                .goalPercentComplete(goalPercent)
                                .currentStreakDays(calculateStreak(completedSessions, today))
                                .byTag(calculateByTag(completedSessions))
                                .byDay(calculateByDay(completedSessions, today))
                                .build();

        }

        private int calculateStreak(List<CodingSession> sessions, LocalDate today) {
                int streak = 0;
                LocalDate current = today;

                while (true) {
                        LocalDate checkDate = current;
                        boolean hasSession = sessions.stream()
                                        .anyMatch(s -> s.getEndedAt().toLocalDate().equals(checkDate));
                        if (!hasSession)
                                break;

                        streak++;
                        current = current.minusDays(1);
                }

                return streak;
        }

        // Sum durations across sessions
        private double totalHours(List<CodingSession> sessions) {
                return sessions.stream()
                                .mapToLong(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                                .sum() / 3600.0;
        }

        // Group sessions by tag, sum hours per tag, sort by hours descending
        private List<TagSummary> calculateByTag(List<CodingSession> sessions) {
                Map<String, Double> hoursByTag = new HashMap<>();

                for (CodingSession session : sessions) {
                        double hours = session.getDurationSeconds() != null
                                        ? session.getDurationSeconds() / 3600.0
                                        : 0;
                        for (Tag tag : session.getTags()) {
                                hoursByTag.merge(tag.getName(), hours, Double::sum);
                        }
                }

                return hoursByTag.entrySet().stream()
                                .map(e -> TagSummary.builder()
                                                .tag(e.getKey())
                                                .hours(round(e.getValue()))
                                                .build())
                                .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
                                .collect(Collectors.toList());
        }

        // Build a list of {date, hours} for the last 7 days including today
        private List<DaySummary> calculateByDay(List<CodingSession> sessions, LocalDate today) {
                List<DaySummary> byDay = new ArrayList<>();

                for (int i = 6; i >= 0; i--) {
                        LocalDate date = today.minusDays(i);
                        double hours = sessions.stream()
                                        .filter(s -> s.getEndedAt().toLocalDate().equals(date))
                                        .mapToLong(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
                                        .sum() / 3600.0;

                        byDay.add(DaySummary.builder()
                                        .date(date)
                                        .hours(round(hours))
                                        .build());
                }

                return byDay;
        }

        private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
        }
}
