package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.List;
import java.util.Objects;

public record GateDecision(boolean passed, List<String> reasons) {

    public GateDecision {
        Objects.requireNonNull(reasons, "reasons must not be null");
        reasons = List.copyOf(reasons);
        if (passed && !reasons.isEmpty()) {
            throw new IllegalArgumentException("a passed decision must not carry failure reasons");
        }
    }

    public static GateDecision passing() {
        return new GateDecision(true, List.of());
    }

    public static GateDecision failed(List<String> reasons) {
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("a failed decision must carry at least one reason");
        }
        return new GateDecision(false, reasons);
    }
}
