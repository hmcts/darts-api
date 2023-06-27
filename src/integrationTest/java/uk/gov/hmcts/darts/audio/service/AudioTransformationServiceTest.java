package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequest;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequest.model.AudioRequestType.DOWNLOAD;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class AudioTransformationServiceTest {

    @Autowired
    private MediaRequestRepository mediaRequestRepository;
    @Autowired
    private AudioTransformationService audioTransformationService;

    private Integer requestId;

    @BeforeEach
    void setUp() {
        MediaRequest mediaRequest = new MediaRequest();
        mediaRequest.setHearingId(-1);
        mediaRequest.setRequestor(-2);
        mediaRequest.setStatus(OPEN);
        mediaRequest.setRequestType(DOWNLOAD);
        mediaRequest.setAttempts(0);
        mediaRequest.setStartTime(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        mediaRequest.setEndTime(OffsetDateTime.parse("2023-06-26T13:45:00Z"));
        mediaRequest.setOutboundLocation(null);
        mediaRequest.setOutputFormat(null);
        mediaRequest.setOutputFilename(null);
        mediaRequest.setLastAccessedDateTime(null);

        MediaRequest savedMediaRequest = mediaRequestRepository.saveAndFlush(mediaRequest);
        assertNotNull(savedMediaRequest);
        requestId = savedMediaRequest.getRequestId();
    }

    @Test
    void processAudioRequest() {
        MediaRequest processingMediaRequest = audioTransformationService.processAudioRequest(requestId);
        assertEquals(PROCESSING, processingMediaRequest.getStatus());
    }

}
