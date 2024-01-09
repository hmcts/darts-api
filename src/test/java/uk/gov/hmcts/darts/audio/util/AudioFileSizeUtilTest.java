package uk.gov.hmcts.darts.audio.util;

import org.junit.jupiter.api.Test;

class AudioFileSizeUtilTest {

    @Test
    void mp2ToMp3FileSize() {
        AudioFileSizeUtil.mp2ToMp3FileSize(2_388_776);
    }
}
