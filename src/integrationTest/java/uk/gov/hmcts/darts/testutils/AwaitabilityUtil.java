package uk.gov.hmcts.darts.testutils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;

public final class AwaitabilityUtil {

    private AwaitabilityUtil() {

    }

    public static void waitForMax10SecondsWithOneSecondPoll(Callable<Boolean> callable) {
        await().atMost(Duration.of(10, ChronoUnit.SECONDS))
            .with().pollInterval(Duration.of(1, ChronoUnit.SECONDS))
            .until(() -> {
                boolean verified;
                try {
                    verified = callable.call();
                } catch (Exception e) {
                    // ignore the error for now
                    verified = false;
                }
                return verified;
            });
    }
}
