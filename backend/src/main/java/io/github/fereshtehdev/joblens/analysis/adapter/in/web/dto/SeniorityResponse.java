package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SeniorityAssessment;

public record SeniorityResponse(Seniority claimed, Seniority actual, VerdictResponse titleVsReality) {

    public static SeniorityResponse from(SeniorityAssessment assessment) {
        return new SeniorityResponse(assessment.claimed(), assessment.actual(),
                VerdictResponse.from(assessment.titleVsRealityVerdict()));
    }
}
