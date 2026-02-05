package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.service.ArmRpoBacklogCatchupService;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.arm.service.TriggerArmRpoSearchService;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArmRpoBacklogCatchupServiceImpl implements ArmRpoBacklogCatchupService {

    private final ArmRpoService armRpoService;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;
    private final TriggerArmRpoSearchService triggerArmRpoSearchService;
    private final CurrentTimeHelper currentTimeHelper;

    public void performCatchup(Integer batchSize, Integer maxHoursEndingPoint, Integer totalCatchupHours, Duration threadSleepDuration) {
        var armRpoExecutionDetailEntity = armRpoService.getLatestArmRpoExecutionDetailEntity();
        if (!validateTaskCanBeRun(maxHoursEndingPoint, totalCatchupHours, armRpoExecutionDetailEntity)) {
            return;
        }
        StringBuilder errorMessage = new StringBuilder();
        ArmAutomatedTaskEntity armAutomatedTaskEntity = armRpoService.getArmAutomatedTaskEntity(errorMessage);
        if (isNull(armAutomatedTaskEntity)) {
            log.error("Failed to retrieve ArmAutomatedTaskEntity: {}", errorMessage);
            return;
        }
        armAutomatedTaskEntity.setRpoCsvStartHour(maxHoursEndingPoint);
        armAutomatedTaskEntity.setRpoCsvEndHour(maxHoursEndingPoint + totalCatchupHours);
        armAutomatedTaskRepository.save(armAutomatedTaskEntity);
        triggerArmRpoSearchService.triggerArmRpoSearch(threadSleepDuration);
    }

    private boolean validateTaskCanBeRun(Integer maxHoursEndingPoint, Integer totalCatchupHours,
                                         ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity) {
        if (isNull(armRpoExecutionDetailEntity) || isNull(armRpoExecutionDetailEntity.getArmRpoState())) {
            log.info("ARM RPO state is not valid, skipping backlog catchup.");
            return false;
        }
        if (!ArmRpoHelper.removeProductionRpoState().getId().equals(armRpoExecutionDetailEntity.getArmRpoState().getId())
            && !ArmRpoHelper.failedRpoStatus().getId().equals(armRpoExecutionDetailEntity.getArmRpoStatus().getId())) {
            log.info("Last ARM RPO execution is not in REMOVE_PRODUCTION state or FAILED status, skipping backlog catchup.");
            return false;
        }
        // Only perform backlog catchup if the last execution is in REMOVE_PRODUCTION state or FAILED status
        var earliestEodInRpo = externalObjectDirectoryRepository.findOldestByStatusAndLocation(EodHelper.armRpoPendingStatus(), EodHelper.armLocation());
        if (isNull(earliestEodInRpo)) {
            log.info("No EODs found in ARM RPO pending status for backlog catchup.");
            return false;
        }

        // check the earliest EOD date is greater than the maxHoursEndingPoint plus totalCatchupHours
        OffsetDateTime currentTime = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime earliestEodDateTime = currentTime.minus(maxHoursEndingPoint + totalCatchupHours, ChronoUnit.HOURS);
        if (earliestEodInRpo.getCreatedDateTime().isAfter(earliestEodDateTime)) {
            log.info("Earliest EODs found in ARM RPO pending status is not suitable for backlog catchup.");
            return false;
        }
        return true;
    }
}
