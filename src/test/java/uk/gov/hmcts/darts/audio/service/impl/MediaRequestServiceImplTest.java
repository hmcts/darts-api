package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestDetails;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequest.model.AudioRequestType.DOWNLOAD;

@ExtendWith(MockitoExtension.class)
class MediaRequestServiceImplTest {

    private static final Integer TEST_REQUESTER = 1234;
    private static final String OFFSET_T_09_00_00_Z = "2023-05-31T09:00:00Z";
    private static final String OFFSET_T_12_00_00_Z = "2023-05-31T12:00:00Z";

    @InjectMocks
    private MediaRequestServiceImpl mediaRequestService;

    @Mock
    private MediaRequestRepository mediaRequestRepository;

    private MediaRequestEntity mockMediaRequestEntity;

    @BeforeEach
    void beforeEach() {

        mockMediaRequestEntity = new MediaRequestEntity();
        mockMediaRequestEntity.setRequestId(1);
        mockMediaRequestEntity.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        mockMediaRequestEntity.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        mockMediaRequestEntity.setRequestor(TEST_REQUESTER);
        mockMediaRequestEntity.setStatus(OPEN);
        mockMediaRequestEntity.setAttempts(0);
        OffsetDateTime now = OffsetDateTime.now();
        mockMediaRequestEntity.setCreatedDateTime(now);
        mockMediaRequestEntity.setLastUpdatedDateTime(now);
    }

    @Test
    void whenSavingAudioRequestIsSuccessful() {

        Integer hearingId = 4567;
        mockMediaRequestEntity.setRequestId(1);

        var requestDetails = new AudioRequestDetails();
        requestDetails.setHearingId(hearingId);
        requestDetails.setRequestor(TEST_REQUESTER);
        requestDetails.setStartTime(OffsetDateTime.parse(OFFSET_T_09_00_00_Z));
        requestDetails.setEndTime(OffsetDateTime.parse(OFFSET_T_12_00_00_Z));
        requestDetails.setRequestType(DOWNLOAD);

        when(mediaRequestRepository.saveAndFlush(any(MediaRequestEntity.class))).thenReturn(mockMediaRequestEntity);

        var requestId = mediaRequestService.saveAudioRequest(requestDetails);

        verify(mediaRequestRepository, times(1)).saveAndFlush(any(MediaRequestEntity.class));
        assertEquals(requestId, mockMediaRequestEntity.getRequestId());
    }

}
