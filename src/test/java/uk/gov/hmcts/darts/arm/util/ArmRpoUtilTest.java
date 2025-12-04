package uk.gov.hmcts.darts.arm.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoUtilTest {

    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ArmApiService armApiService;

    private ArmRpoUtil armRpoUtil;

    @BeforeEach
    void setUp() {
        armRpoUtil = new ArmRpoUtil(armRpoService, armApiService);
    }

    @Test
    void generateUniqueProductionName_shouldAppendCsvExtension() {
        // given
        String productionName = "testProduction";

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then: prefix and suffix, and ensure there's a UUID-like part between
        assertThat(result).startsWith(productionName + "_");
        assertThat(result).endsWith("_CSV");

        String middle = result.substring((productionName + "_").length(), result.length() - "_CSV".length());
        // validate it's a UUID
        UUID.fromString(middle);
    }

    @Test
    void generateUniqueProductionName_shouldHandleEmptyString() {
        // given
        String productionName = "";

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then
        assertThat(result).startsWith("_");
        assertThat(result).endsWith("_CSV");

        String middle = result.substring(1, result.length() - "_CSV".length());
        UUID.fromString(middle);
    }

    @Test
    void generateUniqueProductionName_shouldHandleNull() {
        // given
        String productionName = null;

        // when
        String result = armRpoUtil.generateUniqueProductionName(productionName);

        // then - null becomes "null" in string concatenation
        assertThat(result).startsWith("null_");
        assertThat(result).endsWith("_CSV");

        String middle = result.substring("null_".length(), result.length() - "_CSV".length());
        UUID.fromString(middle);
    }

    @Test
    void getBearerToken_returnsTokenWhenPresent() {
        // given
        when(armApiService.getArmBearerToken()).thenReturn("token123");

        // when
        String result = armRpoUtil.getBearerToken("someEndpoint");

        // then
        assertThat(result).isEqualTo("token123");
        verify(armApiService, times(1)).getArmBearerToken();
        verify(armApiService, never()).evictToken();
        verifyNoMoreInteractions(armApiService);
    }

    @Test
    void getBearerToken_emptyThenRetrySucceeds() {
        // given:
        when(armApiService.getArmBearerToken()).thenReturn("", "retryToken");

        // when
        String result = armRpoUtil.getBearerToken("someEndpoint");

        // then
        assertThat(result).isEqualTo("retryToken");
        verify(armApiService, times(2)).getArmBearerToken();
        verify(armApiService, times(1)).evictToken();
    }

    @Test
    void getBearerToken_whenGetThrowsWrappedInArmRpoException() {
        // given
        when(armApiService.getArmBearerToken()).thenThrow(new RuntimeException("boom"));

        // when + assert
        ArmRpoException ex = assertThrows(ArmRpoException.class, () -> armRpoUtil.getBearerToken("someEndpoint"));
        assertThat(ex).hasMessageContaining("Exception occurred while getting bearer token for someEndpoint");
        verify(armApiService, times(1)).getArmBearerToken();
        verify(armApiService, never()).evictToken();
    }

    @Test
    void retryGetBearerToken_evictThenGetReturnsToken() {
        // given
        when(armApiService.getArmBearerToken()).thenReturn("retriedToken");

        // when
        String token = armRpoUtil.retryGetBearerToken("someEndpoint");

        // then
        assertThat(token).isEqualTo("retriedToken");

        InOrder inOrder = inOrder(armApiService);
        inOrder.verify(armApiService).evictToken();
        inOrder.verify(armApiService).getArmBearerToken();
        verifyNoMoreInteractions(armApiService);
    }

    @Test
    void retryGetBearerToken_whenStillEmpty_throwsArmRpoExceptionAndEvicted() {
        // given
        when(armApiService.getArmBearerToken()).thenReturn("");

        // when + assert
        ArmRpoException ex = assertThrows(ArmRpoException.class, () -> armRpoUtil.retryGetBearerToken("someEndpoint"));
        assertThat(ex).hasMessageContaining("Unable to get bearer token for someEndpoint after retrying");

        verify(armApiService, times(1)).evictToken();
        verify(armApiService, times(1)).getArmBearerToken();
    }
}
