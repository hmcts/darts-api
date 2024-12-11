package uk.gov.hmcts.darts.util;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;

public final class DurationUtil {
    private DurationUtil() {

    }

    public static String formatDurationHumanReadable(Duration duration) {
        return DurationFormatUtils.formatDurationWords(
            duration.toMillis(),
            true,
            true);
    }
}
