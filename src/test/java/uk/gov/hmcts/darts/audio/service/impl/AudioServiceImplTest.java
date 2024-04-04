package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.AudioBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.component.impl.AddAudioRequestMapperImpl;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioServiceImplTest {

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    public static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";
    private static final String DUMMY_FILE_CONTENT_EMPTY = "";

    @Captor
    ArgumentCaptor<MediaEntity> mediaEntityArgumentCaptor;
    @Captor
    ArgumentCaptor<InputStream> inboundBlobStorageArgumentCaptor;
    @Captor
    ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityArgumentCaptor;
    @Mock
    private AudioTransformationService audioTransformationService;
    @Mock
    private AudioOperationService audioOperationService;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CourtLogEventRepository courtLogEventRepository;
    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    AudioBeingProcessedFromArchiveQuery audioBeingProcessedFromArchiveQuery;
    @Mock
    private LogApi logApi;
    private AudioService audioService;

    @BeforeEach
    void setUp() {
        AddAudioRequestMapper mapper = new AddAudioRequestMapperImpl(retrieveCoreObjectService, userIdentity);
        FileContentChecksum fileContentChecksum = new FileContentChecksum();
        audioService = new AudioServiceImpl(
            audioTransformationService,
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            mediaRepository,
            audioOperationService,
            fileOperationService,
            retrieveCoreObjectService,
            hearingRepository,
            mapper,
            dataManagementApi,
            userIdentity,
            fileContentChecksum,
            courtLogEventRepository,
            audioConfigurationProperties,
            audioBeingProcessedFromArchiveQuery,
            logApi
        );
    }

    @Test
    void addAudio() {
        // Given
        HearingEntity hearingEntity = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(hearingEntity);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom("SWANSEA", "1"))
            .thenReturn(courtroomEntity);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        when(mediaRepository.save(any(MediaEntity.class))).thenReturn(mediaEntity);

        MockMultipartFile audioFile = new MockMultipartFile(
            "addAudio",
            "audio_sample.mp2",
            "audio/mpeg",
            DUMMY_FILE_CONTENT.getBytes()
        );
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);

        // When
        audioService.addAudio(audioFile, addAudioMetadataRequest);

        // Then
        verify(dataManagementApi).saveBlobDataToInboundContainer(inboundBlobStorageArgumentCaptor.capture());
        verify(mediaRepository).save(mediaEntityArgumentCaptor.capture());
        verify(hearingRepository, times(3)).saveAndFlush(any());
        verify(logApi, times(1)).audioUploaded(addAudioMetadataRequest);

        MediaEntity savedMedia = mediaEntityArgumentCaptor.getValue();
        assertEquals(startedAt, savedMedia.getStart());
        assertEquals(endedAt, savedMedia.getEnd());
        assertEquals(1, savedMedia.getChannel());
        assertEquals(2, savedMedia.getTotalChannels());
        assertEquals("SWANSEA", savedMedia.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals("1", savedMedia.getCourtroom().getName());
    }

    @Test
    void addAudioWillNotSaveBlobToDataStoreWhenAudioFileIsEmpty() {
        // Given
        HearingEntity hearingEntity = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(hearingEntity);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom("SWANSEA", "1"))
            .thenReturn(courtroomEntity);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        when(mediaRepository.save(any(MediaEntity.class))).thenReturn(mediaEntity);

        ObjectRecordStatusEntity failedStatus = new ObjectRecordStatusEntity();
        failedStatus.setId(4);
        when(objectRecordStatusRepository.getReferenceById(any())).thenReturn(failedStatus);

        MockMultipartFile audioFile = new MockMultipartFile(
            "addAudio",
            "audio_sample.mp2",
            "audio/mpeg",
            DUMMY_FILE_CONTENT_EMPTY.getBytes()
        );
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);

        // When
        audioService.addAudio(audioFile, addAudioMetadataRequest);

        // Then
        verify(externalObjectDirectoryRepository).save(externalObjectDirectoryEntityArgumentCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = externalObjectDirectoryEntityArgumentCaptor.getValue();
        assertEquals(null, externalObjectDirectoryEntity.getExternalLocation());
        assertEquals(null, externalObjectDirectoryEntity.getChecksum());
        assertEquals(FAILURE_FILE_NOT_FOUND.getId(), externalObjectDirectoryEntity.getStatus().getId());
    }

    @Test
    void addAudioWillNotSaveBlobToDataStoreWhenAudioFileSizeMismatch() {
        // Given
        HearingEntity hearingEntity = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(hearingEntity);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom("SWANSEA", "1"))
            .thenReturn(courtroomEntity);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        when(mediaRepository.save(any(MediaEntity.class))).thenReturn(mediaEntity);

        ObjectRecordStatusEntity failedStatus = new ObjectRecordStatusEntity();
        failedStatus.setId(5);
        when(objectRecordStatusRepository.getReferenceById(any())).thenReturn(failedStatus);

        MockMultipartFile audioFile = new MockMultipartFile(
            "addAudio",
            "audio_sample.mp2",
            "audio/mpeg",
            DUMMY_FILE_CONTENT.getBytes()
        );
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);
        addAudioMetadataRequest.setFileSize((long) (DUMMY_FILE_CONTENT.length() * 2));
        // When
        audioService.addAudio(audioFile, addAudioMetadataRequest);

        // Then
        verify(externalObjectDirectoryRepository).save(externalObjectDirectoryEntityArgumentCaptor.capture());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = externalObjectDirectoryEntityArgumentCaptor.getValue();
        assertEquals(null, externalObjectDirectoryEntity.getExternalLocation());
        assertEquals(null, externalObjectDirectoryEntity.getChecksum());
        assertEquals(FAILURE_FILE_SIZE_CHECK_FAILED.getId(), externalObjectDirectoryEntity.getStatus().getId());
    }

    @Test
    void handheldAudioShouldNotLinkAudioToHearingByEvent() {

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        addAudioMetadataRequest.setTotalChannels(1);

        HearingEntity hearing = new HearingEntity();
        EventEntity eventEntity = new EventEntity();
        eventEntity.addHearing(hearing);

        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        when(audioConfigurationProperties.getHandheldAudioCourtroomNumbers())
            .thenReturn(List.of(addAudioMetadataRequest.getCourtroom()));

        audioService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(0)).saveAndFlush(any());
        assertEquals(0, hearing.getMediaList().size());
    }

    @Test
    void linkAudioToHearingByEvent() {
        HearingEntity hearing = new HearingEntity();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setTimestamp(STARTED_AT.minusMinutes(30));
        eventEntity.addHearing(hearing);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        when(courtLogEventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(List.of(eventEntity));

        audioService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(1)).saveAndFlush(any());
        assertEquals(1, hearing.getMediaList().size());
    }

    @Test
    void linkAudioToHearingByEventShouldOnlyLinkOncePerHearing() {
        HearingEntity hearing = new HearingEntity();
        EventEntity firstEventEntity = new EventEntity();
        firstEventEntity.setTimestamp(STARTED_AT.plusMinutes(15));
        firstEventEntity.addHearing(hearing);

        EventEntity secondEventEntity = new EventEntity();
        secondEventEntity.setTimestamp(STARTED_AT.plusMinutes(20));
        secondEventEntity.addHearing(hearing);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        when(courtLogEventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(Arrays.asList(firstEventEntity, secondEventEntity));

        audioService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(1)).saveAndFlush(any());
        assertEquals(1, hearing.getMediaList().size());
    }

    private MediaEntity createMediaEntity(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setStart(startedAt);
        mediaEntity.setEnd(endedAt);
        mediaEntity.setChannel(1);
        mediaEntity.setTotalChannels(2);
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        mediaEntity.setCourtroom(new CourtroomEntity(1, "1", courthouse));
        return mediaEntity;
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format("mp3");
        addAudioMetadataRequest.filename("test");
        addAudioMetadataRequest.courthouse("SWANSEA");
        addAudioMetadataRequest.courtroom("1");
        addAudioMetadataRequest.cases(List.of("1", "2", "3"));
        addAudioMetadataRequest.setFileSize((long) DUMMY_FILE_CONTENT.length());
        return addAudioMetadataRequest;
    }

    @Test
    void linkAudioAndHearing() {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        HearingEntity hearing = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(hearing);
        audioService.linkAudioToHearingInMetadata(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(3)).saveAndFlush(any());
        assertEquals(3, hearing.getMediaList().size());
    }

    @Test
    void whenAudioMetadataListContainsMediaIdsReturnedByQuery_thenIsArchivedWillBeTrue() {
        int mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);
        AudioBeingProcessedFromArchiveQueryResult audioRequest = new AudioBeingProcessedFromArchiveQueryResult(mediaId, 2, 3);
        List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords = List.of(audioRequest);

        when(audioBeingProcessedFromArchiveQuery.getResults(any())).thenReturn(archivedArmRecords);

        audioService.setIsArchived(audioMetadataList, 1);

        assertEquals(true, audioMetadataList.get(0).getIsArchived());
    }

    @Test
    void whenAudioMetadataListOmitsMediaIdsReturnedByQuery_thenIsArchivedWillBeFalse() {
        int mediaId = 1;
        AudioMetadata audioMetadata = new AudioMetadata();
        audioMetadata.setId(mediaId);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata);

        audioService.setIsArchived(audioMetadataList, 1);

        assertEquals(false, audioMetadataList.get(0).getIsArchived());
    }

    @Test
    void whenAudioMetadataListContainsMediaIdsStoredInUnstructured_thenIsAvailableWillBeTrue() {
        AudioMetadata audioMetadata1 = new AudioMetadata();
        audioMetadata1.setId(1);
        AudioMetadata audioMetadata2 = new AudioMetadata();
        audioMetadata2.setId(2);
        AudioMetadata audioMetadata3 = new AudioMetadata();
        audioMetadata3.setId(3);
        List<AudioMetadata> audioMetadataList = List.of(audioMetadata1, audioMetadata2, audioMetadata3);

        when(externalObjectDirectoryRepository.findMediaIdsByInMediaIdStatusAndType(anyList(), any(), any())).thenReturn(List.of(1, 3));

        audioService.setIsAvailable(audioMetadataList);

        assertEquals(true, audioMetadataList.get(0).getIsAvailable());
        assertEquals(false, audioMetadataList.get(1).getIsAvailable());
        assertEquals(true, audioMetadataList.get(2).getIsAvailable());
    }
}
