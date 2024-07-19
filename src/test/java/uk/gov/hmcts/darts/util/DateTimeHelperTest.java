package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class DateTimeHelperTest {


    @Test
    void testOffsetDateTimeIsFormatted() {
        var dateTime = OffsetDateTime.parse("2024-07-31T11:29:56.101701Z");
        String result = DateTimeHelper.getDateTimeIsoFormatted(dateTime);
        assertEquals("2024-07-31T11:29:56.101701Z", result);
    }

    @Test
    void testNullDateTimeIsHandled() {
        assertNull(DateTimeHelper.getDateTimeIsoFormatted(null));
    }
}