package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationPerClassBase;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

class MediaRequestServiceTest extends IntegrationPerClassBase {

    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @Autowired
    private MediaRequestService mediaRequestService;

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
    @Order(1)
    void shouldSaveAudioRequestWithZuluTimeOk() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(request.getId());
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
    @Order(2)
    void shouldSaveAudioRequestWithOffsetTimeOk() {
        requestDetails.setStartTime(OffsetDateTime.parse("2023-05-31T10:00:00+01:00"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-05-31T13:00:00+01:00"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(request.getId());
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
    @Order(3)
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeStarts() {
        // In the UK the clocks go forward 1 hour at 1am on the last Sunday in March.
        // The period when the clocks are 1 hour ahead is called British Summer Time (BST).
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-25T23:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T01:30:00Z"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(request.getId());
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
    @Order(4)
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeEnds() {
        // In the UK the clocks go back 1 hour at 2am on the last Sunday in October.
        requestDetails.setStartTime(OffsetDateTime.parse("2023-10-29T00:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-10-29T02:15:00Z"));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(request.getId());
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
    @Order(5)
    void shouldUpdateStatusToProcessing() {
        MediaRequestEntity mediaRequestEntity = mediaRequestService.updateAudioRequestStatus(1, PROCESSING);

        assertEquals(PROCESSING, mediaRequestEntity.getStatus());
    }

    @Test
    @Order(5)
    void shouldGetMediaRequestsByStatus() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        mediaRequestService.saveAudioRequest(requestDetails);

        Optional<MediaRequestEntity> mediaRequest = mediaRequestService.getOldestMediaRequestByStatus(OPEN);

        assertTrue(mediaRequest.isPresent());
        assertEquals(OPEN, mediaRequest.get().getStatus());
    }

    @Test
    @Order(7)
    void shouldThrowExceptionWhenGetMediaRequestByIdInvalid() {
        assertThrows(NoSuchElementException.class, () -> mediaRequestService.getMediaRequestById(-3));
    }

    @Test
    @Order(8)
    void shouldDeleteAudioRequestById() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var request = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(request.getId());
        assertNotNull(mediaRequestEntity);

        mediaRequestService.deleteAudioRequest(request.getId());
        assertThrows(NoSuchElementException.class, () -> mediaRequestService.getMediaRequestById(request.getId()));
    }

}
