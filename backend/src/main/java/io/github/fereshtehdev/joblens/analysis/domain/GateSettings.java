package io.github.fereshtehdev.joblens.analysis.domain;

/**
 * Operator-tunable part of the gate (lives in {@code application.yml}). The other input to
 * {@link GatePolicy}, the candidate's target seniority and minimum sponsorship likelihood,
 * is per-candidate and comes from {@code CandidateProfile} instead.
 */
public record GateSettings(double maxRedFlagScore) {

    public GateSettings {
        if (maxRedFlagScore < 0.0) {
            throw new IllegalArgumentException("maxRedFlagScore must not be negative");
        }
    }
}
