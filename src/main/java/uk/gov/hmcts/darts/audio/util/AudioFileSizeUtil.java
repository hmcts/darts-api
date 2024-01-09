package uk.gov.hmcts.darts.audio.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class AudioFileSizeUtil {
    private BigDecimal percentDifference = new BigDecimal(1.004709525);
    private int staticDifference = 331;

    public long mp2ToMp3FileSize(long mp2FileSize) {
        return (percentDifference.multiply(new BigDecimal(mp2FileSize))).add(new BigDecimal(staticDifference)).longValue();
    }
}
