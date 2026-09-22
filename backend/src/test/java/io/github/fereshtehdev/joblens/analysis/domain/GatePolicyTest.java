package io.github.fereshtehdev.joblens.analysis.domain;

import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatePolicyTest {

    private static final GateSettings LENIENT_SETTINGS = new GateSettings(1.5);

    @Test
    void passesWhenSeniorityMatchesFlagsAreLowAndSponsorshipMeetsMinimum() {
        Analysis analysis = analysis(Seniority.SENIOR, List.of(ignoredFlag()), sponsorship(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.LIKELY));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, LENIENT_SETTINGS);

        assertThat(decision.passed()).isTrue();
        assertThat(decision.reasons()).isEmpty();
    }

    @Test
    void failsWhenActualSeniorityDoesNotMatchTarget() {
        Analysis analysis = analysis(Seniority.MID, List.of(ignoredFlag()), sponsorship(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.LIKELY));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, LENIENT_SETTINGS);

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reasons()).hasSize(1).allMatch(reason -> reason.contains("seniority"));
    }

    @Test
    void failsWhenCombinedRedFlagScoreExceedsLimit() {
        Analysis analysis = analysis(Seniority.SENIOR,
                List.of(confidentFlag(RedFlagKind.HIDDEN_OVERTIME, 0.9), confidentFlag(RedFlagKind.VAGUE_SCOPE, 0.9)),
                sponsorship(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.LIKELY));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNLIKELY);
        GateSettings strictSettings = new GateSettings(1.0);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, strictSettings);

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reasons()).hasSize(1).allMatch(reason -> reason.contains("red-flag"));
    }

    @Test
    void failsWhenVisaLikelihoodBelowMinimum() {
        Analysis analysis = analysis(Seniority.SENIOR, List.of(ignoredFlag()), sponsorship(SponsorshipLikelihood.EXPLICIT_NO, SponsorshipLikelihood.LIKELY));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.LIKELY);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, LENIENT_SETTINGS);

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reasons()).hasSize(1).allMatch(reason -> reason.contains("Visa"));
    }

    @Test
    void failsWhenRelocationLikelihoodBelowMinimum() {
        Analysis analysis = analysis(Seniority.SENIOR, List.of(ignoredFlag()), sponsorship(SponsorshipLikelihood.LIKELY, SponsorshipLikelihood.EXPLICIT_NO));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.LIKELY);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, LENIENT_SETTINGS);

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reasons()).hasSize(1).allMatch(reason -> reason.contains("Relocation"));
    }

    @Test
    void minimumSponsorshipUnknown_meansNoRequirement_evenBelowExplicitNo() {
        // Regression test: UNKNOWN as a minimum documented in candidate-profile.example.yml
        // as "I don't need this" must not reject postings ranked below UNKNOWN either - found
        // via a live test where a real posting's UNLIKELY relocation wrongly failed the gate
        // against a minimum of UNKNOWN.
        Analysis analysis = analysis(Seniority.SENIOR, List.of(ignoredFlag()),
                sponsorship(SponsorshipLikelihood.EXPLICIT_NO, SponsorshipLikelihood.EXPLICIT_NO));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.UNKNOWN);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, LENIENT_SETTINGS);

        assertThat(decision.passed()).isTrue();
        assertThat(decision.reasons()).isEmpty();
    }

    @Test
    void accumulatesAllFailureReasons() {
        Analysis analysis = analysis(Seniority.MID,
                List.of(confidentFlag(RedFlagKind.HIDDEN_OVERTIME, 0.95), confidentFlag(RedFlagKind.VAGUE_SCOPE, 0.95)),
                sponsorship(SponsorshipLikelihood.EXPLICIT_NO, SponsorshipLikelihood.EXPLICIT_NO));
        CandidateProfile profile = new CandidateProfile(Seniority.SENIOR, SponsorshipLikelihood.LIKELY);
        GateSettings strictSettings = new GateSettings(1.0);

        GateDecision decision = GatePolicy.evaluate(analysis, profile, strictSettings);

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reasons()).hasSize(4);
    }

    private static Analysis analysis(Seniority actual, List<RedFlag> redFlags, SponsorshipAssessment sponsorship) {
        return new Analysis(
                new SeniorityAssessment(Seniority.SENIOR, actual, new Verdict.Confident(new Probability(0.9))),
                redFlags,
                sponsorship,
                "jev-1.13.0");
    }

    private static SponsorshipAssessment sponsorship(SponsorshipLikelihood visa, SponsorshipLikelihood relocation) {
        return new SponsorshipAssessment(visa, relocation);
    }

    private static RedFlag ignoredFlag() {
        return new RedFlag(RedFlagKind.HIDDEN_OVERTIME, new Verdict.Ignored());
    }

    private static RedFlag confidentFlag(RedFlagKind kind, double probability) {
        return new RedFlag(kind, new Verdict.Confident(new Probability(probability)));
    }
}
