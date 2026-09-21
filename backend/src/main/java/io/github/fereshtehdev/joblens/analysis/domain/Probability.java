package io.github.fereshtehdev.joblens.analysis.domain;

public record Probability(double value) {

    public Probability {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Probability must be in [0,1], was " + value);
        }
    }
}
