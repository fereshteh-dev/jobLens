package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.Objects;

public record RedFlag(RedFlagKind kind, Verdict verdict) {

    public RedFlag {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
    }
}
