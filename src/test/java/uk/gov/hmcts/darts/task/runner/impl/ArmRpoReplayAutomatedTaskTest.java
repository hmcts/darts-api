package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.arm.service.impl.ArmRpoReplayServiceImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;

import java.time.OffsetDateTime;
import java.util.List;

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
    private UserIdentity userIdentity;

    private static EodHelperMocks eodHelperMocks;

    private ArmRpoReplayAutomatedTask armRpoReplayAutomatedTask;

    @BeforeEach
    void beforeEach() {
        eodHelperMocks = new EodHelperMocks();
        eodHelperMocks.simulateInitWithMockedData();

        ArmRpoReplayServiceImpl armRpoReplayService = new ArmRpoReplayServiceImpl(
            automatedTaskService,
            externalObjectDirectoryRepository,
            userIdentity
        );

        AutomatedTaskRepository automatedTaskRepository = mock(AutomatedTaskRepository.class);
        this.armRpoReplayAutomatedTask = spy(
            new ArmRpoReplayAutomatedTask(
                automatedTaskRepository,
                null,
                null,
                null,
                armRpoReplayService
            )
        );
    }

    @AfterEach
    void afterEach() {
        eodHelperMocks.close();
    }

    @Test
    void positiveGetAutomatedTaskName() {
        assertThat(armRpoReplayAutomatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME);
    }

    @Test
    void positiveRunTask() {
        // given
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        OffsetDateTime startTs = OffsetDateTime.now();
        OffsetDateTime endTs = OffsetDateTime.now().plusHours(10);
        when(armAutomatedTaskEntity.getArmReplayStartTs()).thenReturn(startTs);
        when(armAutomatedTaskEntity.getArmReplayEndTs()).thenReturn(endTs);

        when(automatedTaskService.getArmAutomatedTaskEntity(AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME))
            .thenReturn(armAutomatedTaskEntity);
        when(externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            EodHelper.armReplayStatus(),
            startTs,
            endTs,
            EodHelper.armLocation(),
            Limit.of(0)
        )).thenReturn(List.of(22, 14));

        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        int userId = 123;
        when(userAccount.getId()).thenReturn(userId);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        // when
        armRpoReplayAutomatedTask.runTask();

        // then
        verify(automatedTaskService).getArmAutomatedTaskEntity(AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME);
        verify(armAutomatedTaskEntity).getArmReplayStartTs();
        verify(armAutomatedTaskEntity).getArmReplayEndTs();
        verify(userIdentity).getUserAccount();
        verify(externalObjectDirectoryRepository).updateEodStatusAndTransferAttemptsWhereIdIn(
            EodHelper.failedArmRawDataStatus(),
            0,
            userId,
            List.of(22, 14)
        );
    }
}
