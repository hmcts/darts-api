package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.TriggerArmRpoSearchService;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoBacklogCatchupServiceImplTest {

    private static final int BATCH_SIZE = 10;
    private static final int MAX_HOURS_ENDING_POINT = 50;
    private static final int TOTAL_CATCHUP_HOURS = 12;

    @Mock
    private ArmRpoService armRpoService;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmAutomatedTaskRepository armAutomatedTaskRepository;
    @Mock
    private TriggerArmRpoSearchService triggerArmRpoSearchService;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private ArmRpoHelperMocks armRpoHelperMocks;

    private ArmRpoBacklogCatchupServiceImpl service;

    private final Duration sleepDuration = Duration.ofMillis(BATCH_SIZE);

    @BeforeEach
    void setupData() {
        armRpoHelperMocks = new ArmRpoHelperMocks();
        service = new ArmRpoBacklogCatchupServiceImpl(
            armRpoService,
            externalObjectDirectoryRepository,
            armAutomatedTaskRepository,
            triggerArmRpoSearchService,
            currentTimeHelper
        );
    }

    @Test
    void performCatchup_doesNothing_whenNoLatestExecution() {
        // given
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(null);

        // when
        service.performCatchup(BATCH_SIZE, MAX_HOURS_ENDING_POINT, TOTAL_CATCHUP_HOURS, sleepDuration);

        // then
        verifyNoInteractions(armAutomatedTaskRepository, triggerArmRpoSearchService);
    }

    @Test
    void performCatchup_doesNothing_whenStateIsInProgress() {
        // given
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        ArmRpoStateEntity state = ArmRpoHelper.getProfileEntitlementsRpoState();
        armRpoExecutionDetailEntity.setId(1);
        armRpoExecutionDetailEntity.setArmRpoState(state);
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);

        // when
        service.performCatchup(BATCH_SIZE, MAX_HOURS_ENDING_POINT, TOTAL_CATCHUP_HOURS, sleepDuration);

        // then
        verifyNoInteractions(armAutomatedTaskRepository, triggerArmRpoSearchService);
    }

    @Test
    void performCatchup_doesNothing_whenStatusIsFailed() {
        // given
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        ArmRpoStateEntity state = ArmRpoHelper.removeProductionRpoState();
        armRpoExecutionDetailEntity.setId(1);
        armRpoExecutionDetailEntity.setArmRpoState(state);
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.failedRpoStatus());
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);

        // when
        service.performCatchup(BATCH_SIZE, MAX_HOURS_ENDING_POINT, TOTAL_CATCHUP_HOURS, sleepDuration);

        // then
        verifyNoInteractions(armAutomatedTaskRepository, triggerArmRpoSearchService);
    }

    @Test
    void performCatchup_doesNothing_whenNoEodsToProcess() {
        // given
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(1);
        ArmRpoStateEntity removeProductionState = ArmRpoHelper.removeProductionRpoState();
        armRpoExecutionDetailEntity.setArmRpoState(removeProductionState);
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);

        when(externalObjectDirectoryRepository.findOldestByInputUploadProcessedTsAndStatusAndLocation(any(), any()))
            .thenReturn(null);

        // when
        service.performCatchup(BATCH_SIZE, MAX_HOURS_ENDING_POINT, TOTAL_CATCHUP_HOURS, sleepDuration);

        // then
        verifyNoInteractions(armAutomatedTaskRepository, triggerArmRpoSearchService);
    }

    @Test
    void performCatchup_doesNothing_whenEarliestEodTooRecent() {
        // given
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        ArmRpoStateEntity validState = ArmRpoHelper.removeProductionRpoState();
        armRpoExecutionDetailEntity.setArmRpoState(validState);
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setId(1);
        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);

        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setCreatedDateTime(OffsetDateTime.now().minusHours(1)); // recent
        eod.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(1)); // recent
        when(externalObjectDirectoryRepository.findOldestByInputUploadProcessedTsAndStatusAndLocation(any(), any()))
            .thenReturn(eod);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        service.performCatchup(BATCH_SIZE, 4, 1, sleepDuration);

        // then
        verifyNoInteractions(armAutomatedTaskRepository, triggerArmRpoSearchService);
    }

    @Test
    void performCatchup_runsSuccessfully_whenValid() {
        // given
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        ArmRpoStateEntity removeProduction = ArmRpoHelper.removeProductionRpoState();
        armRpoExecutionDetailEntity.setArmRpoState(removeProduction);
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());

        when(armRpoService.getLatestArmRpoExecutionDetailEntity()).thenReturn(armRpoExecutionDetailEntity);

        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        OffsetDateTime earliest = OffsetDateTime.now().minusHours(100);
        eod.setCreatedDateTime(earliest);
        eod.setInputUploadProcessedTs(earliest);
        when(externalObjectDirectoryRepository.findOldestByInputUploadProcessedTsAndStatusAndLocation(any(), any()))
            .thenReturn(eod);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        ArmAutomatedTaskEntity automatedTask = new ArmAutomatedTaskEntity();
        automatedTask.setRpoCsvStartHour(MAX_HOURS_ENDING_POINT);
        automatedTask.setRpoCsvEndHour(MAX_HOURS_ENDING_POINT + TOTAL_CATCHUP_HOURS);
        when(armRpoService.getArmAutomatedTaskEntity(any())).thenReturn(automatedTask);

        // when
        service.performCatchup(BATCH_SIZE, MAX_HOURS_ENDING_POINT, TOTAL_CATCHUP_HOURS, sleepDuration);

        // then
        verify(armAutomatedTaskRepository).save(automatedTask);
        verify(triggerArmRpoSearchService).triggerArmRpoSearch(sleepDuration);

    }

    @AfterEach
    void close() {
        if (armRpoHelperMocks != null) {
            armRpoHelperMocks.close();
        }
    }
}
