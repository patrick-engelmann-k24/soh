package de.kfzteile24.salesOrderHub.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class TimerService {
    public static final long DELAY_MS = 50;
    public static final long TIMEOUT_MS = 1000;

    public Boolean schedule(long delay, long timeout, Supplier<Boolean> test) {
        final var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
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

    public boolean scheduleWithDefaultTiming(Supplier<Boolean> test) {
        return schedule(DELAY_MS, TIMEOUT_MS, test);
    }

}
