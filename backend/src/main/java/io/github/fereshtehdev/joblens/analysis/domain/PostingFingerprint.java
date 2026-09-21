package io.github.fereshtehdev.joblens.analysis.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Cache key for an {@code Analysis}. Two postings with the same description text, modulo
 * case and whitespace, fingerprint identically so re-opening the same tab is a cache hit.
 */
public record PostingFingerprint(String value) {

    public PostingFingerprint {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PostingFingerprint of(JobPosting posting) {
        return new PostingFingerprint(sha256Hex(normalize(posting.descriptionText())));
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
