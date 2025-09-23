package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class DateTimeHelperTest {

    @Test
    void getDateTimeIsoFormatted_ReturnsOffsetDateTimeIsFormatted() {
        var dateTime = OffsetDateTime.parse("2024-07-31T11:29:56.101701Z");
        String result = DateTimeHelper.getDateTimeIsoFormatted(dateTime);
        assertEquals("2024-07-31T11:29:56.101701Z", result);
    }

    @Test
    void getDateTimeIsoFormatted_ReturnsNullDateTimeIsHandled() {
        assertNull(DateTimeHelper.getDateTimeIsoFormatted(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-07-31T11:29:56.101701Z", "2024-07-31T11:29:00.00000Z"})
    void floorToMinutes_ReturnsOffsetDateTimeWithSecondsAndNanosZeroed(OffsetDateTime dateTime) {
        OffsetDateTime result = DateTimeHelper.floorToMinutes(dateTime);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        assertEquals("2024-07-31T11:29:00.000Z", result.format(dateTimeFormatter));
    }

    @Test
    void floorToMinutes_ReturnsNullDateTimeIsHandled() {
        assertNull(DateTimeHelper.floorToMinutes(null));
    }
    
}