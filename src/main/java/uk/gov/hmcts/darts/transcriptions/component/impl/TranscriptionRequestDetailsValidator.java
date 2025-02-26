package uk.gov.hmcts.darts.transcriptions.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.COURT_LOG;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.AUDIO_NOT_FOUND;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionRequestDetailsValidator implements Validator<TranscriptionRequestDetails> {

    private final CaseService caseService;
    private final HearingsService hearingsService;

    @Override
    public void validate(@NotNull TranscriptionRequestDetails transcriptionRequestDetails) {
        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        } else if (nonNull(transcriptionRequestDetails.getHearingId())) {
            checkHearingHasValidMedia(transcriptionRequestDetails);
        } else {
            checkIdExistsOrThrowCaseNotFoundException(transcriptionRequestDetails);
        }
        checkStartAndEndDatesValid(transcriptionRequestDetails);
    }

    private void checkIdExistsOrThrowCaseNotFoundException(TranscriptionRequestDetails transcriptionRequestDetails) {
        caseService.getCourtCaseById(transcriptionRequestDetails.getCaseId());
    }

    private void checkHearingHasValidMedia(TranscriptionRequestDetails transcriptionRequestDetails) {
        final HearingEntity hearing = hearingsService.getHearingByIdWithValidation(transcriptionRequestDetails.getHearingId());
        checkAudioFileExistsAndTimesValid(transcriptionRequestDetails, hearing);
    }

    private void checkAudioFileExistsAndTimesValid(TranscriptionRequestDetails transcriptionRequestDetails, HearingEntity hearing) {
        if (isEmpty(hearing.getMediaList())) {
            log.error(
                "Transcription could not be requested. No audio found for hearing id {}",
                transcriptionRequestDetails.getHearingId()
            );
            throw new DartsApiException(AUDIO_NOT_FOUND);
        }
    }

    private void checkStartAndEndDatesValid(TranscriptionRequestDetails transcriptionRequestDetails) {
        final Integer transcriptionTypeId = transcriptionRequestDetails.getTranscriptionTypeId();

        checkEnumsIdValid(transcriptionRequestDetails, transcriptionTypeId);

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

    private void checkEnumsIdValid(TranscriptionRequestDetails transcriptionRequestDetails, Integer transcriptionTypeId) {
        TranscriptionTypeEnum.fromId(transcriptionTypeId);
        TranscriptionUrgencyEnum.fromId(transcriptionRequestDetails.getTranscriptionUrgencyId());
    }

    private boolean transcriptionTypesThatRequireDates(Integer transcriptionTypeId) {
        return SPECIFIED_TIMES.getId().equals(transcriptionTypeId)
            || COURT_LOG.getId().equals(transcriptionTypeId);
    }

    private boolean transcriptionDatesAreSet(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        return nonNull(startDateTime) && nonNull(endDateTime);
    }

}
