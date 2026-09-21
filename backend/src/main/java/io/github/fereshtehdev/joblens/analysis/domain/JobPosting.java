package io.github.fereshtehdev.joblens.analysis.domain;

import java.util.Objects;

public record JobPosting(String url, String title, String company, String location, String descriptionText) {

    public JobPosting {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(company, "company must not be null");
        Objects.requireNonNull(location, "location must not be null");
        if (descriptionText == null || descriptionText.isBlank()) {
            throw new IllegalArgumentException("descriptionText must not be blank");
        }
    }
}
