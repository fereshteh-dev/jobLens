package io.github.fereshtehdev.joblens.analysis.adapter.out.mock;

import io.github.fereshtehdev.joblens.analysis.application.port.PostingClassifier;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.ClaimedSeniorityParser;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;
import io.github.fereshtehdev.joblens.analysis.domain.Seniority;
import io.github.fereshtehdev.joblens.analysis.domain.SeniorityAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipAssessment;
import io.github.fereshtehdev.joblens.analysis.domain.SponsorshipLikelihood;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Offline stand-in for the real Jev adapter (Phase 4). Deterministic keyword heuristics only
 * - good enough to exercise the web adapter end to end, not a real classifier. Never used to
 * generate free text; still returns typed, verdict-wrapped judgments like the real thing will.
 * Active whenever {@code joblens.jev.enabled=false} (the default) - see JevPostingClassifier.
 */
@Component
@ConditionalOnProperty(prefix = "joblens.jev", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MockPostingClassifier implements PostingClassifier {

    private static final String MOCK_MODEL_VERSION = "mock-classifier-0";

    @Override
    public Analysis classify(JobPosting posting) {
        Seniority claimed = ClaimedSeniorityParser.parse(posting.title());
        SeniorityAssessment seniority =
                new SeniorityAssessment(claimed, claimed, new Verdict.Confident(new Probability(0.9)));

        List<RedFlag> redFlags = List.of(
                new RedFlag(RedFlagKind.NO_COMPENSATION_INFO, compensationVerdict(posting.descriptionText())),
                new RedFlag(RedFlagKind.HIDDEN_OVERTIME, new Verdict.Ignored()),
                new RedFlag(RedFlagKind.VAGUE_SCOPE, new Verdict.Ignored()),
                new RedFlag(RedFlagKind.TOO_MANY_ROLES, new Verdict.Ignored()),
                new RedFlag(RedFlagKind.UNREALISTIC_REQUIREMENTS, new Verdict.Ignored()),
                new RedFlag(RedFlagKind.HIGH_TURNOVER_SIGNALS, new Verdict.Ignored()));

        SponsorshipAssessment sponsorship =
                new SponsorshipAssessment(SponsorshipLikelihood.UNKNOWN, SponsorshipLikelihood.UNKNOWN);

        return new Analysis(seniority, redFlags, sponsorship, MOCK_MODEL_VERSION);
    }

    private static Verdict compensationVerdict(String descriptionText) {
        String lower = descriptionText.toLowerCase(Locale.ROOT);
        boolean mentionsCompensation = lower.contains("$") || lower.contains("salary") || lower.contains("compensation");
        return mentionsCompensation ? new Verdict.Ignored() : new Verdict.Confident(new Probability(0.8));
    }
}
