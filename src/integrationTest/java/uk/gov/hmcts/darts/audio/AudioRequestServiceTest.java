package uk.gov.hmcts.darts.audio;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.AudioRequest;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.audio.service.AudioRequestService;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
class AudioRequestServiceTest {

    private static final String OPEN_STATUS = "OPEN";
    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @MockBean
    private LockProvider lock;

    @Autowired
    private AudioRequestService audioRequestService;

    @Autowired
    private AudioRequestRepository audioRequestRepository;

    private AudioRequestDetails requestDetails;

    @BeforeEach
    void beforeEach() {
        requestDetails = new AudioRequestDetails(null,null,null,null,null);
        requestDetails.setCaseId("123456");
        requestDetails.setRequester("test@test.com");
        requestDetails.setRequestType("Download");
    }

    @Test
    void saveAudioRequestWithZuluTimeOkConfirmEntryInDb() {
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));

        var requestId = audioRequestService.saveAudioRequest(requestDetails);

        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(requestId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN_STATUS, requestResult.getStatus());
        assertEquals(requestDetails.getCaseId(), requestResult.getCaseId());
        assertEquals(requestDetails.getStartTime(), requestResult.getStartTime());
        assertEquals(requestDetails.getEndTime(), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    void saveAudioRequestWithOffsetTimeOkConfirmEntryInDb() {
        requestDetails.setStartTime(OffsetDateTime.parse("2023-05-31T10:00:00+01:00"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-05-31T13:00:00+01:00"));

        var requestId = audioRequestService.saveAudioRequest(requestDetails);

        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(requestId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN_STATUS, requestResult.getStatus());
        assertEquals(requestDetails.getCaseId(), requestResult.getCaseId());
        assertEquals(OffsetDateTime.parse(T_09_00_00_Z), requestResult.getStartTime());
        assertEquals(OffsetDateTime.parse(T_12_00_00_Z), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    void saveAudioRequestDaylightSavingTimeStarts() {
        // In the UK the clocks go forward 1 hour at 1am on the last Sunday in March.
        // The period when the clocks are 1 hour ahead is called British Summer Time (BST).
        requestDetails.setStartTime(OffsetDateTime.parse("2023-03-25T23:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-03-26T01:30:00Z"));

        var requestId = audioRequestService.saveAudioRequest(requestDetails);

        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(requestId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN_STATUS, requestResult.getStatus());
        assertEquals(requestDetails.getCaseId(), requestResult.getCaseId());
        assertEquals(requestDetails.getStartTime(), requestResult.getStartTime());
        assertEquals(requestDetails.getEndTime(), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    void saveAudioRequestDaylightSavingTimeEnds() {
        // In the UK the clocks go back 1 hour at 2am on the last Sunday in October.
        requestDetails.setStartTime(OffsetDateTime.parse("2023-10-29T00:30:00Z"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-10-29T02:15:00Z"));

        var requestId = audioRequestService.saveAudioRequest(requestDetails);

        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(requestId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals(OPEN_STATUS, requestResult.getStatus());
        assertEquals(requestDetails.getCaseId(), requestResult.getCaseId());
        assertEquals(requestDetails.getStartTime(), requestResult.getStartTime());
        assertEquals(requestDetails.getEndTime(), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

}
