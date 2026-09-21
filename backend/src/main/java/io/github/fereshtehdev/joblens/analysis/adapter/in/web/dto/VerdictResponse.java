package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.fereshtehdev.joblens.analysis.domain.Probability;
import io.github.fereshtehdev.joblens.analysis.domain.Verdict;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerdictResponse(String kind, Double probability) {

    public static VerdictResponse from(Verdict verdict) {
        return switch (verdict) {
            case Verdict.Confident(Probability p) -> new VerdictResponse("CONFIDENT", p.value());
            case Verdict.Uncertain(Probability p) -> new VerdictResponse("UNCERTAIN", p.value());
            case Verdict.Ignored ignored -> new VerdictResponse("IGNORED", null);
        };
    }
}
