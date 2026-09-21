package io.github.fereshtehdev.joblens.analysis.adapter.out.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.fereshtehdev.joblens.analysis.application.port.AnalysisCache;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.PostingFingerprint;
import io.github.fereshtehdev.joblens.config.properties.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Bounded, expiring replacement for Phase 2's {@code InMemoryAnalysisCache}. Hit/miss/size stats exposed via Micrometer. */
@Component
public class CaffeineAnalysisCache implements AnalysisCache {

    private final Cache<PostingFingerprint, Analysis> cache;

    public CaffeineAnalysisCache(CacheProperties properties, MeterRegistry meterRegistry) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.maxSize())
                .expireAfterWrite(properties.expireAfterWrite())
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, "analysis");
    }

    @Override
    public Optional<Analysis> get(PostingFingerprint key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    @Override
    public void put(PostingFingerprint key, Analysis analysis) {
        cache.put(key, analysis);
    }
}
