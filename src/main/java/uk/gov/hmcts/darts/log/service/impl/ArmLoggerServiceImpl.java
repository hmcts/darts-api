package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.log.service.ArmLoggerService;
import uk.gov.hmcts.darts.util.DurationUtil;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class ArmLoggerServiceImpl implements ArmLoggerService {

    private static final String ARM_PUSH_SUCCESSFUL = "Successfully pushed object to ARM dropzone: eod_id={}";
    private static final String ARM_PUSHED_FAILED = "Failed to push object to ARM dropzone: eod_id={}";
    private static final String ARCHIVE_TO_ARM_SUCCESSFUL = "Successfully archived object to ARM: eod_id={}";
    private static final String ARCHIVE_TO_ARM_FAILED = "Failed to archive object to ARM: eod_id={}";

    private static final String ARM_MISSING_RESPONSE = "No response files produced by ARM within {} for EOD {}";


    private static final String ARM_RPO_SEARCH_SUCCESSFULLY_COMPLETED_FOR_EXECUTION_ID = "ARM RPO Search - Successfully completed for execution Id = {}";
    private static final String ARM_RPO_SEARCH_FAILED_FOR_EXECUTION_ID = "ARM RPO Search - Failed for execution Id = {}";
    private static final String ARM_RPO_POLLING_SUCCESSFULLY_COMPLETED_FOR_EXECUTION_ID = "ARM RPO Polling - Successfully completed for execution Id = {}";
    private static final String ARM_RPO_POLLING_FAILED_FOR_EXECUTION_ID = "ARM RPO Polling - Failed for execution Id = {}";

    @Override
    public void armPushSuccessful(Integer eodId) {
        log.info(ARM_PUSH_SUCCESSFUL, eodId);
    }

    @Override
    public void armPushFailed(Integer eodId) {
        log.error(ARM_PUSHED_FAILED, eodId);
    }

    @Override
    public void archiveToArmSuccessful(Integer eodId) {
        log.info(ARCHIVE_TO_ARM_SUCCESSFUL, eodId);
    }

    @Override
    public void archiveToArmFailed(Integer eodId) {
        log.error(ARCHIVE_TO_ARM_FAILED, eodId);
    }

    @Override
    public void armRpoSearchSuccessful(Integer executionId) {
        log.info(ARM_RPO_SEARCH_SUCCESSFULLY_COMPLETED_FOR_EXECUTION_ID, executionId);
    }

    @Override
    public void armRpoSearchFailed(Integer executionId) {
        log.error(ARM_RPO_SEARCH_FAILED_FOR_EXECUTION_ID, executionId);
    }

    @Override
    public void armRpoPollingSuccessful(Integer executionId) {
        log.info(ARM_RPO_POLLING_SUCCESSFULLY_COMPLETED_FOR_EXECUTION_ID, executionId);
    }

    @Override
    public void logArmMissingResponse(Duration armMissingResponseDuration, Integer eodId) {
        log.error(ARM_MISSING_RESPONSE, DurationUtil.formatDurationHumanReadable(armMissingResponseDuration), eodId);
    }

    @Override
    public void armRpoPollingFailed(Integer executionId) {
        log.error(ARM_RPO_POLLING_FAILED_FOR_EXECUTION_ID, executionId);
    }

}
