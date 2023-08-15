package uk.gov.hmcts.darts.common.util;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DateConverters {

    public static final ZoneId EUROPE_LONDON_ZONE = ZoneId.of("Europe/London");

    public ZonedDateTime offsetDateTimeToLegacyDateTime(final OffsetDateTime offsetDateTime) {
        return offsetDateTime.atZoneSameInstant(EUROPE_LONDON_ZONE);
    }

}
