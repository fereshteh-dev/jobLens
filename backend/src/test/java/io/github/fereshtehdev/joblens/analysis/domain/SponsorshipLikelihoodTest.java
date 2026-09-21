package io.github.fereshtehdev.joblens.analysis.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SponsorshipLikelihoodTest {

    @Test
    void explicitYesMeetsEveryMinimum() {
        for (SponsorshipLikelihood minimum : SponsorshipLikelihood.values()) {
            assertThat(SponsorshipLikelihood.EXPLICIT_YES.meetsMinimum(minimum)).isTrue();
        }
    }

    @Test
    void explicitNoOnlyMeetsExplicitNoMinimum() {
        assertThat(SponsorshipLikelihood.EXPLICIT_NO.meetsMinimum(SponsorshipLikelihood.EXPLICIT_NO)).isTrue();
        assertThat(SponsorshipLikelihood.EXPLICIT_NO.meetsMinimum(SponsorshipLikelihood.UNKNOWN)).isFalse();
        assertThat(SponsorshipLikelihood.EXPLICIT_NO.meetsMinimum(SponsorshipLikelihood.UNLIKELY)).isFalse();
    }

    @Test
    void unknownRanksAboveExplicitNoAndUnlikelyButBelowLikely() {
        assertThat(SponsorshipLikelihood.UNKNOWN.meetsMinimum(SponsorshipLikelihood.EXPLICIT_NO)).isTrue();
        assertThat(SponsorshipLikelihood.UNKNOWN.meetsMinimum(SponsorshipLikelihood.UNLIKELY)).isTrue();
        assertThat(SponsorshipLikelihood.UNKNOWN.meetsMinimum(SponsorshipLikelihood.LIKELY)).isFalse();
    }

    @Test
    void everyLevelMeetsItsOwnMinimum() {
        for (SponsorshipLikelihood level : SponsorshipLikelihood.values()) {
            assertThat(level.meetsMinimum(level)).isTrue();
        }
    }
}
