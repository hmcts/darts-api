package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateConverterUtilTest {

    @Test
    void convertsUtcLocalDateTimeToLegacyDateTimeForGmt() {
        var utcDateTime = OffsetDateTime.parse("2022-01-11T16:00:00.000Z");

        var dateTime = DateConverterUtil.toZonedDateTime(utcDateTime);

        assertThat(dateTime.getHour()).isEqualTo(16);
    }

    @Test
    void convertsUtcLocalDateTimeToLegacyDateTimeForBst() {
        var utcDateTime = OffsetDateTime.parse("2022-06-11T16:00:00.000Z");

        var dateTime = DateConverterUtil.toZonedDateTime(utcDateTime);

        assertThat(dateTime.getHour()).isEqualTo(17);
    }

    @Test
    void convertsUtcLocalDateTimeToLegacyLocalDateForGmt() {
        var utcDateTime = OffsetDateTime.parse("2022-01-11T23:45:00.000Z");

        var localDate = DateConverterUtil.toLocalDate(utcDateTime);

        // utc to gmt does not roll over to next day
        assertThat(localDate.isEqual(localDate.of(2022, 1, 11))).isTrue();
    }

    @Test
    void convertsUtcLocalDateTimeToLegacyLocalDateForBst() {
        var utcDateTime = OffsetDateTime.parse("2022-05-11T16:00:00.000Z");

        var localDate = DateConverterUtil.toLocalDate(utcDateTime);

        assertThat(localDate.isEqual(localDate.of(2022, 5, 11))).isTrue();
    }

    @Test
    void convertsUtcLocalDateTimeToLegacyLocalDateForBstWithDateCrossover() {
        var utcDateTime = OffsetDateTime.parse("2022-05-11T23:59:00.000Z");

        var localDate = DateConverterUtil.toLocalDate(utcDateTime);

        // utc to bst rolls over to next day
        assertThat(localDate.isEqual(localDate.of(2022, 5, 12))).isTrue();
    }

}