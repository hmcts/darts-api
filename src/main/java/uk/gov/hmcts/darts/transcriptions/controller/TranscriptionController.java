package uk.gov.hmcts.darts.transcriptions.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionApi;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;

import static java.util.Objects.isNull;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TranscriptionController implements TranscriptionApi {

    private final TranscriptionService transcriptionService;
    private final CaseService caseService;
    private final HearingsService hearingsService;


    @Override
    public ResponseEntity<Void> requestTranscription(TranscriptionRequestDetails transcriptionRequestDetails) {
        validateTranscriptionRequestValues(transcriptionRequestDetails);
        try {
            transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EntityNotFoundException exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private boolean validateTranscriptionRequestValues(TranscriptionRequestDetails transcriptionRequestDetails) {
        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        } else if (!isNull(transcriptionRequestDetails.getHearingId())) {
            hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
        } else {
            caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId());
        }

        Integer transcriptionTypeId = transcriptionRequestDetails.getTranscriptionTypeId();

        if (transcriptionTypesThatRequireDates(transcriptionTypeId)
            && !transcriptionDatesAreSet(transcriptionRequestDetails.getStartDateTime(), transcriptionRequestDetails.getEndDateTime())
        ) {
            log.error("This transcription type {} requires both the start date ({}) and end dates ({})",
                      transcriptionRequestDetails.getTranscriptionTypeId(), transcriptionRequestDetails.getStartDateTime(),
                      transcriptionRequestDetails.getEndDateTime());
            throw new DartsApiException(TranscriptionError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }

        return true;
    }

    private boolean transcriptionTypesThatRequireDates(Integer transcriptionTypeId) {
        return TranscriptionTypeEnum.SPECIFIED_TIMES.getTranscriptionTypeKey().equals(transcriptionTypeId)
            || TranscriptionTypeEnum.COURT_LOG.getTranscriptionTypeKey().equals(transcriptionTypeId);
    }

    private boolean transcriptionDatesAreSet(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        return !isNull(startDateTime) || !isNull(endDateTime);
    }
}
