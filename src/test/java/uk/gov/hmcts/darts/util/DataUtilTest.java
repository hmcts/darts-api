package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataUtilTest {

    @Test
    void toUpperCase() {
        assertThat(DataUtil.toUpperCase("test")).isEqualTo("TEST");
    }


    @Test
    void toUpperCaseNullValue() {
        assertThat(DataUtil.toUpperCase(null)).isNull();
    }
}
