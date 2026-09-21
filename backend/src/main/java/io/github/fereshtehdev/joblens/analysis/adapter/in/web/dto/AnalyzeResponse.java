package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.fereshtehdev.joblens.analysis.application.AnalysisOutcome;
import io.github.fereshtehdev.joblens.analysis.domain.Analysis;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyzeResponse(
        SeniorityResponse seniority,
        List<RedFlagResponse> redFlags,
        SponsorshipResponse sponsorship,
        GateResponse gate,
        TalkingPointsResponse talkingPoints) {

    public static AnalyzeResponse from(AnalysisOutcome outcome) {
        Analysis analysis = outcome.analysis();
        TalkingPointsResponse talkingPoints = switch (outcome) {
            case AnalysisOutcome.WithTalkingPoints withTalkingPoints ->
                    TalkingPointsResponse.from(withTalkingPoints.talkingPoints());
            case AnalysisOutcome.WithoutTalkingPoints withoutTalkingPoints -> null;
        };

        return new AnalyzeResponse(
                SeniorityResponse.from(analysis.seniority()),
                analysis.redFlags().stream().map(RedFlagResponse::from).toList(),
                SponsorshipResponse.from(analysis.sponsorship()),
                GateResponse.from(outcome.gate()),
                talkingPoints);
    }
}
