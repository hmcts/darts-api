package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateRequestDetectorTest {

    private static final OffsetDateTime START_TIME = OffsetDateTime.now().minusDays(2);
    private static final OffsetDateTime END_TIME = OffsetDateTime.now().minusDays(1);

    private final TranscriptionRepository transcriptionRepository = mock(TranscriptionRepository.class);
    private final TranscriptionTypeRepository transcriptionTypeRepository = mock(TranscriptionTypeRepository.class);
    private TranscriptionTypeEntity transcriptionType;
    private final Random random = new Random();

    private final DuplicateRequestDetector duplicateRequestDetector = new DuplicateRequestDetector(transcriptionRepository, transcriptionTypeRepository);

    @BeforeEach
    void setup() {
        transcriptionType = someTranscriptionType();

        when(transcriptionTypeRepository.getReferenceById(transcriptionType.getId()))
            .thenReturn(transcriptionType);
    }

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void doesntThrowWhenNoMatchingTranscriptions(boolean isManual) {
        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManual(1, transcriptionType, START_TIME, END_TIME, isManual))
            .thenReturn(emptyList());

        var requestDetails = someTranscriptionRequestDetails();

        assertThatNoException().isThrownBy(
            () -> duplicateRequestDetector.checkForDuplicate(requestDetails, isManual));
    }

    @Test
    void throwsWhenMatchingManuallyRequestedTranscriptionFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManual(1, transcriptionType, START_TIME, END_TIME, true))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, 1));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, true))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(List.of(1));
    }

    @Test
    void throwsWhenMatchingAutomaticallyRequestedTranscriptionFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManual(1, transcriptionType, START_TIME, END_TIME, false))
            .thenReturn(someTranscriptionRequestedAutomaticallyThatMatches(requestDetails, 1));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, false))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(List.of(1));
    }

    @Test
    void throwsWhenMultipleMatchingAutomaticallyRequestedTranscriptionsFound() {
        var requestDetails = someTranscriptionRequestDetails();

        when(transcriptionRepository.findByHearingIdTypeStartAndEndAndIsManual(1, transcriptionType, START_TIME, END_TIME, false))
            .thenReturn(someTranscriptionRequestedManuallyThatMatches(requestDetails, 3));

        assertThatThrownBy(() -> duplicateRequestDetector.checkForDuplicate(requestDetails, false))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", TranscriptionApiError.DUPLICATE_TRANSCRIPTION)
            .extracting("customProperties.duplicate_transcription_id").isEqualTo(List.of(1, 2, 3));
    }

    private List<TranscriptionEntity> someTranscriptionRequestedManuallyThatMatches(TranscriptionRequestDetails requestDetails, int count) {
        return rangeClosed(1, count)
            .mapToObj(index -> {
                var transcription = someTranscriptionThatMatches(requestDetails, index);
                transcription.setIsManualTranscription(true);
                return transcription;
            }).collect(toList());
    }

    private List<TranscriptionEntity> someTranscriptionRequestedAutomaticallyThatMatches(TranscriptionRequestDetails requestDetails, int count) {
        return rangeClosed(1, count)
            .mapToObj(index -> {
                var transcription = someTranscriptionThatMatches(requestDetails, index);
                transcription.setIsManualTranscription(false);
                return transcription;
            }).collect(toList());
    }

    private static TranscriptionRequestDetails someTranscriptionRequestDetails() {
        var requestDetails = new TranscriptionRequestDetails();
        requestDetails.setHearingId(1);
        requestDetails.setTranscriptionTypeId(1);
        requestDetails.setStartDateTime(START_TIME);
        requestDetails.setEndDateTime(END_TIME);
        return requestDetails;
    }

    private TranscriptionEntity someTranscriptionThatMatches(TranscriptionRequestDetails requestDetails, int id) {
        var transcription = new TranscriptionEntity();
        transcription.setId(id);
        transcription.setStartTime(requestDetails.getStartDateTime());
        transcription.setEndTime(requestDetails.getEndDateTime());
        transcription.setHearing(hearingWithId(requestDetails.getHearingId()));
        return transcription;
    }

    private HearingEntity hearingWithId(Integer hearingId) {
        var hearingEntity = new HearingEntity();
        hearingEntity.setId(hearingId);
        return hearingEntity;
    }


    private TranscriptionTypeEntity someTranscriptionType() {
        var transcriptionType = new TranscriptionTypeEntity();
        transcriptionType.setId(1);
        return transcriptionType;
    }

}
