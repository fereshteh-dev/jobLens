package io.github.fereshtehdev.joblens.analysis.adapter.out.mock;

import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsWriter;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Offline stand-in for the real Spring AI adapter (Phase 3). Canned, not generated. Active
 * whenever {@code joblens.ai.enabled=false} (the default) - see SpringAiTalkingPointsWriter.
 */
@Component
@ConditionalOnProperty(prefix = "joblens.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubTalkingPointsWriter implements TalkingPointsWriter {

    @Override
    public TalkingPoints write(JobPosting posting, Analysis analysis, CandidateProfile profile) {
        return new TalkingPoints(
                List.of("Connect your experience to the %s role at %s.".formatted(posting.title(), posting.company())),
                List.of("What does success in this role look like after six months?"),
                List.of("This posting's assessed seniority matches your target of %s.".formatted(profile.targetSeniority())));
    }
}
