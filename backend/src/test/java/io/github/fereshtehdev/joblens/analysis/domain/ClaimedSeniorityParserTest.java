package io.github.fereshtehdev.joblens.analysis.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimedSeniorityParserTest {

    @ParameterizedTest
    @CsvSource({
            "Staff Backend Engineer, STAFF_PLUS",
            "Principal Engineer, STAFF_PLUS",
            "Senior Backend Engineer, SENIOR",
            "Sr. Software Engineer, SENIOR",
            "Junior Developer, JUNIOR",
            "Jr. Software Engineer, JUNIOR",
            "Backend Engineer, MID",
            "Software Engineer II, MID"
    })
    void parsesSeniorityFromTitleKeywords(String title, Seniority expected) {
        assertThat(ClaimedSeniorityParser.parse(title)).isEqualTo(expected);
    }
}
