package io.github.fereshtehdev.joblens.analysis.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostingFingerprintTest {

    @Test
    void sameNormalizedTextProducesSameFingerprint() {
        JobPosting a = posting("Senior  Backend   Engineer needed.");
        JobPosting b = posting("senior backend engineer needed.");

        assertThat(PostingFingerprint.of(a)).isEqualTo(PostingFingerprint.of(b));
    }

    @Test
    void leadingAndTrailingWhitespaceIsIgnored() {
        JobPosting a = posting("Backend Engineer");
        JobPosting b = posting("  Backend Engineer  \n");

        assertThat(PostingFingerprint.of(a)).isEqualTo(PostingFingerprint.of(b));
    }

    @Test
    void differentTextProducesDifferentFingerprint() {
        JobPosting a = posting("Backend Engineer");
        JobPosting b = posting("Frontend Engineer");

        assertThat(PostingFingerprint.of(a)).isNotEqualTo(PostingFingerprint.of(b));
    }

    @Test
    void fingerprintIsSha256Hex() {
        JobPosting posting = posting("hello world");

        assertThat(PostingFingerprint.of(posting).value()).hasSize(64).matches("[0-9a-f]{64}");
    }

    private static JobPosting posting(String descriptionText) {
        return new JobPosting("https://example.com/job", "Title", "Company", "Remote", descriptionText);
    }
}
