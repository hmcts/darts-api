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
import java.time.temporal.ChronoUnit;
import javax.validation.constraints.NotNull;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.COURT_LOG;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.AUDIO_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TIMES_OUTSIDE_OF_HEARING_TIMES;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionRequestDetailsValidator implements Validator<TranscriptionRequestDetails> {

    private final CaseService caseService;
    private final HearingsService hearingsService;

    /**
     *  TODO: This method has been lifted-and-shifted as-is from TranscriptionController and would benefit from refactoring to reduce complexity (ref DMP-2083).
     */
    @Override
    public void validate(@NotNull TranscriptionRequestDetails transcriptionRequestDetails) {
        if (isNull(transcriptionRequestDetails.getHearingId()) && isNull(transcriptionRequestDetails.getCaseId())) {
            throw new DartsApiException(TranscriptionApiError.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST);
        } else if (nonNull(transcriptionRequestDetails.getHearingId())) {
            HearingEntity hearing = hearingsService.getHearingById(transcriptionRequestDetails.getHearingId());
            if (hearing.getMediaList() == null || hearing.getMediaList().isEmpty()) {
                log.error(
                    "Transcription could not be requested. No audio found for hearing id {}",
                    transcriptionRequestDetails.getHearingId()
                );
                throw new DartsApiException(AUDIO_NOT_FOUND);
            } else {
                //check times
                OffsetDateTime requestStartDateTime = transcriptionRequestDetails.getStartDateTime();
                OffsetDateTime requestEndDateTime = transcriptionRequestDetails.getEndDateTime();
                if (requestStartDateTime != null && requestEndDateTime != null) {
                    boolean validTimes = hearing.getMediaList().stream().anyMatch(
                        m -> checkStartTime(m.getStart().truncatedTo(ChronoUnit.SECONDS),
                                            requestStartDateTime.truncatedTo(ChronoUnit.SECONDS), requestEndDateTime.truncatedTo(ChronoUnit.SECONDS))
                            && checkEndTime(m.getEnd().truncatedTo(ChronoUnit.SECONDS),
                                            requestStartDateTime.truncatedTo(ChronoUnit.SECONDS), requestEndDateTime.truncatedTo(ChronoUnit.SECONDS)));
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

        Integer transcriptionTypeId = transcriptionRequestDetails.getTranscriptionTypeId();
        TranscriptionTypeEnum.fromId(transcriptionTypeId);
        TranscriptionUrgencyEnum.fromId(transcriptionRequestDetails.getTranscriptionUrgencyId());

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

    private boolean checkEndTime(OffsetDateTime mediaEndDateTime, OffsetDateTime requestStartDateTime, OffsetDateTime requestEndDateTime) {
        return (mediaEndDateTime.isEqual(requestEndDateTime) || mediaEndDateTime.isAfter(requestEndDateTime));
    }

    private boolean checkStartTime(OffsetDateTime mediaStartDateTime, OffsetDateTime requestStartDateTime, OffsetDateTime requestEndDateTime) {
        return (mediaStartDateTime.isEqual(requestStartDateTime) || mediaStartDateTime.isBefore(requestStartDateTime));
    }

    private boolean transcriptionTypesThatRequireDates(Integer transcriptionTypeId) {
        return SPECIFIED_TIMES.getId().equals(transcriptionTypeId)
            || COURT_LOG.getId().equals(transcriptionTypeId);
    }

    private boolean transcriptionDatesAreSet(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        return nonNull(startDateTime) && nonNull(endDateTime);
    }

}
