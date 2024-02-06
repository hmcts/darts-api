package uk.gov.hmcts.darts.transcriptions.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TranscriptionRequestDetailsValidatorTest {

    private static final int DUMMY_HEARING_ID = 1;
    private static final int DUMMY_CASE_ID = 1;
    private static final Class<DartsApiException> DARTS_EXCEPTION = DartsApiException.class;
    private static final OffsetDateTime MEDIA_1_START_TIME = OffsetDateTime.parse("2024-01-15T14:00:00Z");
    private static final OffsetDateTime MEDIA_1_END_TIME = OffsetDateTime.parse("2024-01-15T14:30:00Z");
    private static final OffsetDateTime MEDIA_2_START_TIME = MEDIA_1_END_TIME;
    private static final OffsetDateTime MEDIA_2_END_TIME = OffsetDateTime.parse("2024-01-15T15:00:00Z");

    @Mock
    private CaseService caseServiceMock;

    @Mock
    private HearingsService hearingsServiceMock;

    private TranscriptionRequestDetailsValidator validator;

    private static Stream<Arguments> requestsWithInvalidTimings() {
        return Stream.of(
              // Boundary cases around media 1 timespan
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME.minusSeconds(1), MEDIA_1_END_TIME)),
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME, MEDIA_1_END_TIME.plusSeconds(1))),

              // Boundary cases around media 2 timespan
              Arguments.of(createRequestDetails(MEDIA_2_START_TIME.minusSeconds(1), MEDIA_2_END_TIME)),
              Arguments.of(createRequestDetails(MEDIA_2_START_TIME, MEDIA_2_END_TIME.plusSeconds(1))),

              // Boundary cases around overall timespan
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME, MEDIA_2_END_TIME)),
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME.minusSeconds(1), MEDIA_2_END_TIME)),
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME, MEDIA_2_END_TIME.plusSeconds(1)))
        );
    }

    private static Stream<Arguments> requestsThatRequireDatesButNoDatesArePresent() {
        return Stream.of(
              Arguments.of(createRequestDetails(TranscriptionTypeEnum.SPECIFIED_TIMES)),
              Arguments.of(createRequestDetails(TranscriptionTypeEnum.COURT_LOG))
        );
    }

    private static Stream<Arguments> requestsThatRequireDatesAndDatesArePresent() {
        return Stream.of(
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME, MEDIA_1_END_TIME, TranscriptionTypeEnum.SPECIFIED_TIMES)),
              Arguments.of(createRequestDetails(MEDIA_1_START_TIME, MEDIA_1_END_TIME, TranscriptionTypeEnum.COURT_LOG))
        );
    }

    private static TranscriptionRequestDetails createRequestDetails(OffsetDateTime startTime,
          OffsetDateTime endTime) {
        var requestDetails = new TranscriptionRequestDetails();
        requestDetails.setStartDateTime(startTime);
        requestDetails.setEndDateTime(endTime);

        requestDetails.setHearingId(DUMMY_HEARING_ID);

        return requestDetails;
    }

    private static TranscriptionRequestDetails createRequestDetails(TranscriptionTypeEnum transcriptionTypeEnum) {
        var requestDetails = new TranscriptionRequestDetails();
        requestDetails.setTranscriptionTypeId(transcriptionTypeEnum.getId());

        requestDetails.setCaseId(DUMMY_CASE_ID);
        requestDetails.setTranscriptionUrgencyId(TranscriptionUrgencyEnum.STANDARD.getId());

        return requestDetails;
    }

    private static TranscriptionRequestDetails createRequestDetails(OffsetDateTime startTime,
          OffsetDateTime endTime,
          TranscriptionTypeEnum transcriptionTypeEnum) {
        var requestDetails = new TranscriptionRequestDetails();
        requestDetails.setStartDateTime(startTime);
        requestDetails.setEndDateTime(endTime);
        requestDetails.setTranscriptionTypeId(transcriptionTypeEnum.getId());
        requestDetails.setCaseId(DUMMY_CASE_ID);

        requestDetails.setTranscriptionUrgencyId(TranscriptionUrgencyEnum.STANDARD.getId());

        return requestDetails;
    }

    @BeforeEach
    void setUp() {
        validator = new TranscriptionRequestDetailsValidator(caseServiceMock, hearingsServiceMock);
    }

    @Test
    void validateShouldThrowExceptionWhenHearingIdAndCaseIdIsNull() {
        // Given
        var validatable = new TranscriptionRequestDetails();
        validatable.setHearingId(null);
        validatable.setCaseId(null);

        // When
        var actualException = assertThrows(DARTS_EXCEPTION, () -> validator.validate(validatable));

        // Then
        assertEquals(URI.create("TRANSCRIPTION_100"), actualException.getError().getType());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void validateShouldThrowExceptionWhenProvidedHearingHasNoAudio(List<MediaEntity> mediaEntityList) {
        // Given
        var hearingEntity = new HearingEntity();
        hearingEntity.setMediaList(mediaEntityList);

        Mockito.when(hearingsServiceMock.getHearingById(DUMMY_HEARING_ID))
              .thenReturn(hearingEntity);

        var validatable = new TranscriptionRequestDetails();
        validatable.hearingId(DUMMY_HEARING_ID);

        // When
        var actualException = assertThrows(DARTS_EXCEPTION, () -> validator.validate(validatable));

        // Then
        assertEquals(URI.create("TRANSCRIPTION_110"), actualException.getError().getType());
    }

    @Test
    void validateShouldThrowExceptionWhenProvidedCaseDoesNotExist() {
        // Given
        var bubbledException = new DartsApiException(CaseApiError.CASE_NOT_FOUND);
        Mockito.when(caseServiceMock.getCourtCaseById(DUMMY_CASE_ID))
              .thenThrow(bubbledException);

        var validatable = new TranscriptionRequestDetails();
        validatable.setCaseId(DUMMY_CASE_ID);

        // When
        var actualException = assertThrows(DARTS_EXCEPTION, () -> validator.validate(validatable));

        // Then
        assertEquals(URI.create("CASE_104"), actualException.getError().getType());
    }

    @ParameterizedTest
    @MethodSource("requestsWithInvalidTimings")
    void validateShouldThrowExceptionWhenTranscriptionRequestTimesAreInvalid(TranscriptionRequestDetails requestDetails) {
        // Given
        var hearingEntity = new HearingEntity();
        hearingEntity.setMediaList(createMediaList());
        Mockito.when(hearingsServiceMock.getHearingById(DUMMY_HEARING_ID))
              .thenReturn(hearingEntity);

        // When
        var actualException = assertThrows(DARTS_EXCEPTION, () -> validator.validate(requestDetails));

        // Then
        assertEquals(URI.create("TRANSCRIPTION_111"), actualException.getError().getType());
    }

    @ParameterizedTest
    @MethodSource("requestsThatRequireDatesButNoDatesArePresent")
    void validateShouldThrowExceptionWhenTranscriptTypeRequiresDatesAndDatesAreNotPresent(TranscriptionRequestDetails requestDetails) {
        var actualException = assertThrows(DARTS_EXCEPTION, () -> validator.validate(requestDetails));

        assertEquals(URI.create("TRANSCRIPTION_100"), actualException.getError().getType());
    }

    @ParameterizedTest
    @MethodSource("requestsThatRequireDatesAndDatesArePresent")
    void validateShouldSucceedWhenTranscriptTypeRequiresDatesAndDatesArePresent(TranscriptionRequestDetails requestDetails) {
        assertDoesNotThrow(() -> validator.validate(requestDetails));
    }

    @Test
    void validateShouldSucceedWhenRequestHasHearingIdAndValidDates() {
        // Given
        var hearingEntity = new HearingEntity();
        hearingEntity.setMediaList(createMediaList());
        Mockito.when(hearingsServiceMock.getHearingById(DUMMY_HEARING_ID))
              .thenReturn(hearingEntity);

        var requestDetails = createRequestDetails(MEDIA_1_START_TIME, MEDIA_1_END_TIME);
        requestDetails.setTranscriptionTypeId(TranscriptionTypeEnum.SPECIFIED_TIMES.getId());
        requestDetails.setTranscriptionUrgencyId(TranscriptionUrgencyEnum.STANDARD.getId());

        // Then
        assertDoesNotThrow(() -> validator.validate(requestDetails));
    }

    private List<MediaEntity> createMediaList() {
        return Arrays.asList(
              createMediaEntity(MEDIA_1_START_TIME, MEDIA_1_END_TIME),
              createMediaEntity(MEDIA_2_START_TIME, MEDIA_2_END_TIME)
        );
    }

    private MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime) {
        var mediaEntity = new MediaEntity();
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);

        return mediaEntity;
    }

}
