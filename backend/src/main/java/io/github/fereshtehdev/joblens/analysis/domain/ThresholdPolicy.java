package io.github.fereshtehdev.joblens.analysis.domain;

/** Pure mapping from a raw probability to a {@link Verdict}. No I/O, no framework types. */
public final class ThresholdPolicy {

    private ThresholdPolicy() {
    }

    public static Verdict toVerdict(Probability probability, ThresholdSettings settings) {
        if (probability.value() >= settings.confidentAt().value()) {
            return new Verdict.Confident(probability);
        }
        if (probability.value() >= settings.uncertainAt().value()) {
            return new Verdict.Uncertain(probability);
        }
        return new Verdict.Ignored();
    }
}
