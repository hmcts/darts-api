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

    public static boolean greaterThan(Duration duration1, Duration duration2) {
        return duration1.compareTo(duration2) > 0;
    }
}
