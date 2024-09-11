package uk.gov.hmcts.darts.test.common;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.awaitility.Awaitility.await;

public final class AwaitabilityUtil {

    private AwaitabilityUtil() {

    }

    public static void waitForMax15SecondsWithOneSecondPoll(Callable<Boolean> callable) {
        waitForMaxWithOneSecondPoll(callable, Duration.ofSeconds(15));
    }

    public static void waitForMax10SecondsWithOneSecondPoll(Callable<Boolean> callable) {
        waitForMaxWithOneSecondPoll(callable, Duration.ofSeconds(10));
    }

    public static void waitForMaxWithOneSecondPoll(Callable<Boolean> callable, Duration duration) {
        var awaitAlias = random(4);
        await(awaitAlias).atMost(duration)
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
