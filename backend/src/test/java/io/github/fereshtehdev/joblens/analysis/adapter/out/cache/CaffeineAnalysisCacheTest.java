package io.github.fereshtehdev.joblens.analysis.adapter.out.cache;

import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.PostingFingerprint;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;
import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SeniorityAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;
import io.github.fereshtehdev.joblens.config.properties.CacheProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaffeineAnalysisCacheTest {

    @Test
    void missReturnsEmpty() {
        CaffeineAnalysisCache cache = newCache();

        assertThat(cache.get(new PostingFingerprint("a"))).isEmpty();
    }

    @Test
    void putThenGetReturnsTheStoredValue() {
        CaffeineAnalysisCache cache = newCache();
        Analysis analysis = analysis();
        PostingFingerprint key = new PostingFingerprint("a");

        cache.put(key, analysis);

        assertThat(cache.get(key)).contains(analysis);
    }

    @Test
    void distinctKeysAreIndependent() {
        CaffeineAnalysisCache cache = newCache();
        cache.put(new PostingFingerprint("a"), analysis());

        assertThat(cache.get(new PostingFingerprint("b"))).isEmpty();
    }

    private static CaffeineAnalysisCache newCache() {
        return new CaffeineAnalysisCache(new CacheProperties(100, Duration.ofMinutes(10)), new SimpleMeterRegistry());
    }

    private static Analysis analysis() {
        return new Analysis(
                new SeniorityAssessment(Seniority.SENIOR, Seniority.SENIOR, new Verdict.Confident(new Probability(0.9))),
                List.of(new RedFlag(RedFlagKind.HIDDEN_OVERTIME, new Verdict.Ignored())),
                new SponsorshipAssessment(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.LIKELY),
                "mock-classifier-0");
    }
}
