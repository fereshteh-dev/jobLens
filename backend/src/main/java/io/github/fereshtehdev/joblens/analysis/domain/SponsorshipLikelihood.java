package io.github.fereshtehdev.joblens.analysis.domain;

/**
 * Declaration order mirrors the Jev {@code choice} criteria. Comparison never relies on
 * that order (enum ordinals would misrank UNKNOWN): an explicit {@code rank} encodes that
 * silence (UNKNOWN) is a weaker negative signal than an explicit refusal or a hinted
 * "unlikely", but weaker than any positive signal.
 */
public enum SponsorshipLikelihood {
    EXPLICIT_YES(4),
    LIKELY(3),
    UNLIKELY(1),
    EXPLICIT_NO(0),
    UNKNOWN(2);

    private final int rank;

    SponsorshipLikelihood(int rank) {
        this.rank = rank;
    }

    public boolean meetsMinimum(SponsorshipLikelihood minimum) {
        return this.rank >= minimum.rank;
    }
}
