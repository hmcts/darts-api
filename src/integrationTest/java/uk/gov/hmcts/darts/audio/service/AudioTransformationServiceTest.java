package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
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
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearingId(-1);
        mediaRequestEntity.setRequestor(-2);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        mediaRequestEntity.setEndTime(OffsetDateTime.parse("2023-06-26T13:45:00Z"));
        mediaRequestEntity.setOutboundLocation(null);
        mediaRequestEntity.setOutputFormat(null);
        mediaRequestEntity.setOutputFilename(null);
        mediaRequestEntity.setLastAccessedDateTime(null);

        MediaRequestEntity savedMediaRequestEntity = mediaRequestRepository.saveAndFlush(mediaRequestEntity);
        assertNotNull(savedMediaRequestEntity);
        requestId = savedMediaRequestEntity.getRequestId();
    }

    @Test
    void processAudioRequest() {
        MediaRequestEntity processingMediaRequestEntity = audioTransformationService.processAudioRequest(requestId);
        assertEquals(PROCESSING, processingMediaRequestEntity.getStatus());
    }


    @Test
    @DisplayName("Check if file is created in temporary folder")
    void saveBlobDataToTempWorkspaceTest_1() throws IOException {
        String data = "this is a binary data file";
        BinaryData mediaFile = BinaryData.fromString(data);

        String fileName = "caseAudioFile";
        Path filePath = audioTransformationService.saveBlobDataToTempWorkspace(mediaFile, fileName);

        assertTrue(Files.exists(filePath));
    }

}
