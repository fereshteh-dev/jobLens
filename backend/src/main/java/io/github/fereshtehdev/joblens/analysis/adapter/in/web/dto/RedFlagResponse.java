package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlagKind;

public record RedFlagResponse(RedFlagKind kind, VerdictResponse verdict) {

    public static RedFlagResponse from(RedFlag redFlag) {
        return new RedFlagResponse(redFlag.kind(), VerdictResponse.from(redFlag.verdict()));
    }
}
