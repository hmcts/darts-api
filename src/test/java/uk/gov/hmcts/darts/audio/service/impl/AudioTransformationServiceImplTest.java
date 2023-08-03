package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;

@ExtendWith(MockitoExtension.class)
class AudioTransformationServiceImplTest {

    private static final UUID BLOB_LOCATION = UUID.randomUUID();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());

    @Mock
    private DataManagementApi mockDataManagementApi;

    @Mock
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @Mock
    private MediaRepository mediaRepository;


    @InjectMocks
    private AudioTransformationServiceImpl audioTransformationService;

    @Test
    void testGetAudioBlobData() {
        when(mockDataManagementApi.getBlobDataFromUnstructuredContainer(BLOB_LOCATION))
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
        final MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        final MediaRequestEntity mediaRequestEntityUpdated = new MediaRequestEntity();
        mediaRequestEntityUpdated.setStatus(COMPLETED);

        when(mockDataManagementApi.saveBlobDataToOutboundContainer(any()))
            .thenReturn(BLOB_LOCATION);

        when(mockTransientObjectDirectoryService.saveTransientDataLocation(
            any(),
            any()
        )).thenReturn(mockTransientObjectDirectoryEntity);

        audioTransformationService.saveProcessedData(
            mediaRequestEntity,
            BINARY_DATA
        );

        verify(mockDataManagementApi).saveBlobDataToOutboundContainer(BINARY_DATA);

        verify(mockTransientObjectDirectoryService).saveTransientDataLocation(mediaRequestEntity, BLOB_LOCATION);
    }

}
