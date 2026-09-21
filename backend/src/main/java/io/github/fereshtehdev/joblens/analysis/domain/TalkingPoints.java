package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.List;
import java.util.Objects;

public record TalkingPoints(List<String> coverLetterAngles, List<String> questionsToAsk, List<String> whyItFits) {

    public TalkingPoints {
        Objects.requireNonNull(coverLetterAngles, "coverLetterAngles must not be null");
        Objects.requireNonNull(questionsToAsk, "questionsToAsk must not be null");
        Objects.requireNonNull(whyItFits, "whyItFits must not be null");
        coverLetterAngles = List.copyOf(coverLetterAngles);
        questionsToAsk = List.copyOf(questionsToAsk);
        whyItFits = List.copyOf(whyItFits);
    }
}
