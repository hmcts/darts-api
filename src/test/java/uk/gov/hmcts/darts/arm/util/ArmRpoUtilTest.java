package uk.gov.hmcts.darts.arm.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmRpoUtilTest {

    @Mock
    private ArmRpoService armRpoService;

    private ArmRpoUtil armRpoUtil;

    @BeforeEach
    void setUp() {
        armRpoUtil = new ArmRpoUtil(armRpoService);
    }

    @Test
    void generateUniqueProductionName_shouldAppendCsvExtension() {
        // given
        String productionName = "testProduction";

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertEquals("testProduction_" + result.substring(15, 51) + "_CSV", result);
    }

    @Test
    void generateUniqueProductionName_shouldHandleEmptyString() {
        // given
        String productionName = "";

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertEquals("_" + result.substring(1, 37) + "_CSV", result);
    }

    @Test
    void generateUniqueProductionName_shouldHandleNull() {
        // given
        String productionName = null;

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertEquals("null_" + result.substring(5, 41) + "_CSV", result);
    }
}