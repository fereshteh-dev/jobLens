package io.github.fereshtehdev.joblens.analysis.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProbabilityTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void acceptsValuesWithinBounds(double value) {
        assertThat(new Probability(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.0001, 1.0001, -1.0, 2.0})
    void rejectsValuesOutsideBounds(double value) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Probability(value));
    }

    @Test
    void rejectsNaN() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Probability(Double.NaN));
    }
}
