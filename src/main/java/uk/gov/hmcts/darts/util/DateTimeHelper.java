package uk.gov.hmcts.darts.util;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static uk.gov.hmcts.darts.common.util.DateConverterUtil.EUROPE_LONDON_ZONE;

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

    public static ZonedDateTime getCurrentTimeInEuropeLondon(Clock clock) {
        return ZonedDateTime.ofInstant(clock.instant(), EUROPE_LONDON_ZONE);
    }
}
