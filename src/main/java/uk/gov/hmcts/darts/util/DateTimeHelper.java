package uk.gov.hmcts.darts.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class DateTimeHelper {

    private DateTimeHelper() {
        // Default
    }

    public static String getDateTimeIsoFormatted(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }

    public static OffsetDateTime floorToMinutes(OffsetDateTime dateTime) {
        return Objects.nonNull(dateTime) ? dateTime.truncatedTo(ChronoUnit.MILLIS).withSecond(0).withNano(0) : null;
    }
}
