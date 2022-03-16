package de.kfzteile24.salesOrderHub.services;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.Test;

class TimedPollingServiceTest {

    private final TimedPollingService pollingService = new TimedPollingService();

    @Test
    void pollingEndsWithTrueAsSoonAsTheTestFunctionReturnsTrue() {
        MutableLong counter = new MutableLong(0);

        final Boolean result = pollingService.poll(Duration.ofMillis(1), Duration.ofSeconds(1), () -> {
            counter.increment();
            return true;
        });

        assertThat(result).isTrue();
        assertThat(counter.getValue()).isEqualTo(1);
    }

    @Test
    void pollingEndsWithFalseAfterTheTimeoutIfTheTestFunctionAlwaysReturnsFalse() {
        final var startTime = System.currentTimeMillis();
        final var timeout = Duration.ofMillis(100);

        final Boolean result = pollingService.poll(Duration.ofMillis(5), timeout, () -> false);

        assertThat(result).isFalse();
        assertThat(System.currentTimeMillis() - startTime).isGreaterThanOrEqualTo(timeout.toMillis());
    }

    @Test
    void errorsThrownByTheTestFunctionDontAreCaught() {
        final Boolean result = pollingService.poll(Duration.ofMillis(5), Duration.ofMillis(100),
                 () -> { throw new AssertionError(); });

        assertThat(result).isFalse();
    }

    @Test
    void pollWithDefaultTimingCallsPollWithDefaultValues() {
        final TimedPollingService pollingServiceSpy = spy(new TimedPollingService());

        pollingServiceSpy.pollWithDefaultTiming(() -> true);

        verify(pollingServiceSpy).poll(eq(TimedPollingService.DELAY), eq(TimedPollingService.TIMEOUT), any());
    }
}