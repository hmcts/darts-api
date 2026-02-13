package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.RemoveRpoProductionsService;
import uk.gov.hmcts.darts.arm.util.ArmRpoUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveRpoProductionsServiceImplTest {
    
    private static final int BATCH_SIZE = 50;
    private static final String BEARER_TOKEN = "some token";
    
    @Mock
    private RemoveRpoProductionsService removeRpoProductionsService;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private LogApi logApi;
    @Mock
    private ArmRpoApi armRpoApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ArmRpoUtil armRpoUtil;
    @Mock
    private UserAccountEntity userAccountEntity;

    private static final Integer EXECUTION_ID = 1;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private final Duration waitDuration = Duration.ofDays(30);
    
    @BeforeEach
    void setUp() {
        removeRpoProductionsService = new RemoveRpoProductionsServiceImpl(
              logApi, userIdentity, armRpoService, armRpoUtil, armRpoApi);

        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);
        
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        lenient().when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
    }
    
    @Test
    void removeOldArmRpoProductions_ShouldRemoveProductions_WhenRemovableExecutionsExist() {
        // given
        when(armRpoService.findIdsByStatusAndLastModifiedDateTimeAfter(
            eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
            any(OffsetDateTime.class)
        )).thenReturn(java.util.List.of(EXECUTION_ID));
        when(armRpoUtil.getBearerToken(any())).thenReturn(BEARER_TOKEN);
        
        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, BATCH_SIZE);

        // then
        verify(armRpoApi).removeProduction(BEARER_TOKEN, 1, userAccountEntity);
        verify(logApi).removeOldArmRpoProductionsSuccessful(EXECUTION_ID);
    }
    
    @Test
    void removeOldArmRpoProductions_ShouldNotRemoveProductions_WhenNoRemovableExecutionsExist() {
        // given
        var currentTime = OffsetDateTime.now();
        armRpoExecutionDetailEntity.setArmRpoStatus(ARM_RPO_HELPER_MOCKS.getCompletedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ARM_RPO_HELPER_MOCKS.getRemoveProductionRpoState());
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setLastModifiedDateTime(
            currentTime.minusDays(20)
        );

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, BATCH_SIZE);

        // then
        verify(armRpoApi, org.mockito.Mockito.times(0)).removeProduction(any(), any(), any());
        verify(logApi, org.mockito.Mockito.times(0)).removeOldArmRpoProductionsSuccessful(EXECUTION_ID);
    }
    
    @Test
    void removeOldArmRpoProductions_ShouldHandleException_WhenRpoServiceThrowsException() {
        // given
        when(armRpoService.findIdsByStatusAndLastModifiedDateTimeAfter(
            eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
            any(OffsetDateTime.class)
        )).thenThrow(new RuntimeException("Mocked RuntimeException"));

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, BATCH_SIZE);

        // then
        verify(logApi).removeOldArmRpoProductionsFailed();
        verify(armRpoApi, org.mockito.Mockito.times(0)).removeProduction(any(), any(), any());
        verify(logApi, org.mockito.Mockito.times(0)).removeOldArmRpoProductionsSuccessful(EXECUTION_ID);

    }

    @Test
    void removeOldArmRpoProductions_ShouldHandleException_WhenArmApiServiceThrowsException() {
        // given
        when(armRpoService.findIdsByStatusAndLastModifiedDateTimeAfter(
            eq(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus()),
            any(OffsetDateTime.class)
        )).thenReturn(java.util.List.of(EXECUTION_ID));
        
        doThrow(new RuntimeException("Mocked RuntimeException"))
            .when(armRpoApi)
            .removeProduction(any(), any(), any());

        // when
        removeRpoProductionsService.removeOldArmRpoProductions(false, waitDuration, BATCH_SIZE);

        // then
        verify(logApi).removeOldArmRpoProductionsFailed(EXECUTION_ID);
        verify(logApi, org.mockito.Mockito.times(0)).removeOldArmRpoProductionsSuccessful(EXECUTION_ID);
    }
}
