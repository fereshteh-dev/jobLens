package io.github.fereshtehdev.joblens.analysis.application.port;

import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;

/** Generates talking points. Called only after the gate has passed. */
public interface TalkingPointsWriter {

    TalkingPoints write(JobPosting posting, Analysis analysis, CandidateProfile profile);
}
