package uk.gov.hmcts.darts.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WaitUtil {
    private WaitUtil() {
    }

    public static void waitFor(Supplier<Boolean> supplier, int intervalMs, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        while (!supplier.get()) {
            if (System.currentTimeMillis() - startTime > timeoutSeconds * 1000) {
                throw new RuntimeException("Timeout waiting for condition");
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
