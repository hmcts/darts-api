package uk.gov.hmcts.darts.transcriptions.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionApi;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.COURT_LOG;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TranscriptionController implements TranscriptionApi {

    private final TranscriptionService transcriptionService;
    private final CaseService caseService;
    private final HearingsService hearingsService;


    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<RequestTranscriptionResponse> requestTranscription(
        TranscriptionRequestDetails transcriptionRequestDetails) {
        validateTranscriptionRequestValues(transcriptionRequestDetails);
        try {
            return new ResponseEntity<>(
                transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails),
                HttpStatus.OK
            );
        } catch (EntityNotFoundException exception) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID, securityRoles = {APPROVER, TRANSCRIBER})
    public ResponseEntity<UpdateTranscriptionResponse> updateTranscription(Integer transcriptionId,
                                                                           UpdateTranscription updateTranscription) {

        return new ResponseEntity<>(
            transcriptionService.updateTranscription(transcriptionId, updateTranscription),
            HttpStatus.OK
        );
    }

    private void validateTranscriptionRequestValues(TranscriptionRequestDetails transcriptionRequestDetails) {

        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        } else if (nonNull(transcriptionRequestDetails.getHearingId())) {
            hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
        } else {
            caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId());
        }

        Integer transcriptionTypeId = transcriptionRequestDetails.getTranscriptionTypeId();
        TranscriptionTypeEnum.fromId(transcriptionTypeId);
        TranscriptionUrgencyEnum.fromId(transcriptionRequestDetails.getUrgencyId());

        if (transcriptionTypesThatRequireDates(transcriptionTypeId)
            && !transcriptionDatesAreSet(
            transcriptionRequestDetails.getStartDateTime(),
            transcriptionRequestDetails.getEndDateTime()
        )) {
            log.error(
                "This transcription type {} requires both the start date ({}) and end dates ({})",
                transcriptionRequestDetails.getTranscriptionTypeId(),
                transcriptionRequestDetails.getStartDateTime(),
                transcriptionRequestDetails.getEndDateTime()
            );
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }
    }

    private boolean transcriptionTypesThatRequireDates(Integer transcriptionTypeId) {
        return SPECIFIED_TIMES.getId().equals(transcriptionTypeId)
            || COURT_LOG.getId().equals(transcriptionTypeId);
    }

    private boolean transcriptionDatesAreSet(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        return !isNull(startDateTime) || !isNull(endDateTime);
    }
}
