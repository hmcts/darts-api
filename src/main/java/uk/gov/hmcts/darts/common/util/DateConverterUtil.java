package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@UtilityClass
public class DateConverterUtil {

    public static final ZoneId EUROPE_LONDON_ZONE = ZoneId.of("Europe/London");
    public static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    public static final OffsetTime MIDNIGHT_UTC = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

    public ZonedDateTime toZonedDateTime(final OffsetDateTime offsetDateTime) {
        return offsetDateTime.atZoneSameInstant(EUROPE_LONDON_ZONE);
    }

    public ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(EUROPE_LONDON_ZONE);
    }

    public OffsetDateTime toOffsetDateTime(LocalDate localDate) {
        return localDate.atTime(MIDNIGHT_UTC);
    }

    private OffsetDateTime toOffsetDateTime(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toOffsetDateTime();
    }

    public OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        ZonedDateTime zonedDateTime = toZonedDateTime(localDateTime);
        return toOffsetDateTime(zonedDateTime);
    }

    private LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toLocalDateTime();
    }

    public LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        ZonedDateTime zonedDateTime = toZonedDateTime(offsetDateTime);
        return toLocalDateTime(zonedDateTime);
    }
    
    public LocalDate toLocalDate(OffsetDateTime offsetDateTime) {
        ZonedDateTime zonedDateTime = toZonedDateTime(offsetDateTime);
        return zonedDateTime.toLocalDate();
    }

}
