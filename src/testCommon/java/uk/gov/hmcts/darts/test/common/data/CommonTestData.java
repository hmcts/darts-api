package uk.gov.hmcts.darts.test.common.data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class CommonTestData {

    private CommonTestData() {

    }

    // Can probably be simplified
    public static OffsetDateTime createOffsetDateTime(String timestamp) {
        ZoneId zoneId = ZoneId.of("UTC");   // Or another geographic: Europe/Paris

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(timestamp, formatter);

        ZoneOffset offset = zoneId.getRules().getOffset(start);

        return OffsetDateTime.of(start, offset);
    }
}