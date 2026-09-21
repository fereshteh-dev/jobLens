package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.Objects;

/**
 * The "high"/"middle" cutoffs from the brief: at or above {@code confidentAt} a probability
 * becomes {@link Verdict.Confident}, at or above {@code uncertainAt} it becomes
 * {@link Verdict.Uncertain}, below that it is {@link Verdict.Ignored}.
 */
public record ThresholdSettings(Probability confidentAt, Probability uncertainAt) {

    public ThresholdSettings {
        Objects.requireNonNull(confidentAt, "confidentAt must not be null");
        Objects.requireNonNull(uncertainAt, "uncertainAt must not be null");
        if (confidentAt.value() < uncertainAt.value()) {
            throw new IllegalArgumentException("confidentAt must be >= uncertainAt");
        }
    }
}
