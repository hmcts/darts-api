package uk.gov.hmcts.darts.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    public static final String TEST_REQUESTER = "test@test.com";
    private static final String T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String T_12_00_00_Z = "2023-05-31T12:00:00Z";
    private static final String DOWNLOAD_REQ_TYPE = "Download";

    @Autowired
    AudioRequestService audioRequestService;

    @Autowired
    AudioRequestRepository audioRequestRepository;

    @BeforeEach
    void beforeEach() {
        audioRequestRepository.deleteAll();
    }

    @Test
    void saveAudioRequestWithZuluTimeOkConfirmEntryInDb() {
        String caseId = "123456";

        var requestDetails = new AudioRequestDetails();
        requestDetails.setCaseId(caseId);
        requestDetails.setRequester(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD_REQ_TYPE);

        var requestId = audioRequestService.saveAudioRequest(requestDetails);
        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(requestId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals("OPEN", requestResult.getStatus());
        assertEquals(caseId, requestResult.getCaseId());
        assertEquals(OffsetDateTime.parse(T_09_00_00_Z), requestResult.getStartTime());
        assertEquals(OffsetDateTime.parse(T_12_00_00_Z), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }

    @Test
    void saveAudioRequestWithOffsetTimeOkConfirmEntryInDb() {
        String caseId = "123456";

        var requestDetails = new AudioRequestDetails();
        requestDetails.setCaseId(caseId);
        requestDetails.setRequester(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse("2023-05-31T10:00:00+01:00"));
        requestDetails.setEndTime(OffsetDateTime.parse("2023-05-31T13:00:00+01:00"));
        requestDetails.setRequestType(DOWNLOAD_REQ_TYPE);

        var requestId = audioRequestService.saveAudioRequest(requestDetails);
        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(requestId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals("OPEN", requestResult.getStatus());
        assertEquals(caseId, requestResult.getCaseId());
        assertEquals(OffsetDateTime.parse(T_09_00_00_Z), requestResult.getStartTime());
        assertEquals(OffsetDateTime.parse(T_12_00_00_Z), requestResult.getEndTime());
        assertNotNull(requestResult.getCreatedDateTime());
        assertNotNull(requestResult.getLastUpdatedDateTime());
    }
}
