package io.github.fereshtehdev.joblens.config;

import io.github.fereshtehdev.joblens.analysis.application.AnalyzePostingUseCase;
import io.github.fereshtehdev.joblens.analysis.application.port.AnalysisCache;
import io.github.fereshtehdev.joblens.analysis.application.port.PostingClassifier;
import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsWriter;
import io.github.fereshtehdev.joblens.analysis.domain.GateSettings;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.ThresholdSettings;
import io.github.fereshtehdev.joblens.config.properties.GateProperties;
import io.github.fereshtehdev.joblens.config.properties.ThresholdProperties;
import io.github.fereshtehdev.joblens.profile.adapter.out.config.CandidateProfileProperties;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the pure domain/application types to their Spring-configured inputs. */
@Configuration
public class AnalysisConfig {

    @Bean
    GateSettings gateSettings(GateProperties properties) {
        return new GateSettings(properties.maxRedFlagScore());
    }

    @Bean
    ThresholdSettings thresholdSettings(ThresholdProperties properties) {
        return new ThresholdSettings(new Probability(properties.confidentAt()), new Probability(properties.uncertainAt()));
    }

    @Bean
    CandidateProfile candidateProfile(CandidateProfileProperties properties) {
        return new CandidateProfile(properties.targetSeniority(), properties.minimumSponsorshipLikelihood());
    }

    @Bean
    AnalyzePostingUseCase analyzePostingUseCase(PostingClassifier classifier, TalkingPointsWriter writer,
                                                 AnalysisCache cache, GateSettings gateSettings,
                                                 MeterRegistry meterRegistry) {
        return new AnalyzePostingUseCase(classifier, writer, cache, gateSettings, meterRegistry);
    }
}
