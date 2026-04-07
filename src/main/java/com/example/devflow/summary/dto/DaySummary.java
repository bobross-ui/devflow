package com.example.devflow.summary.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DaySummary {
    private LocalDate date;
    private double hours;
}
