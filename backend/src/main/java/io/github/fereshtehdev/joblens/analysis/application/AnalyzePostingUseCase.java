package io.github.fereshtehdev.joblens.analysis.application;

import io.github.fereshtehdev.joblens.analysis.application.port.AnalysisCache;
import io.github.fereshtehdev.joblens.analysis.application.port.PostingClassifier;
import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsUnavailableException;
import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsWriter;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.GateDecision;
import io.github.fereshtehdev.joblens.analysis.domain.GatePolicy;
import io.github.fereshtehdev.joblens.analysis.domain.GateSettings;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.PostingFingerprint;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

/**
 * Orchestrates one analysis: fingerprint, cache lookup, classification, gate evaluation, and
 * - only if the gate passes - talking points. See ADR-0003: {@code writer} must never be
 * invoked outside the {@code gate.passed()} branch below. A {@code TalkingPointsWriter}
 * failure (see ADR-0005) degrades to {@code WithoutTalkingPoints} rather than failing the
 * whole request; a {@code PostingClassifier} failure is deliberately left uncaught.
 */
public final class AnalyzePostingUseCase {

    private final PostingClassifier classifier;
    private final TalkingPointsWriter writer;
    private final AnalysisCache cache;
    private final GateSettings gateSettings;
    private final MeterRegistry meterRegistry;

    public AnalyzePostingUseCase(PostingClassifier classifier, TalkingPointsWriter writer, AnalysisCache cache,
                                  GateSettings gateSettings, MeterRegistry meterRegistry) {
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.writer = Objects.requireNonNull(writer, "writer must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
        this.gateSettings = Objects.requireNonNull(gateSettings, "gateSettings must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public AnalysisOutcome analyze(JobPosting posting, CandidateProfile profile) {
        PostingFingerprint fingerprint = PostingFingerprint.of(posting);
        Analysis analysis = cache.get(fingerprint).orElseGet(() -> classifyAndCache(posting, fingerprint));

        GateDecision gate = GatePolicy.evaluate(analysis, profile, gateSettings);
        meterRegistry.counter("joblens.gate.decisions", "result", gate.passed() ? "passed" : "failed").increment();

        if (!gate.passed()) {
            return new AnalysisOutcome.WithoutTalkingPoints(analysis, gate);
        }

        try {
            TalkingPoints talkingPoints = writer.write(posting, analysis, profile);
            return new AnalysisOutcome.WithTalkingPoints(analysis, gate, talkingPoints);
        } catch (TalkingPointsUnavailableException e) {
            return new AnalysisOutcome.WithoutTalkingPoints(analysis, gate);
        }
    }

    private Analysis classifyAndCache(JobPosting posting, PostingFingerprint fingerprint) {
        Analysis fresh = classifier.classify(posting);
        cache.put(fingerprint, fresh);
        return fresh;
    }
}
