package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateConvertersTest {

    private final DateConverters dateConverters = new DateConverters();

    @Test
    void convertsUtcLocalDateTimeToLegacyDateTimeForGmt() {
        var utcDateTime = OffsetDateTime.parse("2022-01-11T16:00:00.000Z");

        var dateTime = dateConverters.offsetDateTimeToLegacyDateTime(utcDateTime);

        assertThat(dateTime.getHour()).isEqualTo(16);
    }

    @Test
    void convertsUtcLocalDateTimeToLegacyDateTimeForBst() {
        var utcDateTime = OffsetDateTime.parse("2022-06-11T16:00:00.000Z");

        var dateTime = dateConverters.offsetDateTimeToLegacyDateTime(utcDateTime);

        assertThat(dateTime.getHour()).isEqualTo(17);
    }

}
