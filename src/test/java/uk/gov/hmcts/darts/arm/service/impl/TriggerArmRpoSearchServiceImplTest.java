package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.rpo.ArmRpoApi;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.log.api.LogApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriggerArmRpoSearchServiceImplTest {

    private static final String BEARER_TOKEN = "some token";
    private static final Integer EXECUTION_ID = 1;
    private static final String MATTER_ID = "some matter id";
    private static final String SEARCH_NAME = "some search name";

    @Mock
    private ArmRpoApi armRpoApi;
    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private LogApi logApi;

    private TriggerArmRpoSearchServiceImpl triggerArmRpoSearchServiceImpl;
    private UserAccountEntity userAccount;
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @BeforeEach
    void setUp() {
        triggerArmRpoSearchServiceImpl = new TriggerArmRpoSearchServiceImpl(armRpoApi,
                                                                            armRpoService,
                                                                            armApiService,
                                                                            userIdentity,
                                                                            logApi);
        userAccount = new UserAccountEntity();
        when(userIdentity.getUserAccount())
            .thenReturn(userAccount);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        when(armRpoService.createArmRpoExecutionDetailEntity(any(UserAccountEntity.class)))
            .thenReturn(armRpoExecutionDetailEntity);

        when(armApiService.getArmBearerToken())
            .thenReturn(BEARER_TOKEN);
    }

    @Test
    void triggerArmRpoSearch_shouldCallExpectedApis() {
        // Given
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt()))
            .thenReturn(armRpoExecutionDetailEntity);

        when(armRpoApi.addAsyncSearch(anyString(), anyInt(), any(UserAccountEntity.class)))
            .thenReturn(SEARCH_NAME);

        // When
        triggerArmRpoSearchServiceImpl.triggerArmRpoSearch();

        // Then
        verify(armRpoService).createArmRpoExecutionDetailEntity(userAccount);
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(armRpoApi).getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, MATTER_ID, userAccount);
        verify(armRpoApi).getStorageAccounts(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(armRpoApi).getProfileEntitlements(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(armRpoApi).getMasterIndexFieldByRecordClassSchema(eq(BEARER_TOKEN), eq(EXECUTION_ID), any(), eq(userAccount));
        verify(armRpoApi).addAsyncSearch(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(armRpoApi).saveBackgroundSearch(BEARER_TOKEN, EXECUTION_ID, SEARCH_NAME, userAccount);
        verify(logApi).armRpoSearchSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(userIdentity);
        verifyNoMoreInteractions(armRpoService);
        verifyNoMoreInteractions(armApiService);
        verifyNoMoreInteractions(armRpoApi);
        verifyNoMoreInteractions(logApi);
    }

    @Test
    void triggerArmRpoSearch_shouldBubbleException_whenDownstreamApiThrowsException() {
        // Given
        doThrow(new ArmRpoException("some message"))
            .when(armRpoApi).getRecordManagementMatter(anyString(), anyInt(), any(UserAccountEntity.class));

        // When
        triggerArmRpoSearchServiceImpl.triggerArmRpoSearch();

        // Then
        verify(armRpoService).createArmRpoExecutionDetailEntity(userAccount);
        verify(armApiService).getArmBearerToken();
        verify(armRpoApi).getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(logApi).armRpoSearchFailed(EXECUTION_ID);

        verifyNoMoreInteractions(userIdentity);
        verifyNoMoreInteractions(armRpoService);
        verifyNoMoreInteractions(armApiService);
        verifyNoMoreInteractions(armRpoApi);
        verifyNoMoreInteractions(logApi);
    }

}