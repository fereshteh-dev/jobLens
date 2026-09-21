package io.github.fereshtehdev.joblens.analysis.application.port;

/**
 * Thrown by a {@link PostingClassifier} when it cannot classify a posting - after exhausting
 * its own retry/circuit-breaker policy, or because Jev's response could not be interpreted.
 * Unlike {@link TalkingPointsUnavailableException}, this is deliberately never caught by
 * {@code AnalyzePostingUseCase}: per ADR-0003/CLAUDE.md, if Jev is down the request must fail
 * with a clear error, never silently fall back to anything. Mapped to a 502 by
 * {@code ProblemDetailHandler}.
 */
public class ClassificationUnavailableException extends RuntimeException {

    public ClassificationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
