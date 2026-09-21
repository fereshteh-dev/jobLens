package io.github.fereshtehdev.joblens.analysis.application;

import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.GateDecision;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;

/**
 * Result of {@code AnalyzePostingUseCase}. Talking points exist only on the branch where the
 * gate passed - the type system, not a convention, makes "talking points without a passed
 * gate" unrepresentable.
 */
public sealed interface AnalysisOutcome {

    Analysis analysis();

    GateDecision gate();

    record WithTalkingPoints(Analysis analysis, GateDecision gate, TalkingPoints talkingPoints)
            implements AnalysisOutcome {}

    record WithoutTalkingPoints(Analysis analysis, GateDecision gate) implements AnalysisOutcome {}
}
