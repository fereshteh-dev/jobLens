package io.github.fereshtehdev.joblens.analysis.application;

import io.github.fereshtehdev.joblens.analysis.application.port.AnalysisCache;
import io.github.fereshtehdev.joblens.analysis.application.port.PostingClassifier;
import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsUnavailableException;
import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsWriter;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.GateSettings;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.PostingFingerprint;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;
import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SeniorityAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyzePostingUseCaseTest {

    private static final JobPosting POSTING =
            new JobPosting("https://example.com/job", "Senior Engineer", "Acme", "Remote", "Great role.");
    private static final GateSettings GATE_SETTINGS = new GateSettings(1.5);

    @Test
    void gatePassing_callsWriterAndReturnsTalkingPoints() {
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);
        FakeClassifier classifier = new FakeClassifier(matchingAnalysis());
        FakeWriter writer = new FakeWriter();
        AnalyzePostingUseCase useCase = useCase(classifier, writer, new FakeCache(), new SimpleMeterRegistry());

        AnalysisOutcome outcome = useCase.analyze(POSTING, profile);

        assertThat(outcome).isInstanceOf(AnalysisOutcome.WithTalkingPoints.class);
        assertThat(outcome.gate().passed()).isTrue();
        assertThat(writer.callCount).isEqualTo(1);
    }

    @Test
    void gateFailing_neverCallsWriter() {
        CandidateProfile profile = new CandidateProfile(Seniority.STAFF_PLUS, SponsorshipLikelihood.UNLIKELY);
        FakeClassifier classifier = new FakeClassifier(matchingAnalysis());
        FakeWriter writer = new FakeWriter();
        AnalyzePostingUseCase useCase = useCase(classifier, writer, new FakeCache(), new SimpleMeterRegistry());

        AnalysisOutcome outcome = useCase.analyze(POSTING, profile);

        assertThat(outcome).isInstanceOf(AnalysisOutcome.WithoutTalkingPoints.class);
        assertThat(outcome.gate().passed()).isFalse();
        assertThat(writer.callCount).isZero();
    }

    @Test
    void cacheHit_neverCallsClassifierAgain() {
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);
        FakeClassifier classifier = new FakeClassifier(matchingAnalysis());
        AnalyzePostingUseCase useCase = useCase(classifier, new FakeWriter(), new FakeCache(), new SimpleMeterRegistry());

        useCase.analyze(POSTING, profile);
        useCase.analyze(POSTING, profile);

        assertThat(classifier.callCount).isEqualTo(1);
    }

    @Test
    void classifyingAPosting_populatesTheCache() {
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);
        FakeClassifier classifier = new FakeClassifier(matchingAnalysis());
        FakeCache cache = new FakeCache();
        AnalyzePostingUseCase useCase = useCase(classifier, new FakeWriter(), cache, new SimpleMeterRegistry());

        useCase.analyze(POSTING, profile);

        assertThat(cache.get(PostingFingerprint.of(POSTING))).isPresent();
    }

    @Test
    void writerUnavailable_degradesGracefullyWithoutFailingTheRequest() {
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);
        FakeClassifier classifier = new FakeClassifier(matchingAnalysis());
        FakeWriter writer = new FakeWriter();
        writer.throwUnavailable = true;
        AnalyzePostingUseCase useCase = useCase(classifier, writer, new FakeCache(), new SimpleMeterRegistry());

        AnalysisOutcome outcome = useCase.analyze(POSTING, profile);

        assertThat(outcome).isInstanceOf(AnalysisOutcome.WithoutTalkingPoints.class);
        assertThat(outcome.gate().passed()).isTrue();
    }

    @Test
    void gateDecision_isRecordedAsAMetric() {
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);
        FakeClassifier classifier = new FakeClassifier(matchingAnalysis());
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AnalyzePostingUseCase useCase = useCase(classifier, new FakeWriter(), new FakeCache(), meterRegistry);

        useCase.analyze(POSTING, profile);

        assertThat(meterRegistry.counter("joblens.gate.decisions", "result", "passed").count()).isEqualTo(1.0);
    }

    private static AnalyzePostingUseCase useCase(PostingClassifier classifier, TalkingPointsWriter writer,
                                                  AnalysisCache cache, MeterRegistry meterRegistry) {
        return new AnalyzePostingUseCase(classifier, writer, cache, GATE_SETTINGS, meterRegistry);
    }

    private static Analysis matchingAnalysis() {
        return new Analysis(
                new SeniorityAssessment(Seniority.SENIOR, Seniority.SENIOR, new Verdict.Confident(new Probability(0.9))),
                List.of(new RedFlag(RedFlagKind.HIDDEN_OVERTIME, new Verdict.Ignored())),
                new SponsorshipAssessment(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.LIKELY),
                "jev-1.13.0");
    }

    private static final class FakeClassifier implements PostingClassifier {
        private final Analysis toReturn;
        int callCount = 0;

        FakeClassifier(Analysis toReturn) {
            this.toReturn = toReturn;
        }

        @Override
        public Analysis classify(JobPosting posting) {
            callCount++;
            return toReturn;
        }
    }

    private static final class FakeWriter implements TalkingPointsWriter {
        int callCount = 0;
        boolean throwUnavailable = false;

        @Override
        public TalkingPoints write(JobPosting posting, Analysis analysis, CandidateProfile profile) {
            callCount++;
            if (throwUnavailable) {
                throw new TalkingPointsUnavailableException("simulated LLM failure", new RuntimeException("boom"));
            }
            return new TalkingPoints(List.of("angle"), List.of("question"), List.of("fit"));
        }
    }

    private static final class FakeCache implements AnalysisCache {
        private final Map<PostingFingerprint, Analysis> store = new HashMap<>();

        @Override
        public Optional<Analysis> get(PostingFingerprint key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void put(PostingFingerprint key, Analysis analysis) {
            store.put(key, analysis);
        }
    }
}
