package de.kfzteile24.salesOrderHub.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidInstanceofChecksInCatchClause"})
public class TimedPollingService {
    public static final Duration DELAY = Duration.ofMillis(50);
    public static final Duration TIMEOUT = Duration.ofSeconds(2);

    public Boolean poll(Duration delay, Duration timeout, Supplier<Boolean> test) {
        final var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay.toMillis());
                if (test.get()) {
                    return true;
                }
            } catch (Throwable t) {
                if (!(t instanceof AssertionError)) {
                    log.error(t.getMessage(), t);
                }
            }
        }
        return false;
    }

    public boolean pollWithDefaultTiming(Supplier<Boolean> test) {
        return poll(DELAY, TIMEOUT, test);
    }

    public void retry(Runnable runnable) {
        retry(DELAY, TIMEOUT, runnable);
    }

    public void retry(Duration delay, Duration timeout, Runnable runnable) {
        final var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
            try {
                runnable.run();
                return;
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                //continue
            }
        }
    }
}
