package io.github.fereshtehdev.joblens.analysis.adapter.in.web;

import io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto.AnalyzeRequest;
import io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto.AnalyzeResponse;
import io.github.fereshtehdev.joblens.analysis.application.AnalysisOutcome;
import io.github.fereshtehdev.joblens.analysis.application.AnalyzePostingUseCase;
import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import io.github.fereshtehdev.joblens.profile.domain.CandidateProfile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyze")
public class AnalyzeController {

    private final AnalyzePostingUseCase useCase;
    private final CandidateProfile candidateProfile;

    public AnalyzeController(AnalyzePostingUseCase useCase, CandidateProfile candidateProfile) {
        this.useCase = useCase;
        this.candidateProfile = candidateProfile;
    }

    @PostMapping
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        JobPosting posting = request.toDomain();
        AnalysisOutcome outcome = useCase.analyze(posting, candidateProfile);
        return AnalyzeResponse.from(outcome);
    }
}
