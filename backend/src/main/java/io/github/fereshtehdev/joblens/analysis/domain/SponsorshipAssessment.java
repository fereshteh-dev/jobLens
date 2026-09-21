package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.Objects;

public record SponsorshipAssessment(SponsorshipLikelihood visa, SponsorshipLikelihood relocation) {

    public SponsorshipAssessment {
        Objects.requireNonNull(visa, "visa must not be null");
        Objects.requireNonNull(relocation, "relocation must not be null");
    }
}
