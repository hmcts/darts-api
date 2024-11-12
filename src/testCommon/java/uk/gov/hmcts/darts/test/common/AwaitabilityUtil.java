package uk.gov.hmcts.darts.test.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.AssertionFailure;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.awaitility.Awaitility.await;

public final class AwaitabilityUtil {

    private AwaitabilityUtil() {

    }

    public static void waitForMax10SecondsWithOneSecondPoll(Callable<Boolean> callable) {
        waitForMaxWithOneSecondPoll(callable, Duration.ofSeconds(10));
    }

    public static void waitForMax10SecondsWithOneSecondPoll(Runnable runnable) {
        waitForMaxWithOneSecondPoll(runnable, Duration.ofSeconds(10));
    }

    public static boolean waitForMaxWithOneSecondPoll(Callable<Boolean> callable, Duration duration) {
        WaitingResult waitingResult = new WaitingResult(callable);

        var awaitAlias = random(4);
        await(awaitAlias).atMost(duration)
            .with().pollInterval(Duration.ofSeconds(1))
            .until(waitingResult);

        if (!waitingResult.isCriteriaMet()) {
            throw new AssertionFailure("Criteria not met within the specified time");
        }
        return waitingResult.isCriteriaMet();
    }

    @RequiredArgsConstructor
    @Getter
    static class WaitingResult implements Callable<Boolean> {
        public final Callable<Boolean> criteria;

        public boolean criteriaMet;

        @Override
        public Boolean call() throws Exception {
            criteriaMet = criteria.call();
            return criteriaMet;
        }
    }

    public static void waitForMaxWithOneSecondPoll(Runnable runnable, Duration duration) {
        var awaitAlias = random(4);
        await(awaitAlias).atMost(duration)
            .with().pollInterval(Duration.ofSeconds(1))
            .until(() -> {
                try {
                    runnable.run();
                } catch (Exception | Error e) {
                    // ignore the error for now
                    return false;
                }
                return true;
            });
    }
}