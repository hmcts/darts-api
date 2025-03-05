package uk.gov.hmcts.darts.util;

import lombok.SneakyThrows;
import org.springframework.boot.test.system.CapturedOutput;

import static org.junit.jupiter.api.Assertions.fail;

public final class LogUtil {

    private LogUtil() {

    }

    @SneakyThrows
    @SuppressWarnings("PMD.DoNotUseThreads")//Required to prevent busy waiting
    //Used to allow logs to catch up with the test
    public static void waitUntilMessage(CapturedOutput capturedOutput, String message,
                                        int timeoutInSeconds) {
        long startTime = System.currentTimeMillis();
        while (!capturedOutput.getAll().contains(message)) {
            if (System.currentTimeMillis() - startTime > timeoutInSeconds * 1000) {
                fail("Timeout waiting for message: " + message);
            }
            Thread.sleep(100);
        }
    }
}
