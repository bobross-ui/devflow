package com.example.devflow.summary.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TagSummary {
    private String tag;
    private double hours;
}
