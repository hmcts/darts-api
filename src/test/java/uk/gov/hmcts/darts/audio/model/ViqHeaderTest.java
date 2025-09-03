package uk.gov.hmcts.darts.audio.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ViqHeaderTest {

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

    @Test
    void shouldGenerateCorrectViqHeaderInBst() {
        var instant = Instant.parse("2023-07-15T09:23:11Z");

        byte[] header = new ViqHeader(instant).getViqHeaderBytes();

        assertThat(header[DAY_OFFSET]).isEqualTo((byte) 15);
        assertThat(header[MONTH_OFFSET]).isEqualTo((byte) 7);
        assertThat(header[HOUR_OFFSET]).isEqualTo((byte) 10); 
        assertThat(header[MINUTE_OFFSET]).isEqualTo((byte) 23);
        assertThat(header[SECONDS_OFFSET]).isEqualTo((byte) 11);
    }

    @Test
    void shouldGenerateCorrectViqHeaderInGmt() {
        // 9:23:11 UTC = 9:23:11 GMT (no offset in winter)
        var instant = Instant.parse("2023-01-15T09:23:11Z");

        byte[] header = new ViqHeader(instant).getViqHeaderBytes();

        assertThat(header[DAY_OFFSET]).isEqualTo((byte) 15);
        assertThat(header[MONTH_OFFSET]).isEqualTo((byte) 1);
        assertThat(header[HOUR_OFFSET]).isEqualTo((byte) 9); 
        assertThat(header[MINUTE_OFFSET]).isEqualTo((byte) 23);
        assertThat(header[SECONDS_OFFSET]).isEqualTo((byte) 11);
    }

    @Test
    void shouldGenerateCorrectStaticViqHeaderFields() {
        var instant = Instant.parse("2023-04-28T09:23:11Z");
        byte[] header = new ViqHeader(instant).getViqHeaderBytes();

        assertThat(header.length).isEqualTo(1024);
        assertThat(header[VERIFY_OFFSET_0]).isEqualTo((byte) VERIFY_OFFSET_VALUE_0);
        assertThat(header[VERIFY_OFFSET_1]).isEqualTo((byte) VERIFY_OFFSET_VALUE_1);
        assertThat(header[VERIFY_OFFSET_2]).isEqualTo((byte) VERIFY_OFFSET_VALUE_2);
        assertThat(header[VERIFY_OFFSET_3]).isEqualTo((byte) VERIFY_OFFSET_VALUE_3);
        assertThat(header[VERIFY_OFFSET_4]).isEqualTo((byte) VERIFY_OFFSET_VALUE_4);
        assertThat(header[FORMAT_OFFSET_0]).isEqualTo((byte) FORMAT_OFFSET_VALUE_0);
        assertThat(header[FORMAT_OFFSET_1]).isEqualTo((byte) FORMAT_OFFSET_VALUE_1);
    }

}
