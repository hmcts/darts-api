package uk.gov.hmcts.darts.util;

import org.springframework.boot.test.system.CapturedOutput;

import static org.assertj.core.api.Assertions.assertThat;

public final class LogUtil {

    private LogUtil() {

    }


    public static void assertOutputHasMessage(CapturedOutput output, String message, int timeoutInSeconds) {
        WaitUtil.waitFor(() -> output.getAll().contains(message), message, timeoutInSeconds);
        assertThat(output).contains(message);
    }
}
