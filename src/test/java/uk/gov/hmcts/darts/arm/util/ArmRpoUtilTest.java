package uk.gov.hmcts.darts.arm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmRpoUtilTest {

    private final ArmRpoUtil armRpoUtil = new ArmRpoUtil();

    @Test
    void generateUniqueProductionName_shouldAppendCsvExtension() {
        // given
        String productionName = "testProduction";

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertEquals("testProduction__CSV", result);
    }

    @Test
    void generateUniqueProductionName_shouldHandleEmptyString() {
        // given
        String productionName = "";

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertEquals("_CSV", result);
    }

    @Test
    void generateUniqueProductionName_shouldHandleNull() {
        // given
        String productionName = null;

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertEquals("null_CSV", result);
    }
}