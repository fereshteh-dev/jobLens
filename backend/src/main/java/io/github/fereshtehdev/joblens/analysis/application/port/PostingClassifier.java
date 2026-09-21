package io.github.fereshtehdev.joblens.analysis.application.port;

import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;

/** Classifies a posting. Always called. Implementations must never generate free text. */
public interface PostingClassifier {

    Analysis classify(JobPosting posting);
}
