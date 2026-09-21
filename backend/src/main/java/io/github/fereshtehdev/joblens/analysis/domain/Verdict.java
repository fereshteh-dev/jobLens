package io.github.fereshtehdev.joblens.analysis.domain;

/**
 * A judgment derived from a probability, already classified against
 * {@link ThresholdSettings}. Callers must handle all three cases explicitly instead of
 * treating an uncertain or ignored signal as a plain boolean.
 */
public sealed interface Verdict {

    record Confident(Probability probability) implements Verdict {}

    record Uncertain(Probability probability) implements Verdict {}

    record Ignored() implements Verdict {}
}
