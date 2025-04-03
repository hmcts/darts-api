package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessArmRpoPendingAutomatedTaskTest {

    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private UserIdentity userIdentity;

    private MockedStatic<EodHelper> eodHelperMockedStatic;

    private ProcessArmRpoPendingAutomatedTask setupProcessArmRpoPendingAutomatedTask(Duration duration) {
        return spy(new ProcessArmRpoPendingAutomatedTask(
            automatedTaskRepository,
            null,
            logApi,
            lockService,
            externalObjectDirectoryRepository,
            currentTimeHelper,
            userIdentity,
            duration
        ));
    }


    @BeforeEach
    void before() {
        eodHelperMockedStatic = Mockito.mockStatic(EodHelper.class);
    }

    @AfterEach
    void after() {
        if (eodHelperMockedStatic != null) {
            eodHelperMockedStatic.close();
        }
    }

    @Test
    void positiveGetAutomatedTaskName() {
        ProcessArmRpoPendingAutomatedTask processArmRpoPendingAutomatedTask = setupProcessArmRpoPendingAutomatedTask(Duration.ofDays(1));
        assertThat(processArmRpoPendingAutomatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.PROCESS_ARM_RPO_PENDING);
    }

    @Test
    void positiveRunTaskDuration1H() {
        ProcessArmRpoPendingAutomatedTask processArmRpoPendingAutomatedTask = setupProcessArmRpoPendingAutomatedTask(Duration.ofHours(1));
        ObjectRecordStatusEntity objectRecordStatusEntityPending = mock(ObjectRecordStatusEntity.class);
        ObjectRecordStatusEntity objectRecordStatusEntityStored = mock(ObjectRecordStatusEntity.class);

        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);

        eodHelperMockedStatic.when(EodHelper::armRpoPendingStatus).thenReturn(objectRecordStatusEntityPending);
        eodHelperMockedStatic.when(EodHelper::storedStatus).thenReturn(objectRecordStatusEntityStored);

        doReturn(50).when(processArmRpoPendingAutomatedTask).getAutomatedTaskBatchSize();

        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        int userId = 1;
        when(userAccount.getId()).thenReturn(userId);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        processArmRpoPendingAutomatedTask.runTask();

        verify(externalObjectDirectoryRepository)
            .updateByStatusEqualsAndDataIngestionTsBefore(
                objectRecordStatusEntityPending,
                currentTime.minus(Duration.ofHours(1)),
                objectRecordStatusEntityStored,
                userId,
                Limit.of(50)
            );

        verify(currentTimeHelper).currentOffsetDateTime();
        eodHelperMockedStatic.verify(EodHelper::armRpoPendingStatus, times(1));
        eodHelperMockedStatic.verify(EodHelper::storedStatus, times(1));
        verify(processArmRpoPendingAutomatedTask).getAutomatedTaskBatchSize();
    }

    @Test
    void positiveRunTaskDuration24H() {
        ProcessArmRpoPendingAutomatedTask processArmRpoPendingAutomatedTask = setupProcessArmRpoPendingAutomatedTask(Duration.ofHours(24));
        ObjectRecordStatusEntity objectRecordStatusEntityPending = mock(ObjectRecordStatusEntity.class);
        ObjectRecordStatusEntity objectRecordStatusEntityStored = mock(ObjectRecordStatusEntity.class);

        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);

        eodHelperMockedStatic.when(EodHelper::armRpoPendingStatus).thenReturn(objectRecordStatusEntityPending);
        eodHelperMockedStatic.when(EodHelper::storedStatus).thenReturn(objectRecordStatusEntityStored);

        doReturn(75).when(processArmRpoPendingAutomatedTask).getAutomatedTaskBatchSize();

        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        int userId = 1;
        when(userAccount.getId()).thenReturn(userId);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        processArmRpoPendingAutomatedTask.runTask();

        verify(externalObjectDirectoryRepository)
            .updateByStatusEqualsAndDataIngestionTsBefore(
                objectRecordStatusEntityPending,
                currentTime.minus(Duration.ofHours(24)),
                objectRecordStatusEntityStored,
                userId,
                Limit.of(75)
            );

        verify(currentTimeHelper).currentOffsetDateTime();
        eodHelperMockedStatic.verify(EodHelper::armRpoPendingStatus, times(1));
        eodHelperMockedStatic.verify(EodHelper::storedStatus, times(1));
        verify(processArmRpoPendingAutomatedTask).getAutomatedTaskBatchSize();
    }
}
