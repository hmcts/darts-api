package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
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
    @Autowired
    private DataManagementConfiguration dataManagementConfiguration;
    @MockBean
    private DataManagementService dataManagementService;

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final UUID BLOB_LOCATION = UUID.randomUUID();

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
    void testGetAudioBlobDataUsingLocation() {
        when(dataManagementService.getBlobData(dataManagementConfiguration.getUnstructuredContainerName(), BLOB_LOCATION)).thenReturn(BINARY_DATA);
        BinaryData binaryData = audioTransformationService.getAudioBlobData(BLOB_LOCATION);
        assertEquals(BINARY_DATA, binaryData);
    }

}
