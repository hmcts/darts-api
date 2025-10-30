package uk.gov.hmcts.darts.arm.rpo.impl;

import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.client.model.rpo.ExtendedProductionsByMatterResponse;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.impl.ArmClientServiceImpl;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetExtendedProductionsByMatterServiceTest {

    public static final String PRODUCTION_NAME = "DARTS_RPO_2024-08-13";
    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmRpoService armRpoService;

    private GetExtendedProductionsByMatterServiceImpl getExtendedProductionsByMatterService;

    private ArmClientService armClientService;

    private UserAccountEntity userAccount;
    private static final Integer EXECUTION_ID = 1;
    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        when(armRpoService.getArmRpoExecutionDetailEntity(EXECUTION_ID)).thenReturn(armRpoExecutionDetailEntity);
        ArmRpoUtil armRpoUtil = new ArmRpoUtil(armRpoService);
        ArmClientService armClientService = new ArmClientServiceImpl(null, null, armRpoClient);
        getExtendedProductionsByMatterService = new GetExtendedProductionsByMatterServiceImpl(armClientService, armRpoService, armRpoUtil);
    }

    @Test
    void getExtendedProductionsByMatter_Success() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = new ExtendedProductionsByMatterResponse();
        extendedProductionsByMatterResponse.setStatus(200);
        extendedProductionsByMatterResponse.setIsError(false);
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setProductionId("12345");
        productions.setName(PRODUCTION_NAME);
        productions.setStartProductionTime("2025-01-16T12:30:02.9343888+00:00");
        productions.setEndProductionTime("2025-01-16T12:30:09.9129726+00:00");
        extendedProductionsByMatterResponse.setProductions(List.of(productions));

        armRpoExecutionDetailEntity.setMatterId("1");

        when(armRpoClient.getExtendedProductionsByMatter(anyString(), any())).thenReturn(extendedProductionsByMatterResponse);

        // when
        var result = getExtendedProductionsByMatterService.getExtendedProductionsByMatter("token", 1, PRODUCTION_NAME, userAccount);

        // then
        assertTrue(result);
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedProductionsByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedProductionsByMatter_ThrowsArmRpoException_WhenFeignExceptionIsThrown() {
        // given
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), anyString())).thenThrow(FeignException.class);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> getExtendedProductionsByMatterService.getExtendedProductionsByMatter("token", 1, PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO Extended Productions By Matter: Unable to get ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedProductionsByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedProductionsByMatter_ThrowsException_WithNullResponse() {
        // given
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), anyString())).thenReturn(null);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> getExtendedProductionsByMatterService.getExtendedProductionsByMatter("token", 1, PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO Extended Productions By Matter: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedProductionsByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedProductionsByMatter_ThrowsException_WithEmptyResponse() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = new ExtendedProductionsByMatterResponse();
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), anyString())).thenReturn(extendedProductionsByMatterResponse);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> getExtendedProductionsByMatterService.getExtendedProductionsByMatter("token", 1, PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO Extended Productions By Matter: ARM RPO API response is invalid"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedProductionsByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @Test
    void getExtendedSearchesByMatter_ThrowsException_WithMissingProductionId() {
        // given
        ExtendedProductionsByMatterResponse extendedProductionsByMatterResponse = new ExtendedProductionsByMatterResponse();
        extendedProductionsByMatterResponse.setStatus(200);
        extendedProductionsByMatterResponse.setIsError(false);
        ExtendedProductionsByMatterResponse.Productions productions = new ExtendedProductionsByMatterResponse.Productions();
        productions.setName(PRODUCTION_NAME);
        extendedProductionsByMatterResponse.setProductions(List.of(productions));
        when(armRpoClient.getExtendedProductionsByMatter(anyString(), anyString())).thenReturn(extendedProductionsByMatterResponse);
        armRpoExecutionDetailEntity.setMatterId("1");

        // when
        ArmRpoException armRpoException = assertThrows(
            ArmRpoException.class, () -> getExtendedProductionsByMatterService.getExtendedProductionsByMatter("token", 1, PRODUCTION_NAME, userAccount));

        // then
        assertThat(armRpoException.getMessage(), containsString(
            "Failure during ARM RPO Extended Productions By Matter: Production Id is missing from ARM RPO response"));
        verify(armRpoService).updateArmRpoStateAndStatus(any(),
                                                         eq(ARM_RPO_HELPER_MOCKS.getGetExtendedProductionsByMatterRpoState()),
                                                         eq(ARM_RPO_HELPER_MOCKS.getInProgressRpoStatus()),
                                                         any());
        verify(armRpoService).updateArmRpoStatus(any(), eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()), any());
        verifyNoMoreInteractions(armRpoService);
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }

}