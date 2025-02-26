package uk.gov.hmcts.darts.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeHelper {

    private DateTimeHelper() {
        // Default
    }

    public static String getDateTimeIsoFormatted(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }
}
