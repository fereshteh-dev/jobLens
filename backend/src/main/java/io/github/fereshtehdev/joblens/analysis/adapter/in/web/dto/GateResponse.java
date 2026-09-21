package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import io.github.fereshtehdev.joblens.analysis.domain.GateDecision;

import java.util.List;

public record GateResponse(boolean passed, List<String> reasons) {

    public static GateResponse from(GateDecision decision) {
        return new GateResponse(decision.passed(), decision.reasons());
    }
}
