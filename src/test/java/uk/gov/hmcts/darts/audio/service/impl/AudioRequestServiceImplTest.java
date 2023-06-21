package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;
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

    private static final Integer TEST_REQUESTER = 1234;
    private static final String OFFSET_T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-31T12:00:00Z";
    private static final String DOWNLOAD_REQ_TYPE = "Download";

    private static final String REQUEST_STATUS_OPEN = "OPEN";

    @InjectMocks
    private AudioRequestServiceImpl audioRequestService;

    @Mock
    private AudioRequestRepository audioRequestRepository;

    private MediaRequest mockMediaRequest;

    @BeforeEach
    void beforeEach() {

        mockMediaRequest = new MediaRequest();
        mockMediaRequest.setRequestId(1);
        mockMediaRequest.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        mockMediaRequest.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        mockMediaRequest.setRequestor(TEST_REQUESTER);
        mockMediaRequest.setStatus(REQUEST_STATUS_OPEN);
        mockMediaRequest.setAttempts(0);
        OffsetDateTime now = OffsetDateTime.now();
        mockMediaRequest.setCreatedDateTime(now);
        mockMediaRequest.setLastUpdatedDateTime(now);
    }

    @Test
    void whenSavingAudioRequestIsSuccessful() {

        Integer hearingId = 4567;
        mockMediaRequest.setRequestId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD_REQ_TYPE);

        when(audioRequestRepository.saveAndFlush(any(MediaRequest.class))).thenReturn(mockMediaRequest);

        var requestId = audioRequestService.saveAudioRequest(requestDetails);

        verify(audioRequestRepository, times(1)).saveAndFlush(any(MediaRequest.class));
        assertEquals(requestId, mockMediaRequest.getRequestId());
    }

}
