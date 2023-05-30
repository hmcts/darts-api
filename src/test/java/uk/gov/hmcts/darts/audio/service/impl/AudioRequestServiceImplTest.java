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

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioRequestServiceImplTest {

    private static final String TEST_REQUESTER = "test@test.com";
    private static final String OFFSET_T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-31T12:00:00Z";
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
        mockAudioRequest.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        mockAudioRequest.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        mockAudioRequest.setRequester(TEST_REQUESTER);
        mockAudioRequest.setStatus(REQUEST_STATUS_OPEN);
        mockAudioRequest.setAttempts(0);
        OffsetDateTime now = OffsetDateTime.now();
        mockAudioRequest.setCreatedDateTime(now);
        mockAudioRequest.setLastUpdatedDateTime(now);
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

        verify(audioRequestRepository, times(1)).saveAndFlush(any(AudioRequest.class));
        assertEquals(requestId, mockAudioRequest.getRequestId());
    }

}
