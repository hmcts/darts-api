package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.rpo.AddAsyncSearchService;
import uk.gov.hmcts.darts.arm.rpo.GetIndexesByMatterIdService;
import uk.gov.hmcts.darts.arm.rpo.GetMasterIndexFieldByRecordClassSchemaService;
import uk.gov.hmcts.darts.arm.rpo.GetProfileEntitlementsService;
import uk.gov.hmcts.darts.arm.rpo.GetRecordManagementMatterService;
import uk.gov.hmcts.darts.arm.rpo.GetStorageAccountsService;
import uk.gov.hmcts.darts.arm.rpo.SaveBackgroundSearchService;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class TriggerArmRpoSearchServiceImplTest {

    private static final String BEARER_TOKEN = "some token";
    private static final Integer EXECUTION_ID = 1;
    private static final String MATTER_ID = "some matter id";
    private static final String SEARCH_NAME = "some search name";

    @Mock
    private GetRecordManagementMatterService getRecordManagementMatterService;
    @Mock
    private GetIndexesByMatterIdService getIndexesByMatterIdService;
    @Mock
    private GetStorageAccountsService getStorageAccountsService;
    @Mock
    private GetProfileEntitlementsService getProfileEntitlementsService;
    @Mock
    private GetMasterIndexFieldByRecordClassSchemaService getMasterIndexFieldByRecordClassSchemaService;
    @Mock
    private AddAsyncSearchService addAsyncSearchService;
    @Mock
    private SaveBackgroundSearchService saveBackgroundSearchService;

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
        triggerArmRpoSearchServiceImpl = new TriggerArmRpoSearchServiceImpl(armRpoService,
                                                                            armApiService,
                                                                            userIdentity,
                                                                            logApi,
                                                                            getRecordManagementMatterService,
                                                                            getIndexesByMatterIdService,
                                                                            getStorageAccountsService,
                                                                            getProfileEntitlementsService,
                                                                            getMasterIndexFieldByRecordClassSchemaService,
                                                                            addAsyncSearchService,
                                                                            saveBackgroundSearchService);
        userAccount = new UserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccount);

        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(EXECUTION_ID);
        armRpoExecutionDetailEntity.setMatterId(MATTER_ID);
        lenient().when(armRpoService.createArmRpoExecutionDetailEntity(any(UserAccountEntity.class)))
            .thenReturn(armRpoExecutionDetailEntity);

        lenient().when(armApiService.getArmBearerToken()).thenReturn(BEARER_TOKEN);
    }

    @Test
    void triggerArmRpoSearch_shouldCallExpectedApis() {
        // Given
        Duration threadSleepDuration = Duration.ofMillis(1);
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);

        when(addAsyncSearchService.addAsyncSearch(anyString(), anyInt(), any(UserAccountEntity.class))).thenReturn(SEARCH_NAME);

        // When
        triggerArmRpoSearchServiceImpl.triggerArmRpoSearch(threadSleepDuration);

        // Then
        verify(armRpoService).createArmRpoExecutionDetailEntity(userAccount);
        verify(armApiService).getArmBearerToken();
        verify(getRecordManagementMatterService).getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(getIndexesByMatterIdService).getIndexesByMatterId(BEARER_TOKEN, EXECUTION_ID, MATTER_ID, userAccount);
        verify(getStorageAccountsService).getStorageAccounts(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(getProfileEntitlementsService).getProfileEntitlements(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(getMasterIndexFieldByRecordClassSchemaService).getMasterIndexFieldByRecordClassSchema(eq(BEARER_TOKEN), eq(EXECUTION_ID), any(),
                                                                                                     eq(userAccount));
        verify(addAsyncSearchService).addAsyncSearch(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(saveBackgroundSearchService).saveBackgroundSearch(BEARER_TOKEN, EXECUTION_ID, SEARCH_NAME, userAccount);
        verify(logApi).armRpoSearchSuccessful(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoService);
        verifyNoMoreInteractions(armApiService);
        verifyNoMoreInteractions(getRecordManagementMatterService);
        verifyNoMoreInteractions(getIndexesByMatterIdService);
        verifyNoMoreInteractions(getStorageAccountsService);
        verifyNoMoreInteractions(getProfileEntitlementsService);
        verifyNoMoreInteractions(getMasterIndexFieldByRecordClassSchemaService);
        verifyNoMoreInteractions(addAsyncSearchService);
        verifyNoMoreInteractions(saveBackgroundSearchService);
        verifyNoMoreInteractions(logApi);
    }

    @Test
    void triggerArmRpoSearch_shouldBubbleException_whenDownstreamApiThrowsException() {
        // Given
        Duration threadSleepDuration = Duration.ofMillis(1);
        doThrow(new ArmRpoException("some message"))
            .when(getRecordManagementMatterService).getRecordManagementMatter(anyString(), anyInt(), any(UserAccountEntity.class));

        // When
        triggerArmRpoSearchServiceImpl.triggerArmRpoSearch(threadSleepDuration);

        // Then
        verify(armRpoService).createArmRpoExecutionDetailEntity(userAccount);
        verify(armApiService).getArmBearerToken();
        verify(getRecordManagementMatterService).getRecordManagementMatter(BEARER_TOKEN, EXECUTION_ID, userAccount);
        verify(logApi).armRpoSearchFailed(EXECUTION_ID);

        verifyNoMoreInteractions(armRpoService);
        verifyNoMoreInteractions(armApiService);
        verifyNoMoreInteractions(getRecordManagementMatterService);
        verifyNoMoreInteractions(logApi);
    }

    @SuppressWarnings("PMD.DoNotUseThreads")
    @Test
    void sleep_shouldHandleInterruptedException() {
        // Given
        Duration threadSleepDuration = Duration.ofMillis(5000);

        try (ExecutorService executor = Executors.newFixedThreadPool(1)) {
            Thread thread = new Thread(() -> {
                log.info("Thread started");
                triggerArmRpoSearchServiceImpl.sleep(threadSleepDuration);
                log.info("Thread finished");
            });

            // When
            executor.submit(() -> {
                thread.start();
            });

            Thread.currentThread().interrupt(); // Simulate an interrupt

            // Then
            assertTrue(thread.interrupted());
        }

    }
}