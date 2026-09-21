package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.Locale;

/**
 * Parses the seniority a posting's title claims, from the title text alone. Shared by the
 * mock and real classifiers since Jev is only asked to judge the *actual* seniority and how
 * well the title matches it (see {@code jev-questions.yml}), not to re-derive the claim.
 */
public final class ClaimedSeniorityParser {

    private ClaimedSeniorityParser() {
    }

    public static Seniority parse(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        if (lower.contains("staff") || lower.contains("principal")) {
            return Seniority.STAFF_PLUS;
        }
        if (lower.contains("senior") || lower.contains("sr.")) {
            return Seniority.SENIOR;
        }
        if (lower.contains("junior") || lower.contains("jr.")) {
            return Seniority.JUNIOR;
        }
        return Seniority.MID;
    }
}
