package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoReplayAutomatedTaskTest {

    @Mock
    private AutomatedTaskService automatedTaskService;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private UserIdentity userIdentity;

    private ArmRpoReplayAutomatedTask armRpoReplayAutomatedTask;

    @BeforeEach
    void beforeEach() {
        this.armRpoReplayAutomatedTask = spy(
            new ArmRpoReplayAutomatedTask(
                null,
                null,
                null,
                null,
                automatedTaskService,
                externalObjectDirectoryRepository,
                objectRecordStatusRepository,
                userIdentity
            )
        );
    }

    @Test
    void positiveGetAutomatedTaskName() {
        assertThat(armRpoReplayAutomatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME);
    }

    @Test
    void positiveRunTask() {
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        OffsetDateTime startTs = OffsetDateTime.now();
        OffsetDateTime endTs = OffsetDateTime.now().plusHours(10);
        when(armAutomatedTaskEntity.getArmReplayStartTs()).thenReturn(startTs);
        when(armAutomatedTaskEntity.getArmReplayEndTs()).thenReturn(endTs);

        when(automatedTaskService.getArmAutomatedTaskEntity(AutomatedTaskName.PROCESS_E2E_ARM_PENDING_TASK_NAME))
            .thenReturn(armAutomatedTaskEntity);

        ObjectRecordStatusEntity armReplay = mock(ObjectRecordStatusEntity.class);
        ObjectRecordStatusEntity armRawDataFailed = mock(ObjectRecordStatusEntity.class);
        when(objectRecordStatusRepository.getReferenceById(22)).thenReturn(armReplay);
        when(objectRecordStatusRepository.getReferenceById(14)).thenReturn(armRawDataFailed);

        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);


        armRpoReplayAutomatedTask.runTask();

        verify(automatedTaskService).getArmAutomatedTaskEntity(AutomatedTaskName.PROCESS_E2E_ARM_PENDING_TASK_NAME);
        verify(objectRecordStatusRepository).getReferenceById(22);
        verify(objectRecordStatusRepository).getReferenceById(14);
        verify(armAutomatedTaskEntity).getArmReplayStartTs();
        verify(armAutomatedTaskEntity).getArmReplayEndTs();
        verify(userIdentity).getUserAccount();
        verify(externalObjectDirectoryRepository).updateEodStatusAndTransferAttemptsWhereLastModifiedIsBetweenTwoDateTimesAndHasStatus(
            armRawDataFailed,
            0,
            armReplay,
            startTs,
            endTs,
            userAccount
        );
    }
}
