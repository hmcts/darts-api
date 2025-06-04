package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

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

    @Test
    void floorToMinutes_ReturnsOffsetDateTimeWithSecondsAndNanosZeroed() {
        var dateTime = OffsetDateTime.parse("2024-07-31T11:29:56.101701Z");
        OffsetDateTime result = DateTimeHelper.floorToMinutes(dateTime);
        assertEquals("2024-07-31T11:29Z", result.toString());
    }

    @Test
    void floorToMinutes_ReturnsNullDateTimeIsHandled() {
        assertNull(DateTimeHelper.floorToMinutes(null));
    }
}