package io.github.fereshtehdev.joblens.analysis.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GateDecisionTest {

    @Test
    void passingHasNoReasons() {
        assertThat(GateDecision.passing().passed()).isTrue();
        assertThat(GateDecision.passing().reasons()).isEmpty();
    }

    @Test
    void failedRequiresAtLeastOneReason() {
        assertThatIllegalArgumentException().isThrownBy(() -> GateDecision.failed(List.of()));
    }

    @Test
    void passedDecisionCannotCarryReasons() {
        assertThatIllegalArgumentException().isThrownBy(() -> new GateDecision(true, List.of("unexpected")));
    }

    @Test
    void failedDecisionKeepsItsReasons() {
        GateDecision decision = GateDecision.failed(List.of("seniority mismatch"));

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reasons()).containsExactly("seniority mismatch");
    }
}
