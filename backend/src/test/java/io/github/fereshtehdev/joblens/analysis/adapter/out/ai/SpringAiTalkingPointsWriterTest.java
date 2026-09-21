package io.github.fereshtehdev.joblens.analysis.adapter.out.ai;

import io.github.fereshtehdev.joblens.analysis.application.port.TalkingPointsUnavailableException;
import io.github.fereshtehdev.joblens.config.properties.TalkingPointsResilienceProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Only the failure-translation logic is tested here - the happy path would require mocking
 * Spring AI's fluent ChatClient chain end to end, which exercises Spring AI's own well-tested
 * plumbing rather than code this project wrote. See ADR-0005.
 */
class SpringAiTalkingPointsWriterTest {

    @Test
    void translateFailure_wrapsAnyThrowableInTalkingPointsUnavailableException() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        SpringAiTalkingPointsWriter writer = new SpringAiTalkingPointsWriter(
                builder, new SimpleMeterRegistry(), resilienceProperties());
        RuntimeException cause = new RuntimeException("boom");

        TalkingPointsUnavailableException result = writer.translateFailure(cause);

        assertThat(result).hasCause(cause);
    }

    private static TalkingPointsResilienceProperties resilienceProperties() {
        return new TalkingPointsResilienceProperties(3, Duration.ofMillis(100), 10, 5, 50f, Duration.ofSeconds(1));
    }
}
