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
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
public class AudioRequestServiceTest {

    public static final String TEST_REQUESTER = "test@test.com";
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String T_09_00_00_Z = "2023-05-15T09:00:00.001Z";
    private static final String T_12_00_00_Z = "2023-05-15T12:00:00.001Z";
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
    void saveAudioRequestOkConfirmEntryInDb() {
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
    }
}
