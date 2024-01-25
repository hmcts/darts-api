package uk.gov.hmcts.darts.audio.model;

import lombok.Value;

import java.time.Instant;
import java.time.ZoneOffset;

@Value
public class ViqHeader {

    private static final int VIQ_HEADER_SIZE = 1024;

    private static final int VERIFY_OFFSET_0 = 3;
    private static final int VERIFY_OFFSET_1 = 4;
    private static final int VERIFY_OFFSET_2 = 5;
    private static final int VERIFY_OFFSET_3 = 6;
    private static final int VERIFY_OFFSET_4 = 7;
    private static final char VERIFY_OFFSET_VALUE_0 = '.';
    private static final char VERIFY_OFFSET_VALUE_1 = 'd';
    private static final char VERIFY_OFFSET_VALUE_2 = 'i';
    private static final char VERIFY_OFFSET_VALUE_3 = 'c';
    private static final char VERIFY_OFFSET_VALUE_4 = '\0';

    private static final int FORMAT_OFFSET_0 = 593;
    private static final int FORMAT_OFFSET_1 = 594;
    private static final char FORMAT_OFFSET_VALUE_0 = 'M';
    private static final char FORMAT_OFFSET_VALUE_1 = '2';

    private static final int DAY_OFFSET = 39;
    private static final int MONTH_OFFSET = 40;
    private static final int MINUTE_OFFSET = 41;
    private static final int HOUR_OFFSET = 42;
    private static final int SECONDS_OFFSET = 44;

    byte[] viqHeader;

    public ViqHeader(Instant instant) {

        viqHeader = new byte[VIQ_HEADER_SIZE];
        viqHeader[DAY_OFFSET] = (byte) instant.atOffset(ZoneOffset.UTC).getDayOfMonth();
        viqHeader[MONTH_OFFSET] = (byte) (instant.atOffset(ZoneOffset.UTC).getMonthValue() + 1);
        viqHeader[MINUTE_OFFSET] = (byte) instant.atOffset(ZoneOffset.UTC).getMinute();
        viqHeader[HOUR_OFFSET] = (byte) instant.atOffset(ZoneOffset.UTC).getHour();
        viqHeader[SECONDS_OFFSET] = (byte) instant.atOffset(ZoneOffset.UTC).getSecond();
        viqHeader[VERIFY_OFFSET_0] = (byte) VERIFY_OFFSET_VALUE_0;
        viqHeader[VERIFY_OFFSET_1] = (byte) VERIFY_OFFSET_VALUE_1;
        viqHeader[VERIFY_OFFSET_2] = (byte) VERIFY_OFFSET_VALUE_2;
        viqHeader[VERIFY_OFFSET_3] = (byte) VERIFY_OFFSET_VALUE_3;
        viqHeader[VERIFY_OFFSET_4] = (byte) VERIFY_OFFSET_VALUE_4;
        viqHeader[FORMAT_OFFSET_0] = (byte) FORMAT_OFFSET_VALUE_0;
        viqHeader[FORMAT_OFFSET_1] = (byte) FORMAT_OFFSET_VALUE_1;
    }
}
