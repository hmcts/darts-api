package uk.gov.hmcts.darts.testutils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateHelper {

    private static LocalDateTime now = LocalDateTime.now();
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss+00");

    private DateHelper() {
        // Default
    }

    public static String todaysDateMinusDaysFormattedForSql(int daysToMinus) {
        LocalDateTime minusDate = now.minusDays(daysToMinus);
        ZonedDateTime zonedDateTime = minusDate.atZone(ZoneId.of("UTC"));
        return zonedDateTime.format(dateTimeFormatter);
    }

    public static String convertSqlDateTimeToLocalDateTime(String dateTimeString) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        return localDateTime.format(formatter);
    }
}
