package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequest.model.AudioRequestType.DOWNLOAD;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@TestInstance(Lifecycle.PER_CLASS)
class MediaRequestServiceTest {

    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @Autowired
    private MediaRequestService mediaRequestService;

    private AudioRequestDetails requestDetails;

    @BeforeAll
    void beforeAll() {
        requestDetails = new AudioRequestDetails(null, null, null, null, null);
        requestDetails.setHearingId(4567);
        requestDetails.setRequestor(1234);
        requestDetails.setRequestType(DOWNLOAD);
    }

    @Test
    @Order(1)
    void shouldGetMediaRequestByIdWhenStartAndEndTimesInsertedWithZuluTime() {
        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(-1);
        assertNotNull(mediaRequestEntity);
        assertEquals(-1, mediaRequestEntity.getRequestId());
        assertEquals(-2, mediaRequestEntity.getHearingId());
        assertEquals(-3, mediaRequestEntity.getRequestor());
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(DOWNLOAD, mediaRequestEntity.getRequestType());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:00:00Z"), mediaRequestEntity.getStartTime());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:45:00Z"), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getLastUpdatedDateTime());
    }

    @Test
    @Order(2)
    void shouldGetMediaRequestByIdWhenStartAndEndTimesInsertedWithOffsetTime() {
        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(-2);
        assertNotNull(mediaRequestEntity);
        assertEquals(-2, mediaRequestEntity.getRequestId());
        assertEquals(-2, mediaRequestEntity.getHearingId());
        assertEquals(-3, mediaRequestEntity.getRequestor());
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(DOWNLOAD, mediaRequestEntity.getRequestType());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:00:00Z"), mediaRequestEntity.getStartTime());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:45:00Z"), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getLastUpdatedDateTime());
    }

    @Test
    @Order(3)
    void shouldThrowExceptionWhenGetMediaRequestByIdInvalid() {
        assertThrows(NoSuchElementException.class, () -> mediaRequestService.getMediaRequestById(-3));
    }

    @Test
    @Order(4)
    void shouldUpdateStatusToProcessing() {
        MediaRequestEntity mediaRequestEntity = mediaRequestService.updateAudioRequestStatus(-1, PROCESSING);

        assertEquals(PROCESSING, mediaRequestEntity.getStatus());
    }

    @Test
    @Order(5)
    void shouldSaveAudioRequestWithZuluTimeOk() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(mediaRequestEntity.getRequestId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearingId());
        assertEquals(requestDetails.getStartTime(), mediaRequestEntity.getStartTime());
        assertEquals(requestDetails.getEndTime(), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getLastUpdatedDateTime());
    }

    @Test
    @Order(6)
    void shouldSaveAudioRequestWithOffsetTimeOk() {
        requestDetails.setStartTime(OffsetDateTime.parse("2023-05-31T10:00:00+01:00"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-05-31T13:00:00+01:00"));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(mediaRequestEntity.getRequestId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearingId());
        assertEquals(OffsetDateTime.parse(T_09_00_00_Z), mediaRequestEntity.getStartTime());
        assertEquals(OffsetDateTime.parse(T_12_00_00_Z), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getLastUpdatedDateTime());
    }

    @Test
    @Order(7)
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeStarts() {
        // In the UK the clocks go forward 1 hour at 1am on the last Sunday in March.
        // The period when the clocks are 1 hour ahead is called British Summer Time (BST).
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-25T23:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T01:30:00Z"));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(mediaRequestEntity.getRequestId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearingId());
        assertEquals(requestDetails.getStartTime(), mediaRequestEntity.getStartTime());
        assertEquals(requestDetails.getEndTime(), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getLastUpdatedDateTime());
    }

    @Test
    @Order(8)
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeEnds() {
        // In the UK the clocks go back 1 hour at 2am on the last Sunday in October.
        requestDetails.setStartTime(OffsetDateTime.parse("2023-10-29T00:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-10-29T02:15:00Z"));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(mediaRequestEntity.getRequestId() > 0);
        assertEquals(OPEN, mediaRequestEntity.getStatus());
        assertEquals(requestDetails.getHearingId(), mediaRequestEntity.getHearingId());
        assertEquals(requestDetails.getStartTime(), mediaRequestEntity.getStartTime());
        assertEquals(requestDetails.getEndTime(), mediaRequestEntity.getEndTime());
        assertNotNull(mediaRequestEntity.getCreatedDateTime());
        assertNotNull(mediaRequestEntity.getLastUpdatedDateTime());
    }

}
