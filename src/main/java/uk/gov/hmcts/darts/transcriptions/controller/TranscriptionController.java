package uk.gov.hmcts.darts.transcriptions.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.http.api.TranscriptionApi;
import uk.gov.hmcts.darts.transcriptions.model.AttachTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetYourTranscriptsResponse;
import uk.gov.hmcts.darts.transcriptions.model.RequestTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionResponse;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.TRANSCRIPTION_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.COURT_LOG;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.AUDIO_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TIMES_OUTSIDE_OF_HEARING_TIMES;


@RestController
@RequiredArgsConstructor
@Slf4j
public class TranscriptionController implements TranscriptionApi {

    private final TranscriptionService transcriptionService;
    private final CaseService caseService;
    private final HearingsService hearingsService;


    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(bodyAuthorisation = true, contextId = ANY_ENTITY_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<RequestTranscriptionResponse> requestTranscription(
        TranscriptionRequestDetails transcriptionRequestDetails) {
        validateTranscriptionRequestValues(transcriptionRequestDetails);
        try {
            return new ResponseEntity<>(
                transcriptionService.saveTranscriptionRequest(transcriptionRequestDetails, true),
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

        return ResponseEntity.ok(transcriptionService.updateTranscription(transcriptionId, updateTranscription));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID, securityRoles = {TRANSCRIBER})
    public ResponseEntity<AttachTranscriptResponse> attachTranscript(Integer transcriptionId,
                                                                     MultipartFile transcript) {
        return ResponseEntity.ok(transcriptionService.attachTranscript(transcriptionId, transcript));
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        securityRoles = {JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<Resource> downloadTranscript(Integer transcriptionId) {
        final DownloadTranscriptResponse downloadTranscriptResponse = transcriptionService.downloadTranscript(
            transcriptionId);
        return ResponseEntity.ok()
            .header(
                CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"", downloadTranscriptResponse.getFileName())
            )
            .header(
                "external_location",
                downloadTranscriptResponse.getExternalLocation().toString()
            )
            .header(
                "transcription_document_id",
                String.valueOf(downloadTranscriptResponse.getTranscriptionDocumentId())
            )
            .contentType(MediaType.parseMediaType(downloadTranscriptResponse.getContentType()))
            .body(downloadTranscriptResponse.getResource());
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<TranscriptionTypeResponse>> getTranscriptionTypes() {
        return new ResponseEntity<>(
            transcriptionService.getTranscriptionTypes(),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<List<TranscriptionUrgencyResponse>> getTranscriptionUrgencies() {
        return new ResponseEntity<>(
            transcriptionService.getTranscriptionUrgenciesByDisplayState(),
            HttpStatus.OK
        );
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    public ResponseEntity<GetYourTranscriptsResponse> getYourTranscripts(Integer userId) {
        return ResponseEntity.ok(transcriptionService.getYourTranscripts(userId));
    }

    private void validateTranscriptionRequestValues(TranscriptionRequestDetails transcriptionRequestDetails) {
        log.info("Starting validateTranscriptionRequestValues");
        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        } else if (nonNull(transcriptionRequestDetails.getHearingId())) {
            log.info("Found hearing");
            HearingEntity hearing = hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
            if (hearing.getMediaList() == null || hearing.getMediaList().isEmpty()) {
                log.error("Transcription could not be requested. No audio found for hearing id {}",
                          transcriptionRequestDetails.getHearingId());
                throw new DartsApiException(AUDIO_NOT_FOUND);
            } else {
                //check times
                log.info("Checking times");
                OffsetDateTime requestStartDateTime = transcriptionRequestDetails.getStartDateTime();
                OffsetDateTime requestEndDateTime = transcriptionRequestDetails.getEndDateTime();
                log.info("requestStartDateTime" + requestStartDateTime);
                log.info("requestEndDateTime" + requestEndDateTime);
                log.info("hearing.getMediaList().get(0).getStart()" + hearing.getMediaList().get(0).getStart());
                log.info("hearing.getMediaList().get(0).getEnd()" + hearing.getMediaList().get(0).getEnd());
                if (requestStartDateTime != null && requestEndDateTime != null) {
                    boolean validTimes = hearing.getMediaList().stream().anyMatch(m -> m.getStart().isBefore(
                        requestStartDateTime) && m.getStart().isBefore(requestEndDateTime)
                        && m.getEnd().isAfter(requestStartDateTime) && m.getEnd().isAfter(requestEndDateTime));
                    log.info("checked times validTimes " + validTimes);
                    if (!validTimes) {
                        log.error(
                            "Transcription could not be requested. Times were outside of hearing times for hearing id {}",
                            transcriptionRequestDetails.getHearingId()
                        );
                        throw new DartsApiException(TIMES_OUTSIDE_OF_HEARING_TIMES);
                    }
                }
            }
        } else {
            caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId());
        }
        log.info("Completed initial checks for transcriptionRequestDetails.getHearingId() " + transcriptionRequestDetails.getHearingId());
        Integer transcriptionTypeId = transcriptionRequestDetails.getTranscriptionTypeId();
        log.info("transcriptionTypeId" + transcriptionTypeId);
        TranscriptionTypeEnum.fromId(transcriptionTypeId);
        TranscriptionUrgencyEnum.fromId(transcriptionRequestDetails.getUrgencyId());
        log.info("got enums");

        if (transcriptionTypesThatRequireDates(transcriptionTypeId)
            && !transcriptionDatesAreSet(
            transcriptionRequestDetails.getStartDateTime(),
            transcriptionRequestDetails.getEndDateTime()
        )) {
            log.info("error");
            log.error(
                "This transcription type {} requires both the start date ({}) and end dates ({})",
                transcriptionRequestDetails.getTranscriptionTypeId(),
                transcriptionRequestDetails.getStartDateTime(),
                transcriptionRequestDetails.getEndDateTime()
            );
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        }
        log.info("got to end");
    }

    private boolean transcriptionTypesThatRequireDates(Integer transcriptionTypeId) {
        log.info("checking types that need dates");
        return SPECIFIED_TIMES.getId().equals(transcriptionTypeId)
            || COURT_LOG.getId().equals(transcriptionTypeId);
    }

    private boolean transcriptionDatesAreSet(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        log.info("checking dates not null");
        return nonNull(startDateTime) && nonNull(endDateTime);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = TRANSCRIPTION_ID,
        securityRoles = {JUDGE, APPROVER, REQUESTER, TRANSCRIBER, RCJ_APPEALS},
        globalAccessSecurityRoles = {JUDGE})
    public ResponseEntity<TranscriptionResponse> getTranscription(
        @Parameter(name = "transcription_id", description = "transcription_id is the internal id of the transcription.", required = true,
            in = ParameterIn.PATH) @PathVariable("transcription_id") Integer transcriptionId
    ) {
        return new ResponseEntity<>(
            transcriptionService.getTranscription(transcriptionId),
            HttpStatus.OK
        );

    }
}
