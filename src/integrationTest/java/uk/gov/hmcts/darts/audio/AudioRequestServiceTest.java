package uk.gov.hmcts.darts.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.dto.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.entity.AudioRequest;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.audio.service.AudioRequestService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@ExtendWith(MockitoExtension.class)
public class AudioRequestServiceTest {

    public static final String TEST_EMAIL_ADDRESS = "test@test.com";
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String T_09_00_00_Z = "2023-05-15 09:00:00";
    private static final String T_12_00_00_Z = "2023-05-15 12:00:00";
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

        AudioRequestDetails requestDetails = AudioRequestDetails.builder()
            .caseId(caseId)
            .emailAddress(TEST_EMAIL_ADDRESS)
            .startTime(LocalDateTime.parse(T_09_00_00_Z, formatter))
            .endTime(LocalDateTime.parse(T_12_00_00_Z, formatter))
            .requestType(DOWNLOAD_REQ_TYPE)
            .build();

        audioRequestService.saveAudioRequest(requestDetails);
        List<AudioRequest> resultList = audioRequestRepository.findByRequestId(caseId);
        AudioRequest requestResult = resultList.get(0);
        assertTrue(requestResult.getRequestId() > 0);
        assertEquals("OPEN", requestResult.getStatus());
        assertEquals(caseId, requestResult.getCaseId());
    }
}
