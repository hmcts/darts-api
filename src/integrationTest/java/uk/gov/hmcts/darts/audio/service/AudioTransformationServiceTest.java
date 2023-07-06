package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingMediaRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.ReprovisionDatabaseBeforeEach;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequest.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.STORED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ReprovisionDatabaseBeforeEach
@TestInstance(Lifecycle.PER_CLASS)
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"})
class AudioTransformationServiceTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final UUID BLOB_LOCATION = UUID.randomUUID();

    @Autowired
    private AudioTransformationService audioTransformationService;

    @Autowired
    private DataManagementConfiguration dataManagementConfiguration;

    @Autowired
    private MediaRequestRepository mediaRequestRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private CourtroomRepository courtroomRepository;

    @Autowired
    private HearingRepository hearingRepository;

    @Autowired
    private HearingMediaRepository hearingMediaRepository;

    @Autowired
    private ObjectDirectoryStatusRepository objectDirectoryStatusRepository;

    @Autowired
    private AudioConfigurationProperties audioConfigurationProperties;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @MockBean
    private DataManagementService mockDataManagementService;

    @MockBean
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    @MockBean
    FileOperationService mockFileOperationService;

    private MediaRequestEntity savedMediaRequestEntity;
    private HearingEntity hearingEntityWithMedia1;
    private HearingEntity hearingEntityWithoutMedia;
    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;

    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @Test
    void shouldProcessAudioRequest() {
        createAndLoadMediaRequestEntity();

        MediaRequestEntity processingMediaRequestEntity = audioTransformationService.processAudioRequest(
            savedMediaRequestEntity.getRequestId());

        assertEquals(PROCESSING, processingMediaRequestEntity.getStatus());
    }

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
        String containerName = dataManagementConfiguration.getUnstructuredContainerName();

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
        createAndLoadMediaRequestEntity();

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

    @Test
    @Transactional
    void getMediaMetadataShouldReturnExpectedMediaEntitiesWhenHearingIdHasRelatedMedia() {
        createAndLoadMediaEntityGraph();

        Integer hearingIdWithMedia = hearingEntityWithMedia1.getId();
        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingIdWithMedia);

        assertEquals(2, mediaEntities.size());

        assertThat(mediaEntities, hasItem(mediaEntity1));
        assertThat(mediaEntities, hasItem(mediaEntity2));

        assertThat(mediaEntities, not(hasItem(mediaEntity3)));
    }

    @Test
    void getMediaMetadataShouldReturnEmptyListWhenHearingIdHasNoRelatedMedia() {
        createAndLoadMediaEntityGraph();

        Integer hearingIdWithNoRelatedMedia = hearingEntityWithoutMedia.getId();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(hearingIdWithNoRelatedMedia);

        assertEquals(0, mediaEntities.size());
    }

    @Test
    void getMediaMetadataShouldReturnEmptyListWhenHearingIdDoesNotExist() {
        createAndLoadMediaEntityGraph();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaMetadata(123_456);

        assertEquals(0, mediaEntities.size());
    }

    @Test
    void shouldGetMediaLocation() {
        MediaEntity media = mediaRepository.getReferenceById(-1);

        assertEquals(
            Optional.of(UUID.fromString("a7ad5828-6a20-4bd0-adb1-bf1496a2622a")),
            audioTransformationService.getMediaLocation(media)
        );
    }

    @Test
    void shouldGetEmptyOptionalMediaLocationWhenNoExternalObjectDirectoryExists() {
        CourtroomEntity courtroom = courtroomRepository.getReferenceById(1);

        MediaEntity newMedia = new MediaEntity();
        newMedia.setCourtroom(courtroom);
        newMedia.setChannel(1);
        newMedia.setTotalChannels(4);
        newMedia.setStart(OffsetDateTime.parse("2023-07-04T10:00:00Z"));
        newMedia.setEnd(OffsetDateTime.parse("2023-07-04T11:00:00Z"));
        newMedia = mediaRepository.saveAndFlush(newMedia);

        assertEquals(Optional.empty(), audioTransformationService.getMediaLocation(newMedia));
    }

    @Test
    void shouldGetMediaLocationWithWarningThatMultipleExistByStatusAndType() {

        CourtroomEntity courtroom = courtroomRepository.getReferenceById(1);

        MediaEntity newMedia = new MediaEntity();
        newMedia.setCourtroom(courtroom);
        newMedia.setChannel(1);
        newMedia.setTotalChannels(4);
        newMedia.setStart(OffsetDateTime.parse("2023-07-04T16:00:00Z"));
        newMedia.setEnd(OffsetDateTime.parse("2023-07-04T17:00:00Z"));
        newMedia = mediaRepository.saveAndFlush(newMedia);

        ObjectDirectoryStatusEntity objectDirectoryStatus = objectDirectoryStatusRepository.getReferenceById(STORED.getId());
        UUID externalLocation1 = UUID.randomUUID();
        UUID externalLocation2 = UUID.randomUUID();
        ExternalObjectDirectoryEntity externalObjectDirectory1 = CommonTestDataUtil.createExternalObjectDirectory(
            newMedia,
            objectDirectoryStatus,
            "unstructured",
            externalLocation1
        );
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory1);

        ExternalObjectDirectoryEntity externalObjectDirectory2 = CommonTestDataUtil.createExternalObjectDirectory(
            newMedia,
            objectDirectoryStatus,
            "unstructured",
            externalLocation2
        );
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory2);

        assertEquals(
            Optional.of(externalLocation1),
            audioTransformationService.getMediaLocation(newMedia)
        );

    }

    private void createAndLoadMediaRequestEntity() {
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearingId(-1);
        mediaRequestEntity.setRequestor(-2);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setStartTime(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        mediaRequestEntity.setEndTime(OffsetDateTime.parse("2023-06-26T13:45:00Z"));
        mediaRequestEntity.setOutputFormat(null);
        mediaRequestEntity.setOutputFilename(null);
        mediaRequestEntity.setLastAccessedDateTime(null);

        savedMediaRequestEntity = mediaRequestRepository.saveAndFlush(mediaRequestEntity);
        assertNotNull(savedMediaRequestEntity);
    }

    private void createAndLoadMediaEntityGraph() {
        var caseEntity = CommonTestDataUtil.createCase("1");
        caseRepository.saveAndFlush(caseEntity);

        var courtroomEntity = CommonTestDataUtil.createCourtroom("Int Test Courtroom");
        courtroomRepository.saveAndFlush(courtroomEntity);

        hearingEntityWithMedia1 = CommonTestDataUtil.createHearing(caseEntity, courtroomEntity);
        var hearingEntityWithMedia2 = CommonTestDataUtil.createHearing(caseEntity, courtroomEntity);
        hearingEntityWithoutMedia = CommonTestDataUtil.createHearing(caseEntity, courtroomEntity);
        hearingRepository.saveAllAndFlush(Arrays.asList(
            hearingEntityWithMedia1,
            hearingEntityWithMedia2,
            hearingEntityWithoutMedia
        ));

        mediaEntity1 = CommonTestDataUtil.createMedia(courtroomEntity);
        mediaEntity2 = CommonTestDataUtil.createMedia(courtroomEntity);
        mediaEntity3 = CommonTestDataUtil.createMedia(courtroomEntity);
        mediaRepository.saveAllAndFlush(Arrays.asList(
            mediaEntity1,
            mediaEntity2,
            mediaEntity3
        ));

        var hearingMediaEntity1 = CommonTestDataUtil.createHearingMedia(
            hearingEntityWithMedia1,
            mediaEntity1
        );
        var hearingMediaEntity2 = CommonTestDataUtil.createHearingMedia(
            hearingEntityWithMedia1,
            mediaEntity2
        );
        var hearingMediaEntity3 = CommonTestDataUtil.createHearingMedia(
            hearingEntityWithMedia2,
            mediaEntity3
        );
        hearingMediaRepository.saveAllAndFlush(Arrays.asList(
            hearingMediaEntity1,
            hearingMediaEntity2,
            hearingMediaEntity3
        ));
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
