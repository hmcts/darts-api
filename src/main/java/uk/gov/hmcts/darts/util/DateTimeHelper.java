package uk.gov.hmcts.darts.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeHelper {

    private DateTimeHelper() {
        // Default
    }

    public static String getDateTimeIsoFormatted(OffsetDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
