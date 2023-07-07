package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;

@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceImplTest {

    private static final UUID BLOB_LOCATION = UUID.randomUUID();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());

    @Mock
    private DataManagementService mockDataManagementService;

    @Mock
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    @Mock
    private MediaRequestService mockMediaRequestService;

    @Mock
    private DataManagementConfiguration mockDataManagementConfiguration;


    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;


    @Mock
    private MediaRepository mediaRepository;


    @InjectMocks
    private AudioTransformationServiceImpl audioTransformationService;

    @Test
    void testGetAudioBlobData() {
        when(mockDataManagementService.getBlobData(
                mockDataManagementConfiguration.getUnstructuredContainerName(),
                BLOB_LOCATION))
            .thenReturn(BINARY_DATA);

        BinaryData binaryData = audioTransformationService.getAudioBlobData(BLOB_LOCATION);
        assertEquals(BINARY_DATA, binaryData);
    }

    @Test
    void getMediaMetadataShouldReturnRepositoryResultsUnmodifiedWhenRepositoryHasResult() {
        List<MediaEntity> expectedResults = Collections.singletonList(new MediaEntity());

        when(mediaRepository.findAllByHearingId(any()))
            .thenReturn(expectedResults);

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(1);

        assertEquals(expectedResults, mediaEntities);
    }

    @Test
    void getMediaMetadataShouldReturnRepositoryResultsUnmodifiedWhenRepositoryResultIsEmpty() {
        List<MediaEntity> expectedResults = Collections.emptyList();

        when(mediaRepository.findAllByHearingId(any()))
            .thenReturn(expectedResults);

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(1);

        assertEquals(expectedResults, mediaEntities);
    }

    @Test
    void saveProcessedDataShouldSaveBlobAndSetStatus() {
        String containerName = "ContainerName";

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        MediaRequestEntity mediaRequestEntityUpdated = new MediaRequestEntity();
        mediaRequestEntityUpdated.setStatus(COMPLETED);

        when(mockDataManagementConfiguration.getOutboundContainerName()).thenReturn(containerName);

        when(mockDataManagementService.saveBlobData(
            any(),
            any()
        )).thenReturn(BLOB_LOCATION);

        when(mockTransientObjectDirectoryService.saveTransientDataLocation(
            any(),
            any()
        )).thenReturn(mockTransientObjectDirectoryEntity);

        when(mockMediaRequestService.updateAudioRequestStatus(
             any(),
             any()
         )).thenReturn(mediaRequestEntityUpdated);

        audioTransformationService.saveProcessedData(
            mediaRequestEntity,
            BINARY_DATA
        );

        verify(mockDataManagementService).saveBlobData(
            eq(containerName),
            eq(BINARY_DATA)
        );

        verify(mockTransientObjectDirectoryService).saveTransientDataLocation(
            eq(mediaRequestEntity),
            eq(BLOB_LOCATION)
        );

        verify(mockMediaRequestService).updateAudioRequestStatus(
            eq(mediaRequestEntity.getId()),
            eq(COMPLETED)
        );
    }


}
