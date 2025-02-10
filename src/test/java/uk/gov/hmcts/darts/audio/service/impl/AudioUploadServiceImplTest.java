package uk.gov.hmcts.darts.audio.service.impl;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.impl.AddAudioRequestMapperImpl;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioUploadServiceImplTest {

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    public static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";
    @Captor
    ArgumentCaptor<MediaEntity> mediaEntityArgumentCaptor;
    @Captor
    ArgumentCaptor<InputStream> inboundBlobStorageArgumentCaptor;
    @Captor
    ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityArgumentCaptor;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private LogApi logApi;
    private AudioUploadServiceImpl audioService;
    @Mock
    private AudioAsyncService audioAsyncService;
    @Mock
    private MediaLinkedCaseHelper mediaLinkedCaseHelper;
    @Mock
    private MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private AddAudioRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = spy(new AddAudioRequestMapperImpl(retrieveCoreObjectService, userIdentity, mediaLinkedCaseHelper, mediaRepository));
        FileContentChecksum fileContentChecksum = new FileContentChecksum();
        audioService = spy(new AudioUploadServiceImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            mediaRepository,
            retrieveCoreObjectService,
            hearingRepository,
            mapper,
            dataManagementApi,
            userIdentity,
            fileContentChecksum,
            logApi,
            mediaLinkedCaseRepository,
            audioAsyncService));
        ReflectionTestUtils.setField(audioService, "smallFileSizeMaxLength", Duration.ofSeconds(2));
        ReflectionTestUtils.setField(audioService, "smallFileSize", 1024);

        lenient().doAnswer(invocation -> invocation.getArgument(0)).when(mediaRepository).saveAndFlush(any());
    }

    @Test
    void addAudio_shouldUploadToInboundAndSave_whenFileAndMetadataUsed() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        // Given
        HearingEntity hearingEntity = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
            any(),
            any()
        )).thenReturn(hearingEntity);
        when(mediaRepository.findMediaByDetails(any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom(eq("SWANSEA"), eq("1"), any(UserAccountEntity.class)))
            .thenReturn(courtroomEntity);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        mediaEntity.setId(10);
        when(mediaRepository.saveAndFlush(any(MediaEntity.class))).thenReturn(mediaEntity);

        MockMultipartFile audioFile = new MockMultipartFile(
            "addAudio",
            "audio_sample.mp2",
            "audio/mpeg",
            DUMMY_FILE_CONTENT.getBytes()
        );
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);

        // When
        UUID externalLocation = UUID.randomUUID();
        when(dataManagementApi.saveBlobDataToInboundContainer(inboundBlobStorageArgumentCaptor.capture())).thenReturn(externalLocation);
        audioService.addAudio(audioFile, addAudioMetadataRequest);

        // Then
        verify(dataManagementApi).saveBlobDataToInboundContainer(inboundBlobStorageArgumentCaptor.capture());
        verify(mediaRepository).save(mediaEntityArgumentCaptor.capture());
        verify(hearingRepository).saveAndFlush(any());
        verify(logApi).audioUploaded(addAudioMetadataRequest);
        verify(externalObjectDirectoryRepository).save(externalObjectDirectoryEntityArgumentCaptor.capture());

        MediaEntity savedMedia = mediaEntityArgumentCaptor.getValue();
        assertEquals(startedAt, savedMedia.getStart());
        assertEquals(endedAt, savedMedia.getEnd());
        assertEquals(1, savedMedia.getChannel());
        assertEquals(2, savedMedia.getTotalChannels());
        assertEquals("SWANSEA", savedMedia.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals("1", savedMedia.getCourtroom().getName());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = externalObjectDirectoryEntityArgumentCaptor.getValue();
        assertEquals(savedMedia.getChecksum(), externalObjectDirectoryEntity.getChecksum());
        assertNotNull(externalObjectDirectoryEntity.getChecksum());
        assertEquals(externalLocation, externalObjectDirectoryEntity.getExternalLocation());
    }

    @Test
    void addAudio_shouldSaveMediaAndEod_whenMetadataOnly() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

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
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom(eq("SWANSEA"), eq("1"), any(UserAccountEntity.class)))
            .thenReturn(courtroomEntity);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        mediaEntity.setId(10);
        when(mediaRepository.saveAndFlush(any(MediaEntity.class))).thenReturn(mediaEntity);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);
        when(dataManagementApi.getChecksum(any(), any()))
            .thenReturn(addAudioMetadataRequest.getChecksum());

        // When
        UUID externalLocation = UUID.randomUUID();
        audioService.addAudio(externalLocation, addAudioMetadataRequest);

        // Then
        verify(mediaRepository).save(mediaEntityArgumentCaptor.capture());
        verify(hearingRepository).saveAndFlush(any());
        verify(logApi).audioUploaded(addAudioMetadataRequest);
        verify(externalObjectDirectoryRepository).save(externalObjectDirectoryEntityArgumentCaptor.capture());
        verify(dataManagementApi).getChecksum(DatastoreContainerType.INBOUND, externalLocation);
        MediaEntity savedMedia = mediaEntityArgumentCaptor.getValue();
        assertEquals(startedAt, savedMedia.getStart());
        assertEquals(endedAt, savedMedia.getEnd());
        assertEquals(1, savedMedia.getChannel());
        assertEquals(2, savedMedia.getTotalChannels());
        assertEquals("SWANSEA", savedMedia.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals("1", savedMedia.getCourtroom().getName());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = externalObjectDirectoryEntityArgumentCaptor.getValue();
        assertEquals(savedMedia.getChecksum(), externalObjectDirectoryEntity.getChecksum());
        assertNotNull(externalObjectDirectoryEntity.getChecksum());
        assertEquals(externalLocation, externalObjectDirectoryEntity.getExternalLocation());
    }

    @Test
    void addAudio_shouldDeleteInboundBlob_whenMetadataOnlyAndAudioIsADuplicate() throws AzureDeleteBlobException {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        // Given
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom(eq("SWANSEA"), eq("1"), any(UserAccountEntity.class)))
            .thenReturn(courtroomEntity);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        mediaEntity.setChecksum(addAudioMetadataRequest.getChecksum());
        mediaEntity.setId(10);

        when(mediaRepository.findMediaByDetails(any(), any(), any(), any(), any()))
            .thenReturn(List.of(mediaEntity));

        when(dataManagementApi.getChecksum(any(), any())).thenReturn(addAudioMetadataRequest.getChecksum());

        // When
        UUID externalLocation = UUID.randomUUID();
        audioService.addAudio(externalLocation, addAudioMetadataRequest);

        // Then
        verify(mediaRepository)
            .findMediaByDetails(courtroomEntity, mediaEntity.getChannel(), mediaEntity.getMediaFile(), startedAt, endedAt);
        verifyNoMoreInteractions(mediaRepository);
        verifyNoInteractions(hearingRepository);
        verifyNoInteractions(logApi);
        verifyNoInteractions(externalObjectDirectoryRepository);
        verify(dataManagementApi).getChecksum(DatastoreContainerType.INBOUND, externalLocation);
        verify(dataManagementApi).deleteBlobDataFromInboundContainer(externalLocation);
        verifyNoMoreInteractions(dataManagementApi);
    }

    @Test
    void addAudioNoUploadChecksumDoNotMatch() {
        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startedAt, endedAt);
        when(dataManagementApi.getChecksum(any(), any()))
            .thenReturn("123");

        // When
        UUID externalLocation = UUID.randomUUID();

        Assertions.assertThatThrownBy(() -> audioService.addAudio(externalLocation, addAudioMetadataRequest))
            .isInstanceOf(DartsApiException.class)
            .hasMessage("Failed to add audio meta data. Checksum for blob '123' does not match the one passed in the API request '123456'.")
            .hasFieldOrPropertyWithValue("error", AudioApiError.FAILED_TO_ADD_AUDIO_META_DATA);

        verify(dataManagementApi).getChecksum(DatastoreContainerType.INBOUND, externalLocation);

        // Then
        verifyNoInteractions(mediaRepository, hearingRepository, logApi, externalObjectDirectoryRepository);
    }

    private MediaEntity createMediaEntity(OffsetDateTime startedAt, OffsetDateTime endedAt) {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setStart(startedAt);
        mediaEntity.setEnd(endedAt);
        mediaEntity.setChannel(1);
        mediaEntity.setMediaFile("test");
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
        addAudioMetadataRequest.checksum("123456");
        addAudioMetadataRequest.setFileSize((long) DUMMY_FILE_CONTENT.length());
        return addAudioMetadataRequest;
    }

    @Test
    void linkAudioAndHearing() {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("1"),
            any(),
            any()
        )).thenReturn(hearing1);

        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("2"),
            any(),
            any()
        )).thenReturn(hearing2);

        HearingEntity hearing3 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("3"),
            any(),
            any()
        )).thenReturn(hearing3);
        audioService.linkAudioToHearingInMetadata(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(3)).saveAndFlush(any());
        assertEquals(1, hearing1.getMediaList().size());
        assertEquals(1, hearing2.getMediaList().size());
        assertEquals(1, hearing3.getMediaList().size());
    }

    @Test
    void linkAudioAndInactiveHearing() {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("1"),
            any(),
            any()
        )).thenReturn(hearing1);

        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearing2.setHearingIsActual(false);
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("2"),
            any(),
            any()
        )).thenReturn(hearing2);

        HearingEntity hearing3 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("3"),
            any(),
            any()
        )).thenReturn(hearing3);
        audioService.linkAudioToHearingInMetadata(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(3)).saveAndFlush(any());
        assertEquals(1, hearing1.getMediaList().size());
        assertEquals(1, hearing2.getMediaList().size());
        assertEquals(1, hearing3.getMediaList().size());
        assertTrue(hearing2.getHearingIsActual());
    }

    @Test
    void linkAudioAndHearingDuplicateCases() {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        addAudioMetadataRequest.setCases(List.of("1", "2", "1"));
        createMediaEntity(STARTED_AT, ENDED_AT);

        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("1"),
            any(),
            any()
        )).thenReturn(hearing1);

        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("2"),
            any(),
            any()
        )).thenReturn(hearing2);

        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");
        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom(eq("SWANSEA"), eq("1"), any(UserAccountEntity.class)))
            .thenReturn(courtroomEntity);

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);

        MockMultipartFile audioFile = new MockMultipartFile(
            "addAudio",
            "audio_sample.mp2",
            "audio/mpeg",
            DUMMY_FILE_CONTENT.getBytes()
        );

        audioService.addAudio(audioFile, addAudioMetadataRequest);
        verify(hearingRepository, times(2)).saveAndFlush(any());
        assertEquals(1, hearing1.getMediaList().size());
        assertEquals(1, hearing2.getMediaList().size());
    }

    @Test
    void versionUpload_shouldLogSmallFileWithLongDurationWarning_whenFileIs1024BytesAndDurationIsMoreThan2Seconds() {
        AddAudioMetadataRequest addAudioMetadataRequest = setupVersionUploadTest(3, 1024L);

        audioService.versionUpload(List.of(), addAudioMetadataRequest, UUID.randomUUID(), "123", mock(UserAccountEntity.class));

        verify(logApi).addAudioSmallFileWithLongDuration(
            "COURTHOUSE_123",
            "COURTROOM_123",
            addAudioMetadataRequest.getStartedAt(),
            addAudioMetadataRequest.getEndedAt(),
            123,
            1024L
        );
    }

    @Test
    void versionUpload_shouldNotLogSmallFileWithLongDurationWarning_whenFileIs1024BytesAndDurationIsEqualTo2Seconds() {
        AddAudioMetadataRequest addAudioMetadataRequest = setupVersionUploadTest(2, 1024L);

        audioService.versionUpload(List.of(), addAudioMetadataRequest, UUID.randomUUID(), "123", mock(UserAccountEntity.class));

        verify(logApi, never()).addAudioSmallFileWithLongDuration(any(), any(), any(), any(), any(), any());
    }

    @Test
    void versionUpload_shouldLogSmallFileWithLongDurationWarning_whenFileIs1025BytesAndDurationIsMoreThan2Seconds() {
        AddAudioMetadataRequest addAudioMetadataRequest = setupVersionUploadTest(3, 1025L);

        audioService.versionUpload(List.of(), addAudioMetadataRequest, UUID.randomUUID(), "123", mock(UserAccountEntity.class));

        verify(logApi, never()).addAudioSmallFileWithLongDuration(any(), any(), any(), any(), any(), any());
    }


    private AddAudioMetadataRequest setupVersionUploadTest(int endTimeOffset, long fileSize) {
        MediaEntity mediaEntity = mock(MediaEntity.class);
        when(mediaEntity.getId()).thenReturn(123);
        doReturn(mediaEntity).when(mapper).mapToMedia(any(), any());

        AddAudioMetadataRequest addAudioMetadataRequest = mock(AddAudioMetadataRequest.class);
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = startTime.plusSeconds(endTimeOffset);
        when(addAudioMetadataRequest.getStartedAt()).thenReturn(startTime);
        when(addAudioMetadataRequest.getEndedAt()).thenReturn(endTime);
        when(addAudioMetadataRequest.getFileSize()).thenReturn(fileSize);
        lenient().when(addAudioMetadataRequest.getCourthouse()).thenReturn("COURTHOUSE_123");
        lenient().when(addAudioMetadataRequest.getCourtroom()).thenReturn("COURTROOM_123");

        doNothing().when(audioService).linkAudioToHearingInMetadata(any(), any());
        doNothing().when(audioService).saveExternalObjectDirectory(any(), any(), any(), any());

        return addAudioMetadataRequest;
    }
}