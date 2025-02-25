package uk.gov.hmcts.darts.audio.service;

import org.assertj.core.data.TemporalUnitOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaRequestTestData;

class MediaRequestServiceTest extends IntegrationBase {

    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @Autowired
    private MediaRequestService mediaRequestService;

    @Autowired
    private MediaRequestRepository requestRepository;

    @MockitoBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private SystemUserHelper systemUserHelper;

    private HearingEntity hearing;

    @BeforeEach
    void setUp() {
        hearing = dartsPersistence.save(PersistableFactory.getHearingTestData().someMinimalHearing());
    }

    @Test
    void shouldSaveAudioRequestWithZuluTimeOk() {
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(request.getId());
        assertTrue(mediaRequestEntity.getId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearing().getId());
        assertEquals(requestDetails.getStartTime(), mediaRequestEntity.getStartTime());
        assertEquals(requestDetails.getEndTime(), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getCreatedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedDateTime());
    }

    @Test
    void shouldSaveAudioRequestWithOffsetTimeOk() {
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse("2023-05-31T10:00:00+01:00"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-05-31T13:00:00+01:00"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(request.getId());
        assertTrue(mediaRequestEntity.getId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearing().getId());
        assertEquals(OffsetDateTime.parse(T_09_00_00_Z), mediaRequestEntity.getStartTime());
        assertEquals(OffsetDateTime.parse(T_12_00_00_Z), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getCreatedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedDateTime());
    }

    @Test
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeStarts() {
        // In the UK the clocks go forward 1 hour at 1am on the last Sunday in March.
        // The period when the clocks are 1 hour ahead is called British Summer Time (BST).
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-25T23:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T01:30:00Z"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(request.getId());
        assertTrue(mediaRequestEntity.getId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearing().getId());
        assertEquals(requestDetails.getStartTime(), mediaRequestEntity.getStartTime());
        assertEquals(requestDetails.getEndTime(), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getCreatedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedDateTime());
    }

    @Test
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeEnds() {
        // In the UK the clocks go back 1 hour at 2am on the last Sunday in October.
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse("2023-10-29T00:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-10-29T02:15:00Z"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(request.getId());
        assertTrue(mediaRequestEntity.getId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearing().getId());
        assertEquals(requestDetails.getStartTime(), mediaRequestEntity.getStartTime());
        assertEquals(requestDetails.getEndTime(), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getCreatedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedBy());
        assertNotNull(mediaRequestEntity.getLastModifiedDateTime());
    }

    @Test
    void shouldUpdateStatusToProcessing() {
        MediaRequestEntity mediaRequest = getMediaRequestTestData().someMinimal();
        dartsPersistence.save(mediaRequest);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.updateAudioRequestStatus(mediaRequest.getId(), PROCESSING);

        assertEquals(PROCESSING, mediaRequestEntity.getStatus());
    }

    @Test
    void shouldGetMediaRequestsByStatus() {
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        mediaRequestService.saveAudioRequest(requestDetails);

        Optional<MediaRequestEntity> mediaRequest = mediaRequestService.getOldestMediaRequestByStatus(OPEN);

        assertTrue(mediaRequest.isPresent());
        assertEquals(OPEN, mediaRequest.get().getStatus());
    }

    @Test
    void shouldThrowExceptionWhenGetMediaRequestByIdInvalid() {
        assertThrows(DartsApiException.class, () -> mediaRequestService.getMediaRequestEntityById(-3));
    }

    @Test
    void shouldDeleteAudioRequestById() {
        MediaRequestEntity mediaRequest = getMediaRequestTestData().someMinimal();
        dartsPersistence.save(mediaRequest);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(mediaRequest.getId());
        assertNotNull(mediaRequestEntity);

        mediaRequestService.deleteAudioRequest(mediaRequest.getId());
        assertThrows(DartsApiException.class, () -> mediaRequestService.getMediaRequestEntityById(mediaRequest.getId()));
    }

    @Test
    void updateAudioRequestCompleted() {
        MediaRequestEntity mediaRequest = getMediaRequestTestData().someMinimal();
        dartsPersistence.save(mediaRequest);
        final OffsetDateTime originalLastModifiedDateTime = mediaRequest.getLastModifiedDateTime();

        MediaRequestEntity updatedMediaRequest = mediaRequestService.updateAudioRequestCompleted(mediaRequest);

        // assert the date and user is set
        assertEquals(COMPLETED, updatedMediaRequest.getStatus());
        assertNotEquals(originalLastModifiedDateTime.atZoneSameInstant(ZoneOffset.UTC),
                        updatedMediaRequest.getLastModifiedDateTime().atZoneSameInstant(ZoneOffset.UTC));
        assertEquals(systemUserHelper.getSystemUser().getId(), updatedMediaRequest.getLastModifiedBy().getId());
    }

    @Test
    void shouldReturnTrueWhenADuplicateAudioRequestIsFound() {
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-25T23:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T01:30:00Z"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);
        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(request.getId());
        assertTrue(mediaRequestEntity.getId() > 0);

        var isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails);
        assertTrue(isDuplicateRequest);
    }

    @Test
    void shouldReturnFalseWhenNoDuplicateAudioRequestExists() {
        AudioRequestDetails requestDetails = createRequestDetails(hearing.getId());
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-26T12:00:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T12:30:00Z"));

        var isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails);
        assertFalse(isDuplicateRequest);
    }

    @Test
    void getsMediaRequestById() {
        MediaRequestEntity persistedMediaRequest = dartsPersistence.save(getMediaRequestTestData().someMinimal());

        var mediaRequestResponse = mediaRequestService.getMediaRequestById(persistedMediaRequest.getId());

        final TemporalUnitOffset within = within(1, ChronoUnit.SECONDS); // Small allowance to account for H2 precision differences
        assertThat(mediaRequestResponse.getId()).isEqualTo(persistedMediaRequest.getId());
        assertThat(mediaRequestResponse.getStartAt()).isCloseTo(persistedMediaRequest.getStartTime(), within);
        assertThat(mediaRequestResponse.getEndAt()).isCloseTo(persistedMediaRequest.getEndTime(), within);
        assertThat(mediaRequestResponse.getRequestedAt()).isCloseTo(persistedMediaRequest.getCreatedDateTime(), within);
        assertThat(mediaRequestResponse.getOwnerId()).isEqualTo(persistedMediaRequest.getCurrentOwner().getId());
        assertThat(mediaRequestResponse.getRequestedById()).isEqualTo(persistedMediaRequest.getRequestor().getId());
        assertThat(mediaRequestResponse.getCourtroom().getId()).isEqualTo(persistedMediaRequest.getHearing().getCourtroom().getId());
        assertThat(mediaRequestResponse.getCourtroom().getName()).isEqualTo(persistedMediaRequest.getHearing().getCourtroom().getName());
        assertThat(mediaRequestResponse.getHearing().getId()).isEqualTo(persistedMediaRequest.getHearing().getId());
        assertThat(mediaRequestResponse.getHearing().getHearingDate()).isEqualTo(persistedMediaRequest.getHearing().getHearingDate());
    }

    private AudioRequestDetails createRequestDetails(int hearingId) {
        AudioRequestDetails requestDetails = new AudioRequestDetails(null, null, null, null, null);
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(0);
        requestDetails.setRequestType(DOWNLOAD);

        return requestDetails;
    }

}