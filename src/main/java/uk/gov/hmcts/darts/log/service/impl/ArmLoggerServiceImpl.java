package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.log.service.ArmLoggerService;

@Service
@AllArgsConstructor
@Slf4j
public class ArmLoggerServiceImpl implements ArmLoggerService {

    private static final String ARM_PUSH_SUCCESSFUL = "Successfully pushed object to ARM dropzone: eod_id={eodId}";

    private static final String ARM_PUSHED_FAILED = "Failed to push object to ARM dropzone: eod_id={eodId}";

    private static final String ARCHIVE_TO_ARM_SUCCESSFUL = "Successfully archived object to ARM: eod_id={eodId}";

    private static final String ARCHIVE_TO_ARM_FAILED = "Failed to archive object to ARM: eod_id={eodId}";


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
        log.info(ARCHIVE_TO_ARM_FAILED, eodId);
    }
}
