package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.hmcts.darts.PostgresqlContainer;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingMediaRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.ReprovisionDatabaseBeforeEach;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
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

@SpringBootTest
@ActiveProfiles({"intTest", "postgresTestContainer"})
@ReprovisionDatabaseBeforeEach
@SuppressWarnings("PMD.ExcessiveImports")
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
    private CommonTestDataUtil commonTestDataUtil;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private CourtroomRepository courtroomRepository;

    @Autowired
    private HearingRepository hearingRepository;

    @Autowired
    private HearingMediaRepository hearingMediaRepository;

    @MockBean
    private DataManagementService mockDataManagementService;

    @MockBean
    private TransientObjectDirectoryService mockTransientObjectDirectoryService;

    private MediaRequestEntity savedMediaRequestEntity;
    private HearingEntity hearingEntityWithMedia1;
    private HearingEntity hearingEntityWithoutMedia;
    private MediaEntity mediaEntity1;
    private MediaEntity mediaEntity2;
    private MediaEntity mediaEntity3;

    @Mock
    private TransientObjectDirectoryEntity mockTransientObjectDirectoryEntity;

    @ClassRule
    private static PostgreSQLContainer postgreSQLContainer = PostgresqlContainer.getInstance();

    @BeforeAll
    public static void postgresSetUp() {
        postgreSQLContainer.start();
    }

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

    private void createAndLoadMediaRequestEntity() {
        HearingEntity hearing = commonTestDataUtil.createHearing("test1", LocalTime.of(10, 0));
        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearingId(hearing.getId());
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
        var caseEntity = commonTestDataUtil.createCase("1");
        caseRepository.saveAndFlush(caseEntity);

        var courtroomEntity = commonTestDataUtil.createCourtroom("Int Test Courtroom");
        courtroomRepository.saveAndFlush(courtroomEntity);

        hearingEntityWithMedia1 = commonTestDataUtil.createHearing(caseEntity, courtroomEntity);
        var hearingEntityWithMedia2 = commonTestDataUtil.createHearing(caseEntity, courtroomEntity);
        hearingEntityWithoutMedia = commonTestDataUtil.createHearing(caseEntity, courtroomEntity);
        hearingRepository.saveAllAndFlush(Arrays.asList(
            hearingEntityWithMedia1,
            hearingEntityWithMedia2,
            hearingEntityWithoutMedia
        ));

        mediaEntity1 = commonTestDataUtil.createMedia();
        mediaEntity2 = commonTestDataUtil.createMedia();
        mediaEntity3 = commonTestDataUtil.createMedia();
        mediaRepository.saveAllAndFlush(Arrays.asList(
            mediaEntity1,
            mediaEntity2,
            mediaEntity3
        ));

        var hearingMediaEntity1 = commonTestDataUtil.createHearingMedia(
            hearingEntityWithMedia1,
            mediaEntity1
        );
        var hearingMediaEntity2 = commonTestDataUtil.createHearingMedia(
            hearingEntityWithMedia1,
            mediaEntity2
        );
        var hearingMediaEntity3 = commonTestDataUtil.createHearingMedia(
            hearingEntityWithMedia2,
            mediaEntity3
        );
        hearingMediaRepository.saveAllAndFlush(Arrays.asList(
            hearingMediaEntity1,
            hearingMediaEntity2,
            hearingMediaEntity3
        ));
    }

}
