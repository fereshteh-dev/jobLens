package io.github.fereshtehdev.joblens.analysis.application.port;

import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.PostingFingerprint;

import java.util.Optional;

public interface AnalysisCache {

    Optional<Analysis> get(PostingFingerprint key);

    void put(PostingFingerprint key, Analysis analysis);
}
