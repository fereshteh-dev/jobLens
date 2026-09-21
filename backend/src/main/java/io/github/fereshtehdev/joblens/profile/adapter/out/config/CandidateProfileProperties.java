package io.github.fereshtehdev.joblens.profile.adapter.out.config;

import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Spring-aware twin of {@code CandidateProfile}, bound from {@code profile/candidate-profile.yml}
 * (git-ignored; see {@code profile/candidate-profile.example.yml} and CLAUDE.md). Kept separate
 * from the domain record so the domain package stays free of Spring annotations.
 */
@Validated
@ConfigurationProperties(prefix = "joblens.candidate-profile")
public record CandidateProfileProperties(
        @NotNull Seniority targetSeniority,
        @NotNull SponsorshipLikelihood minimumSponsorshipLikelihood) {
}
