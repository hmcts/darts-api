package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoReplayServiceImplTest {

    @Mock
    private AutomatedTaskService automatedTaskService;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private UserIdentity userIdentity;

    private ArmRpoReplayServiceImpl armRpoReplayService;

    @BeforeEach
    void setUp() {
        armRpoReplayService = new ArmRpoReplayServiceImpl(
            automatedTaskService,
            externalObjectDirectoryRepository,
            userIdentity
        );
    }

    @Test
    void replayArmRpo_noEodsFound() {
        // given
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        when(automatedTaskService.getArmAutomatedTaskEntity(AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME))
            .thenReturn(armAutomatedTaskEntity);
        when(externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            any(), any(), any(), any(), any(Limit.class)
        )).thenReturn(List.of());

        // when
        armRpoReplayService.replayArmRpo(100);

        // then
        verify(externalObjectDirectoryRepository, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void replayArmRpo_eodsFound() {
        // given
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        OffsetDateTime startTs = OffsetDateTime.now();
        OffsetDateTime endTs = OffsetDateTime.now().plusHours(10);
        when(armAutomatedTaskEntity.getArmReplayStartTs()).thenReturn(startTs);
        when(armAutomatedTaskEntity.getArmReplayEndTs()).thenReturn(endTs);
        when(automatedTaskService.getArmAutomatedTaskEntity(AutomatedTaskName.ARM_RPO_REPLAY_TASK_NAME))
            .thenReturn(armAutomatedTaskEntity);
        when(externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            any(), eq(startTs), eq(endTs), any(), any(Limit.class)
        )).thenReturn(List.of(22, 14));
        UserAccountEntity userAccountEntity = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        // when
        armRpoReplayService.replayArmRpo(100);

        // then
        verify(externalObjectDirectoryRepository).updateEodStatusAndTransferAttemptsWhereIdIn(
            EodHelper.failedArmRawDataStatus(),
            0,
            userAccountEntity,
            List.of(22, 14)
        );
    }
}