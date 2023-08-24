package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioTransformationServiceTest extends IntegrationBase {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final UUID BLOB_LOCATION = UUID.randomUUID();

    @Autowired
    private AudioTransformationService audioTransformationService;

    @Autowired
    private DataManagementConfiguration dataManagementConfiguration;

    @Autowired
    private AudioConfigurationProperties audioConfigurationProperties;

    @Autowired
    private AudioTransformationServiceGivenBuilder given;

    @MockBean
    private DataManagementService mockDataManagementService;

    @MockBean
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    @MockBean
    FileOperationService mockFileOperationService;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @Test
    void shouldGetAudioBlobDataUsingLocation() {
        String containerName = dataManagementConfiguration.getUnstructuredContainerName();

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
        String containerName = dataManagementConfiguration.getOutboundContainerName();

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
        MediaRequestEntity mediaRequestEntity = dartsDatabase.createAndLoadMediaRequestEntity();

        when(mockTransientObjectDirectoryService.saveTransientDataLocation(
            mediaRequestEntity,
            BLOB_LOCATION
        )).thenReturn(mockTransientObjectDirectoryEntity);

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = audioTransformationService.saveTransientDataLocation(
            mediaRequestEntity,
            BLOB_LOCATION
        );

        assertNotNull(transientObjectDirectoryEntity);
        verify(mockTransientObjectDirectoryService).saveTransientDataLocation(
            eq(mediaRequestEntity),
            eq(BLOB_LOCATION)
        );
        verifyNoMoreInteractions(mockTransientObjectDirectoryService);
    }

    @Test
    void getMediaMetadataShouldReturnExpectedMediaEntitiesWhenHearingIdHasRelatedMedia() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity1());
        Integer hearingIdWithMedia = given.getHearingEntityWithMedia1().getId();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingIdWithMedia);

        assertEquals(2, mediaEntities.size());

        List<Integer> mediaIds = mediaEntities.stream().map(MediaEntity::getId).collect(toList());
        assertTrue(mediaIds.contains(given.getMediaEntity1().getId()));
        assertTrue(mediaIds.contains(given.getMediaEntity2().getId()));

        assertFalse(mediaIds.contains(given.getMediaEntity3().getId()));
    }

    @Test
    void getMediaMetadataShouldReturnEmptyListWhenHearingIdHasNoRelatedMedia() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity1());
        Integer hearingIdWithNoRelatedMedia = given.getHearingEntityWithoutMedia().getId();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingIdWithNoRelatedMedia);

        assertEquals(0, mediaEntities.size());
    }

    @Test
    void getMediaMetadataShouldReturnEmptyListWhenHearingIdDoesNotExist() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity1());

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(123_456);

        assertEquals(0, mediaEntities.size());
    }

    @Test
    void shouldGetMediaLocation() {
        given.setupTest();
        var externalObjectDirectoryEntity =
            given.externalObjectDirForMedia(given.getMediaEntity1());

        assertEquals(
            externalObjectDirectoryEntity.getExternalLocation(),
            audioTransformationService.getMediaLocation(given.getMediaEntity1()).get()
        );
    }

    @Test
    void shouldGetEmptyOptionalMediaLocationWhenNoExternalObjectDirectoryExists() {
        MediaEntity newMedia = new MediaEntity();
        newMedia.setCourtroom(somePersistedCourtroom());
        newMedia.setChannel(1);
        newMedia.setTotalChannels(4);
        newMedia.setStart(OffsetDateTime.parse("2023-07-04T10:00:00Z"));
        newMedia.setEnd(OffsetDateTime.parse("2023-07-04T11:00:00Z"));
        newMedia = dartsDatabase.save(newMedia);

        assertEquals(Optional.empty(), audioTransformationService.getMediaLocation(newMedia));
    }

    @Test
    void shouldGetMediaLocationWithWarningThatMultipleExistByStatusAndType() {

        MediaEntity newMedia = new MediaEntity();
        newMedia.setCourtroom(somePersistedCourtroom());
        newMedia.setChannel(1);
        newMedia.setTotalChannels(4);
        newMedia.setStart(OffsetDateTime.parse("2023-07-04T16:00:00Z"));
        newMedia.setEnd(OffsetDateTime.parse("2023-07-04T17:00:00Z"));
        newMedia = dartsDatabase.save(newMedia);

        ExternalLocationTypeEntity externalLocationTypeEntity =
            dartsDatabase.getExternalLocationTypeRepository().getReferenceById(UNSTRUCTURED.getId());
        ObjectDirectoryStatusEntity objectDirectoryStatus =
            dartsDatabase.getObjectDirectoryStatusRepository().getReferenceById(STORED.getId());
        UUID externalLocation1 = UUID.randomUUID();
        UUID externalLocation2 = UUID.randomUUID();
        ExternalObjectDirectoryEntity externalObjectDirectory1 = externalObjectDirectoryStub.createExternalObjectDirectory(
            newMedia,
            objectDirectoryStatus,
            externalLocationTypeEntity,
            externalLocation1
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(externalObjectDirectory1);

        ExternalObjectDirectoryEntity externalObjectDirectory2 = externalObjectDirectoryStub.createExternalObjectDirectory(
            newMedia,
            objectDirectoryStatus,
            externalLocationTypeEntity,
            externalLocation2
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(externalObjectDirectory2);

        assertEquals(
            Optional.of(externalLocation1),
            audioTransformationService.getMediaLocation(newMedia)
        );

    }

    private CourtroomEntity somePersistedCourtroom() {
        return dartsDatabase.createCourtroomUnlessExists("some-courthouse", "some-room");
    }

    @Test
    void shouldSaveAudioBlobDataUsingTempWorkSpace() throws IOException {
        String fileName = "caseAudioFile.pdf";
        String tempWorkspace = audioConfigurationProperties.getTempBlobWorkspace();
        Path filePath = Path.of(tempWorkspace).resolve(fileName);

        when(mockFileOperationService.saveFileToTempWorkspace(
            BINARY_DATA,
            fileName
        )).thenReturn(filePath);

        Path actualFilePath = audioTransformationService.saveBlobDataToTempWorkspace(BINARY_DATA, fileName);

        assertEquals(filePath, actualFilePath);
        verify(mockFileOperationService).saveFileToTempWorkspace(
            eq(BINARY_DATA),
            eq(fileName)
        );
        verifyNoMoreInteractions(mockFileOperationService);
    }

}
