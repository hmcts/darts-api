package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.AudioRequest;
import uk.gov.hmcts.darts.audio.repository.AudioRequestRepository;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AudioRequestServiceImplTest {

    private static final String TEST_REQUESTER = "test@test.com";
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String OFFSET_T_09_00_00_Z = "2023-05-15T09:00:00.001Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-15T12:00:00.001Z";
    private static final String TIMESTAMP_T_09_00_00_Z = "2023-05-15 09:00:00.001";
    private static final String TIMESTAMP_T_12_00_00_Z = "2023-05-15 12:00:00.001";
    private static final String DOWNLOAD_REQ_TYPE = "Download";

    private static final String REQUEST_STATUS_OPEN = "OPEN";

    @InjectMocks
    private AudioRequestServiceImpl audioRequestService;

    @Mock
    private AudioRequestRepository audioRequestRepository;

    private AudioRequest mockAudioRequest;

    @BeforeEach
    void beforeEach() {

        mockAudioRequest = new AudioRequest();
        mockAudioRequest.setRequestId(1);
        mockAudioRequest.setStartTime(Timestamp.valueOf(TIMESTAMP_T_09_00_00_Z));
        mockAudioRequest.setEndTime(Timestamp.valueOf(TIMESTAMP_T_12_00_00_Z));
        mockAudioRequest.setRequester(TEST_REQUESTER);
        mockAudioRequest.setStatus(REQUEST_STATUS_OPEN);
        mockAudioRequest.setAttempts(0);
        mockAudioRequest.setCreatedDateTime(Timestamp.valueOf("2023-05-23 18:53:24.288"));
        mockAudioRequest.setLastUpdatedDateTime(Timestamp.valueOf("2023-05-23 18:53:24.288"));
    }

    @Test
    void whenSavingAudioRequestIsSuccessful() {

        String caseId = "123456";
        mockAudioRequest.setRequestId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setCaseId(caseId);
        requestDetails.setRequester(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD_REQ_TYPE);

        when(audioRequestRepository.saveAndFlush(any(AudioRequest.class))).thenReturn(mockAudioRequest);

        var requestId = audioRequestService.saveAudioRequest(requestDetails);

        verify(audioRequestRepository,times(1)).saveAndFlush(any(AudioRequest.class));
        assertEquals(requestId, mockAudioRequest.getRequestId());
    }

}
