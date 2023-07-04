package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequest.model.AudioRequestType.DOWNLOAD;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@TestInstance(Lifecycle.PER_CLASS)
class AudioTransformationServiceTest {

    @Autowired
    private MediaRequestRepository mediaRequestRepository;
    @Autowired
    private AudioTransformationService audioTransformationService;
    @Autowired
    private DataManagementConfiguration dataManagementConfiguration;
    @MockBean
    private DataManagementService mockDataManagementService;
    @MockBean
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final UUID BLOB_LOCATION = UUID.randomUUID();

    private String containerName;
    private MediaRequestEntity savedMediaRequestEntity;
    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @BeforeAll
    void beforeAll() {
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

        savedMediaRequestEntity = mediaRequestRepository.saveAndFlush(mediaRequestEntity);
        assertNotNull(savedMediaRequestEntity);

        containerName = dataManagementConfiguration.getUnstructuredContainerName();
    }

    @Test
    void shouldProcessAudioRequest() {
        MediaRequestEntity processingMediaRequestEntity = audioTransformationService.processAudioRequest(
            savedMediaRequestEntity.getRequestId());
        assertEquals(PROCESSING, processingMediaRequestEntity.getStatus());
    }

    @Test
    void shouldGetAudioBlobDataUsingLocation() {
        when(mockDataManagementService.getBlobData(
            containerName,
            BLOB_LOCATION
        )).thenReturn(BINARY_DATA);

        BinaryData binaryData = audioTransformationService.getAudioBlobData(BLOB_LOCATION);

        assertEquals(BINARY_DATA, binaryData);
        verify(mockDataManagementService).getBlobData(
            eq(containerName),
            eq(BLOB_LOCATION)
        );
        verifyNoMoreInteractions(mockDataManagementService);
    }

    @Test
    void shouldSaveAudioBlobData() {
        when(mockDataManagementService.saveBlobData(
            containerName,
            BINARY_DATA
        )).thenReturn(BLOB_LOCATION);

        UUID externalLocation = audioTransformationService.saveAudioBlobData(BINARY_DATA);

        assertEquals(BLOB_LOCATION, externalLocation);
        verify(mockDataManagementService).saveBlobData(
            eq(containerName),
            eq(BINARY_DATA)
        );
        verifyNoMoreInteractions(mockDataManagementService);
    }

    @Test
    void shouldSaveTransientDataLocation() {
        when(mockTransientObjectDirectoryService.saveTransientDataLocation(
            savedMediaRequestEntity,
            BLOB_LOCATION
        )).thenReturn(mockTransientObjectDirectoryEntity);

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = audioTransformationService.saveTransientDataLocation(
            savedMediaRequestEntity,
            BLOB_LOCATION
        );

        assertNotNull(transientObjectDirectoryEntity);
        verify(mockTransientObjectDirectoryService).saveTransientDataLocation(
            eq(savedMediaRequestEntity),
            eq(BLOB_LOCATION)
        );
        verifyNoMoreInteractions(mockTransientObjectDirectoryService);
    }

}
