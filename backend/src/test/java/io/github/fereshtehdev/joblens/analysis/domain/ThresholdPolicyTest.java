package io.github.fereshtehdev.joblens.analysis.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdPolicyTest {

    private final ThresholdSettings settings = new ThresholdSettings(new Probability(0.8), new Probability(0.4));

    @Test
    void atOrAboveConfidentAtIsConfident() {
        assertThat(ThresholdPolicy.toVerdict(new Probability(0.8), settings))
                .isEqualTo(new Verdict.Confident(new Probability(0.8)));
        assertThat(ThresholdPolicy.toVerdict(new Probability(0.95), settings))
                .isInstanceOf(Verdict.Confident.class);
    }

    @Test
    void betweenUncertainAtAndConfidentAtIsUncertain() {
        assertThat(ThresholdPolicy.toVerdict(new Probability(0.4), settings))
                .isEqualTo(new Verdict.Uncertain(new Probability(0.4)));
        assertThat(ThresholdPolicy.toVerdict(new Probability(0.6), settings))
                .isInstanceOf(Verdict.Uncertain.class);
    }

    @Test
    void belowUncertainAtIsIgnored() {
        assertThat(ThresholdPolicy.toVerdict(new Probability(0.39999), settings))
                .isEqualTo(new Verdict.Ignored());
        assertThat(ThresholdPolicy.toVerdict(new Probability(0.0), settings))
                .isInstanceOf(Verdict.Ignored.class);
    }

    @Test
    void thresholdSettingsRejectInvertedCutoffs() {
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new ThresholdSettings(new Probability(0.3), new Probability(0.5)));
    }
}
