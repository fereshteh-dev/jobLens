package io.github.fereshtehdev.joblens.analysis.application.port;

/**
 * Thrown by a {@link TalkingPointsWriter} when it cannot produce talking points after
 * exhausting its own retry/circuit-breaker policy. {@code AnalyzePostingUseCase} catches
 * exactly this type and degrades to {@code AnalysisOutcome.WithoutTalkingPoints} - per
 * ADR-0003, an LLM failure must never fail the whole request, only omit talking points.
 */
public class TalkingPointsUnavailableException extends RuntimeException {

    public TalkingPointsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
