package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.Objects;

public record SeniorityAssessment(Seniority claimed, Seniority actual, Verdict titleVsRealityVerdict) {

    public SeniorityAssessment {
        Objects.requireNonNull(claimed, "claimed must not be null");
        Objects.requireNonNull(actual, "actual must not be null");
        Objects.requireNonNull(titleVsRealityVerdict, "titleVsRealityVerdict must not be null");
    }
}
