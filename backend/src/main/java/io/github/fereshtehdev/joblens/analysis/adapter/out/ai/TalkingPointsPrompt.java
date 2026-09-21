package io.github.fereshtehdev.joblens.analysis.adapter.out.ai;

import io.github.fereshtehdev.joblens.analysis.domain.Analysis;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.analysis.domain.RedFlag;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;

/** Builds the system/user prompt text for the LLM. Kept separate from the adapter so the prompt can change without touching call/resilience wiring. */
final class TalkingPointsPrompt {

    private TalkingPointsPrompt() {
    }

    static final String SYSTEM = """
            You write short, concrete talking points for a job applicant, based only on the \
            job posting and the analysis already computed for it. Never invent facts about the \
            company or role that are not in the posting. Never mention that an automated \
            analysis was performed. Keep every item to one sentence.""";

    static String user(JobPosting posting, Analysis analysis, CandidateProfile profile) {
        StringBuilder redFlagSummary = new StringBuilder();
        for (RedFlag flag : analysis.redFlags()) {
            redFlagSummary.append("- ").append(flag.kind()).append(": ").append(flag.verdict()).append('\n');
        }

        return """
                Job posting:
                Title: %s
                Company: %s
                Location: %s
                Description: %s

                Computed analysis:
                Claimed seniority: %s
                Actual seniority: %s
                Red flags:
                %s
                Visa sponsorship: %s
                Relocation support: %s

                Candidate's target seniority: %s

                Write:
                - coverLetterAngles: 2-3 short angles for a cover letter, grounded in the posting.
                - questionsToAsk: 2-3 questions worth asking the interviewer about this specific role.
                - whyItFits: 1-2 short reasons this role fits the candidate's target seniority.
                """.formatted(
                posting.title(), posting.company(), posting.location(), posting.descriptionText(),
                analysis.seniority().claimed(), analysis.seniority().actual(), redFlagSummary,
                analysis.sponsorship().visa(), analysis.sponsorship().relocation(),
                profile.targetSeniority());
    }
}
