package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;

public record SponsorshipResponse(SponsorshipLikelihood visa, SponsorshipLikelihood relocation) {

    public static SponsorshipResponse from(SponsorshipAssessment assessment) {
        return new SponsorshipResponse(assessment.visa(), assessment.relocation());
    }
}
