package io.github.fereshtehdev.joblens.profile.domain;

import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;

import java.util.Objects;

/**
 * The candidate's own gate inputs: the seniority they're targeting, and how much they
 * personally need visa sponsorship / relocation support to be likely. Loaded from a
 * git-ignored file (see CLAUDE.md); this type doesn't know or care how.
 */
public record CandidateProfile(Seniority targetSeniority, SponsorshipLikelihood minimumSponsorshipLikelihood) {

    public CandidateProfile {
        Objects.requireNonNull(targetSeniority, "targetSeniority must not be null");
        Objects.requireNonNull(minimumSponsorshipLikelihood, "minimumSponsorshipLikelihood must not be null");
    }
}
