package uk.gov.hmcts.darts.testutils;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;

public final class AwaitabilityUtil {

    private AwaitabilityUtil() {

    }

    public static void waitForMax10SecondsWithOneSecondPoll(Callable<Boolean> callable) {
        await().atMost(Duration.ofSeconds(10))
            .with().pollInterval(Duration.ofSeconds(1))
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
