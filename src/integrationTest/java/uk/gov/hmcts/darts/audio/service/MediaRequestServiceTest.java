package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.testutils.IntegrationPerClassBase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.test.common.data.MediaRequestTestData.minimalRequestData;

class MediaRequestServiceTest extends IntegrationPerClassBase {

    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @Autowired
    private MediaRequestService mediaRequestService;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    private AudioRequestDetails requestDetails;

    @BeforeAll
    void beforeAll() {
        HearingEntity hearing = dartsDatabase.hasSomeHearing();

        requestDetails = new AudioRequestDetails(null, null, null, null, null);
        requestDetails.setHearingId(hearing.getId());
        requestDetails.setRequestor(0);
        requestDetails.setRequestType(DOWNLOAD);
    }

    @Test

    void shouldSaveAudioRequestWithZuluTimeOk() {
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
        MediaRequestEntity mediaRequestEntity = mediaRequestService.updateAudioRequestStatus(1, PROCESSING);

        assertEquals(PROCESSING, mediaRequestEntity.getStatus());
    }

    @Test

    void shouldGetMediaRequestsByStatus() {
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
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(request.getId());
        assertNotNull(mediaRequestEntity);

        mediaRequestService.deleteAudioRequest(request.getId());
        assertThrows(DartsApiException.class, () -> mediaRequestService.getMediaRequestEntityById(request.getId()));
    }

    @Test

    void shouldSetDownloadFileNameAndFormat() {
        MediaRequestEntity mediaRequestEntity = mediaRequestService.updateAudioRequestCompleted(mediaRequestService.getMediaRequestEntityById(1));

        assertEquals(COMPLETED, mediaRequestEntity.getStatus());
    }

    @Test

    void shouldSetPlaybackFileNameAndFormat() {
        MediaRequestEntity mediaRequestEntity = mediaRequestService.updateAudioRequestCompleted(mediaRequestService.getMediaRequestEntityById(1));

        assertEquals(COMPLETED, mediaRequestEntity.getStatus());
    }

    @Test
    void shouldReturnTrueWhenADuplicateAudioRequestIsFound() {
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
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-26T12:00:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T12:30:00Z"));

        var isDuplicateRequest = mediaRequestService.isUserDuplicateAudioRequest(requestDetails);
        assertFalse(isDuplicateRequest);
    }

    @Test
    void getsMediaRequestById() {
        var persistedMediaRequest = dartsDatabase.saveWithTransientEntities(minimalRequestData());

        var mediaRequestResponse = mediaRequestService.getMediaRequestById(persistedMediaRequest.getId());

        assertThat(mediaRequestResponse.getId()).isEqualTo(persistedMediaRequest.getId());
        assertThat(mediaRequestResponse.getStartAt()).isEqualTo(persistedMediaRequest.getStartTime());
        assertThat(mediaRequestResponse.getEndAt()).isEqualTo(persistedMediaRequest.getEndTime());
        assertThat(mediaRequestResponse.getRequestedAt()).isEqualTo(persistedMediaRequest.getCreatedDateTime());
        assertThat(mediaRequestResponse.getOwnerId()).isEqualTo(persistedMediaRequest.getCurrentOwner().getId());
        assertThat(mediaRequestResponse.getRequestedById()).isEqualTo(persistedMediaRequest.getRequestor().getId());
        assertThat(mediaRequestResponse.getCourtroom().getId()).isEqualTo(persistedMediaRequest.getHearing().getCourtroom().getId());
        assertThat(mediaRequestResponse.getCourtroom().getName()).isEqualTo(persistedMediaRequest.getHearing().getCourtroom().getName());
        assertThat(mediaRequestResponse.getHearing().getId()).isEqualTo(persistedMediaRequest.getHearing().getId());
        assertThat(mediaRequestResponse.getHearing().getHearingDate()).isEqualTo(persistedMediaRequest.getHearing().getHearingDate());
    }
}
