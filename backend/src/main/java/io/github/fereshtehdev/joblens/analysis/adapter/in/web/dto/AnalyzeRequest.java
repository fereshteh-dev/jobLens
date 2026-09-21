package io.github.fereshtehdev.joblens.analysis.adapter.in.web.dto;

import io.github.fereshtehdev.joblens.analysis.domain.JobPosting;
import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(
        @NotBlank String url,
        @NotBlank String title,
        @NotBlank String company,
        String location,
        @NotBlank String descriptionText) {

    public JobPosting toDomain() {
        return new JobPosting(url, title, company, location == null ? "" : location, descriptionText);
    }
}
