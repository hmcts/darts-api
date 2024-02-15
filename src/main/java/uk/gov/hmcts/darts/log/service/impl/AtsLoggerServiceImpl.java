package uk.gov.hmcts.darts.log.service.impl;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.log.service.AtsLoggerService;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;

@Service
@NoArgsConstructor
@Slf4j
public class AtsLoggerServiceImpl implements AtsLoggerService {

    private static final String ATS_REQUEST_PROCESS_STARTED = "ATS request process started: med_req_id={}, hearing_id={}";
    private static final String ATS_REQUEST_PROCESS_SUCCESS = "ATS request processed successfully: med_req_id={}, hearing_id={}";
    private static final String ATS_REQUEST_PROCESS_FAILED = "ATS request process failed: med_req_id={}, hearing_id={}";

    @Override
    public void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity) {

        if (mediaRequestEntity.getStatus().equals(FAILED)) {
            logProcessingException(mediaRequestEntity.getId(), mediaRequestEntity.getHearing().getId());
        } else {
            logProcessingInfo(mediaRequestEntity.getStatus(), mediaRequestEntity.getId(), mediaRequestEntity.getHearing().getId());
        }
    }

    private void logProcessingInfo(MediaRequestStatus mediaRequestStatus, Integer requestId, Integer hearingId) {

        String logMessage = "";

        if (mediaRequestStatus.equals(COMPLETED)) {
            logMessage = ATS_REQUEST_PROCESS_SUCCESS;
        }
        if (mediaRequestStatus.equals(PROCESSING)) {
            logMessage = ATS_REQUEST_PROCESS_STARTED;
        }

        log.info(logMessage, requestId, hearingId);
    }

    private void logProcessingException(Integer requestId, Integer hearingId) {
        log.error(ATS_REQUEST_PROCESS_FAILED, requestId, hearingId);
    }
}
