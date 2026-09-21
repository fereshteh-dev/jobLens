package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.List;
import java.util.Objects;

public record Analysis(SeniorityAssessment seniority, List<RedFlag> redFlags, SponsorshipAssessment sponsorship,
                        String jevModelVersion) {

    public Analysis {
        Objects.requireNonNull(seniority, "seniority must not be null");
        Objects.requireNonNull(redFlags, "redFlags must not be null");
        Objects.requireNonNull(sponsorship, "sponsorship must not be null");
        if (jevModelVersion == null || jevModelVersion.isBlank()) {
            throw new IllegalArgumentException("jevModelVersion must not be blank");
        }
        redFlags = List.copyOf(redFlags);
    }
}
