package com.example.devflow.summary.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummaryResponse {
    private Integer weeklyGoalHours;
    private double thisWeekHours;
    private double lastWeekHours;
    private int goalPercentComplete;
    private int currentStreakDays;
    private List<TagSummary> byTag;
    private List<DaySummary> byDay;
}
