package uk.gov.hmcts.darts.util;

import lombok.SneakyThrows;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public final class WaitUtil {
    private WaitUtil() {
    }

    @SneakyThrows
    @SuppressWarnings("PMD.DoNotUseThreads")//Required to prevent busy waiting
    public static void waitFor(Supplier<Boolean> supplier, String message, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        while (!supplier.get()) {
            if (System.currentTimeMillis() - startTime > timeoutSeconds * 1000) {
                fail(message);
            }
            Thread.sleep(100);
        }
    }
}
