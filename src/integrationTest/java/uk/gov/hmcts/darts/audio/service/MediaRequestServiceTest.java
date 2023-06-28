package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import java.time.OffsetDateTime;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequest.model.AudioRequestType.DOWNLOAD;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
class MediaRequestServiceTest {

    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @Autowired
    private MediaRequestService mediaRequestService;

    private AudioRequestDetails requestDetails;

    @BeforeAll
    void beforeAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        assertEquals(TimeZone.getTimeZone(UTC), TimeZone.getDefault());

        requestDetails = new AudioRequestDetails(null, null, null, null, null);
        requestDetails.setHearingId(4567);
        requestDetails.setRequestor(1234);
        requestDetails.setRequestType(DOWNLOAD);
    }

    @Test
    @Order(1)
    void shouldGetMediaRequestByIdWhenStartAndEndTimesInsertedWithZuluTime() {
        MediaRequest requestResult = mediaRequestService.getMediaRequestById(-1);
        assertNotNull(requestResult);
        assertEquals(-1, requestResult.getRequestId());
        assertEquals(-2, requestResult.getHearingId());
        assertEquals(-3, requestResult.getRequestor());
        assertEquals(OPEN, requestResult.getStatus());
        assertEquals(DOWNLOAD, requestResult.getRequestType());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:00:00Z"), requestResult.getStartTime());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:45:00Z"), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    @Order(2)
    void shouldGetMediaRequestByIdWhenStartAndEndTimesInsertedWithOffsetTime() {
        MediaRequest requestResult = mediaRequestService.getMediaRequestById(-2);
        assertNotNull(requestResult);
        assertEquals(-2, requestResult.getRequestId());
        assertEquals(-2, requestResult.getHearingId());
        assertEquals(-3, requestResult.getRequestor());
        assertEquals(OPEN, requestResult.getStatus());
        assertEquals(DOWNLOAD, requestResult.getRequestType());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:00:00Z"), requestResult.getStartTime());
        assertEquals(OffsetDateTime.parse("2023-06-26T13:45:00Z"), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    @Order(3)
    void shouldUpdateStatusToProcessing() {
        MediaRequest mediaRequest = mediaRequestService.updateAudioRequestStatus(-1, PROCESSING);

        assertEquals(PROCESSING, mediaRequest.getStatus());
    }

    @Test
    @Order(4)
    void shouldSaveAudioRequestWithZuluTimeOk() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequest requestResult = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN, requestResult.getStatus());
        assertEquals(requestDetails.getHearingId(), requestResult.getHearingId());
        assertEquals(requestDetails.getStartTime(), requestResult.getStartTime());
        assertEquals(requestDetails.getEndTime(), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Disabled("Disabled until h2database TIME ZONE=UTC command works as expected with Transactional - spring.jpa.properties.hibernate.jdbc.time_zone=UTC")
    @Test
    @Order(5)
    void shouldSaveAudioRequestWithOffsetTimeOk() {
        requestDetails.setStartTime(OffsetDateTime.parse("2023-05-31T10:00:00+01:00"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-05-31T13:00:00+01:00"));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequest requestResult = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN, requestResult.getStatus());
        assertEquals(requestDetails.getHearingId(), requestResult.getHearingId());
        assertEquals(OffsetDateTime.parse(T_09_00_00_Z), requestResult.getStartTime());
        assertEquals(OffsetDateTime.parse(T_12_00_00_Z), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    @Order(6)
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeStarts() {
        // In the UK the clocks go forward 1 hour at 1am on the last Sunday in March.
        // The period when the clocks are 1 hour ahead is called British Summer Time (BST).
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-25T23:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T01:30:00Z"));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequest requestResult = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN, requestResult.getStatus());
        assertEquals(requestDetails.getHearingId(), requestResult.getHearingId());
        assertEquals(requestDetails.getStartTime(), requestResult.getStartTime());
        assertEquals(requestDetails.getEndTime(), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    @Order(7)
    void shouldSaveAudioRequestWithZuluTimeOkWhenDaylightSavingTimeEnds() {
        // In the UK the clocks go back 1 hour at 2am on the last Sunday in October.
        requestDetails.setStartTime(OffsetDateTime.parse("2023-10-29T00:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-10-29T02:15:00Z"));

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        MediaRequest requestResult = mediaRequestService.getMediaRequestById(requestId);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN, requestResult.getStatus());
        assertEquals(requestDetails.getHearingId(), requestResult.getHearingId());
        assertEquals(requestDetails.getStartTime(), requestResult.getStartTime());
        assertEquals(requestDetails.getEndTime(), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

}
