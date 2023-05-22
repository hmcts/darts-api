package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.dto.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.entity.AudioRequest;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AudioRequestServiceImplTest {

    private static final String TEST_EMAIL_ADDRESS = "test@test.com";
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String T_09_00_00_Z = "2023-05-15 09:00:00";
    private static final String T_12_00_00_Z = "2023-05-15 12:00:00";
    private static final String DOWNLOAD_REQ_TYPE = "Download";

    @InjectMocks
    private AudioRequestServiceImpl audioRequestService;

    @Mock
    private AudioRequestRepository audioRequestRepository;


    @BeforeEach
    void beforeEach() {
    }

    @Test
    void whenSavingAudioRequestIsSuccessful() {

        AudioRequestDetails requestDetails = AudioRequestDetails.builder()
            .caseId("123456")
            .emailAddress(TEST_EMAIL_ADDRESS)
            .startTime(LocalDateTime.parse(T_09_00_00_Z, formatter))
            .endTime(LocalDateTime.parse(T_12_00_00_Z, formatter))
            .requestType(DOWNLOAD_REQ_TYPE)
            .build();

        audioRequestService.saveAudioRequest(requestDetails);
        verify(audioRequestRepository,times(1)).saveAndFlush(any(AudioRequest.class));
    }
}
