package io.github.fereshtehdev.joblens;

import io.github.fereshtehdev.joblens.analysis.adapter.out.jev.JevQuestionSchema;
import io.github.fereshtehdev.joblens.config.properties.CacheProperties;
import io.github.fereshtehdev.joblens.config.properties.GateProperties;
import io.github.fereshtehdev.joblens.config.properties.JevProperties;
import io.github.fereshtehdev.joblens.config.properties.JevResilienceProperties;
import io.github.fereshtehdev.joblens.config.properties.RateLimitProperties;
import io.github.fereshtehdev.joblens.config.properties.SecurityProperties;
import io.github.fereshtehdev.joblens.config.properties.TalkingPointsResilienceProperties;
import io.github.fereshtehdev.joblens.config.properties.ThresholdProperties;
import io.github.fereshtehdev.joblens.profile.adapter.out.config.CandidateProfileProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        GateProperties.class, CandidateProfileProperties.class, CacheProperties.class,
        RateLimitProperties.class, TalkingPointsResilienceProperties.class, JevProperties.class,
        ThresholdProperties.class, JevResilienceProperties.class, JevQuestionSchema.class,
        SecurityProperties.class
})
public class JoblensApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoblensApplication.class, args);
    }
}
