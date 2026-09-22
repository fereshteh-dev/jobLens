package io.github.fereshtehdev.joblens.analysis.domain;

import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure mapping from an {@link Analysis} to a {@link GateDecision}. No I/O, no framework
 * types. This is the single gate that decides whether the LLM ever runs for a posting.
 */
public final class GatePolicy {

    private GatePolicy() {
    }

    public static GateDecision evaluate(Analysis analysis, CandidateProfile profile, GateSettings settings) {
        List<String> reasons = new ArrayList<>();

        if (analysis.seniority().actual() != profile.targetSeniority()) {
            reasons.add("Actual seniority %s does not match target %s"
                    .formatted(analysis.seniority().actual(), profile.targetSeniority()));
        }

        double redFlagScore = analysis.redFlags().stream()
                .mapToDouble(flag -> contribution(flag.verdict()))
                .sum();
        if (redFlagScore > settings.maxRedFlagScore()) {
            reasons.add("Combined red-flag score %.2f exceeds limit %.2f"
                    .formatted(redFlagScore, settings.maxRedFlagScore()));
        }

        // UNKNOWN as a minimum means "I don't need this, don't gate on it" (see
        // candidate-profile.example.yml) - not "reject anything ranked below UNKNOWN".
        // Ranked comparison only applies once the candidate has an actual requirement.
        if (profile.minimumSponsorshipLikelihood() != SponsorshipLikelihood.UNKNOWN) {
            if (!analysis.sponsorship().visa().meetsMinimum(profile.minimumSponsorshipLikelihood())) {
                reasons.add("Visa sponsorship likelihood %s is below minimum %s"
                        .formatted(analysis.sponsorship().visa(), profile.minimumSponsorshipLikelihood()));
            }
            if (!analysis.sponsorship().relocation().meetsMinimum(profile.minimumSponsorshipLikelihood())) {
                reasons.add("Relocation support likelihood %s is below minimum %s"
                        .formatted(analysis.sponsorship().relocation(), profile.minimumSponsorshipLikelihood()));
            }
        }

        return reasons.isEmpty() ? GateDecision.passing() : GateDecision.failed(List.copyOf(reasons));
    }

    private static double contribution(Verdict verdict) {
        return switch (verdict) {
            case Verdict.Confident(Probability p) -> p.value();
            case Verdict.Uncertain(Probability p) -> p.value();
            case Verdict.Ignored ignored -> 0.0;
        };
    }
}
