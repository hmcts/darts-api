package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateRequestDetectorTest {

    private static final OffsetDateTime START_TIME = OffsetDateTime.now().minusDays(2);
    private static final OffsetDateTime END_TIME = OffsetDateTime.now().minusDays(1);

    private final TranscriptionRepository transcriptionRepository = mock(TranscriptionRepository.class);
    private final TranscriptionTypeRepository transcriptionTypeRepository = mock(TranscriptionTypeRepository.class);
    private TranscriptionTypeEntity transcriptionType;

    private final DuplicateRequestDetector duplicateRequestDetector = new DuplicateRequestDetector(transcriptionRepository, transcriptionTypeRepository);

    @BeforeEach
    void setup() {
        transcriptionType = someTranscriptionType();

        when(transcriptionTypeRepository.getReferenceById(transcriptionType.getId()))
            .thenReturn(transcriptionType);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void doesNotThrowWhenNoMatchingTranscriptions(boolean isManual) {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(true), anyList()))
            .thenReturn(emptyList());

        assertThatNoException().isThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, isManual));
    }

    @Test
    void throwsWhenMatchingManuallyRequestedTranscriptionFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(true), anyList()))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, TranscriptionStatusEnum.COMPLETE, 1));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, true))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(1);
    }

    @Test
    void throwsWhenMatchingManuallyRequestedTranscriptionWithoutTimesFound() {
        var requestDetails = someTranscriptionRequestDetailsWithoutTimes();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(null), eq(null), eq(true), anyList()))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, TranscriptionStatusEnum.COMPLETE, 1));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, true))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(1);
    }

    @Test
    void throwsWhenMultipleMatchingManuallyRequestedTranscriptionFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(true), anyList()))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, TranscriptionStatusEnum.COMPLETE, 3));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, true))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(1);
    }

    @Test
    void throwsWhenMatchingAutomaticallyRequestedTranscriptionsFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(false), anyList()))
            .thenReturn(someTranscriptionRequestedAutomaticallyThatMatches(requestDetails, TranscriptionStatusEnum.COMPLETE, 1));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, false))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(1);
    }

    @Test
    void throwsWhenMultipleMatchingAutomaticallyRequestedTranscriptionsFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(false), anyList()))
            .thenReturn(someTranscriptionRequestedAutomaticallyThatMatches(requestDetails, TranscriptionStatusEnum.COMPLETE, 3));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, false))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(1);
    }

    @Test
    void throwsWhenMatchingNotCompletedRequestedTranscriptionFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(false), anyList()))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, TranscriptionStatusEnum.APPROVED, 1));

        DartsApiException throwable = catchThrowableOfType(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, false), DartsApiException.class);
        assertEquals(throwable.getError(), TranscriptionApiError.DUPLICATE_TRANSCRIPTION);
        assertEquals(0, throwable.getCustomProperties().size());
    }

    @Test
    void throwsWhenMatchingAutomatedNotCompletedRequestedTranscriptionFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
            eq(requestDetails.getHearingId()), eq(transcriptionType), eq(START_TIME), eq(END_TIME), eq(true), anyList()))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, TranscriptionStatusEnum.WITH_TRANSCRIBER, 1));

        DartsApiException throwable = catchThrowableOfType(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, true), DartsApiException.class);
        assertEquals(throwable.getError(), TranscriptionApiError.DUPLICATE_TRANSCRIPTION);
        assertEquals(0, throwable.getCustomProperties().size());
    }

    private List<TranscriptionEntity> someTranscriptionRequestedManuallyThatMatches(
        TranscriptionRequestDetails requestDetails, TranscriptionStatusEnum status, int count) {
        return rangeClosed(1, count)
            .mapToObj(index -> {
                var transcription = someTranscriptionThatMatches(requestDetails, index);
                transcription.setIsManualTranscription(true);
                var transcriptionStatusEntity = new TranscriptionStatusEntity();
                transcriptionStatusEntity.setId(status.getId());
                transcription.setTranscriptionStatus(transcriptionStatusEntity);
                return transcription;
            }).toList();
    }

    private List<TranscriptionEntity> someTranscriptionRequestedAutomaticallyThatMatches(
        TranscriptionRequestDetails requestDetails, TranscriptionStatusEnum status, int count) {
        return rangeClosed(1, count)
            .mapToObj(index -> {
                var transcription = someTranscriptionThatMatches(requestDetails, index);
                var transcriptionStatusEntity = new TranscriptionStatusEntity();
                transcriptionStatusEntity.setId(status.getId());
                transcription.setTranscriptionStatus(transcriptionStatusEntity);
                transcription.setIsManualTranscription(false);
                return transcription;
            }).toList();
    }

    private static TranscriptionRequestDetails someTranscriptionRequestDetails() {
        var requestDetails = new TranscriptionRequestDetails();
        requestDetails.setHearingId(1);
        requestDetails.setTranscriptionTypeId(5);
        requestDetails.setStartDateTime(START_TIME);
        requestDetails.setEndDateTime(END_TIME);
        return requestDetails;
    }

    private static TranscriptionRequestDetails someTranscriptionRequestDetailsWithoutTimes() {
        var requestDetails = new TranscriptionRequestDetails();
        requestDetails.setHearingId(1);
        requestDetails.setTranscriptionTypeId(5);
        return requestDetails;
    }

    private TranscriptionEntity someTranscriptionThatMatches(TranscriptionRequestDetails requestDetails, int id) {
        var transcription = new TranscriptionEntity();
        transcription.setId(id);
        transcription.setStartTime(requestDetails.getStartDateTime());
        transcription.setEndTime(requestDetails.getEndDateTime());
        transcription.addHearing(hearingWithId(requestDetails.getHearingId()));
        return transcription;
    }

    private HearingEntity hearingWithId(Integer hearingId) {
        var hearingEntity = new HearingEntity();
        hearingEntity.setId(hearingId);
        return hearingEntity;
    }


    private TranscriptionTypeEntity someTranscriptionType() {
        var transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(5);
        return transcriptionType;
    }

}
