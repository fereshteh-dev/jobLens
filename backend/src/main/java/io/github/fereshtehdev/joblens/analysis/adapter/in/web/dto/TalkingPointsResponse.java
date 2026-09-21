package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import io.github.fereshtehdev.joblens.analysis.domain.TalkingPoints;

import java.util.List;

public record TalkingPointsResponse(List<String> coverLetterAngles, List<String> questionsToAsk, List<String> whyItFits) {

    public static TalkingPointsResponse from(TalkingPoints talkingPoints) {
        return new TalkingPointsResponse(
                talkingPoints.coverLetterAngles(), talkingPoints.questionsToAsk(), talkingPoints.whyItFits());
    }
}
