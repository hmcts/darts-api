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
import uk.gov.hmcts.darts.audio.component.AudioMessageDigest;
import uk.gov.hmcts.darts.audio.component.impl.AddAudioRequestMapperImpl;
import uk.gov.hmcts.darts.audio.config.AudioConfiguration;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.AudioUploadService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.data.HearingTestData;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.exception.AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioUploadServiceImplTest {

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
    private AudioMessageDigest audioDigest;
    @Mock
    AudioBeingProcessedFromArchiveQuery audioBeingProcessedFromArchiveQuery;
    @Mock
    private LogApi logApi;
    @Mock
    private AuthorisationApi authorisationApi;
    private AudioUploadService audioService;
    @Mock
    private MediaLinkedCaseHelper mediaLinkedCaseHelper;
    @Mock
    private MediaLinkedCaseRepository mediaLinkedCaseRepository;

    @BeforeEach
    void setUp() {
        AddAudioRequestMapper mapper = new AddAudioRequestMapperImpl(retrieveCoreObjectService, userIdentity, mediaLinkedCaseHelper, mediaRepository);
        FileContentChecksum fileContentChecksum = new FileContentChecksum();
        audioService = new AudioUploadServiceImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            mediaRepository,
            mediaLinkedCaseRepository,
            retrieveCoreObjectService,
            hearingRepository,
            mapper,
            dataManagementApi,
            userIdentity,
            fileContentChecksum,
            courtLogEventRepository,
            audioConfigurationProperties,
            logApi,
            audioDigest,
            authorisationApi);
    }

    @Test
    void addAudio() {

        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance(AudioConfiguration.DEFAULT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
        }
        when(audioDigest.getMessageDigest()).thenReturn(digest);

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);

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
        when(mediaRepository.save(any(MediaEntity.class))).thenReturn(mediaEntity);

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
        verify(mediaRepository, times(2)).save(mediaEntityArgumentCaptor.capture());
        verify(hearingRepository, times(3)).saveAndFlush(any());
        verify(logApi, times(1)).audioUploaded(addAudioMetadataRequest);
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
        when(retrieveCoreObjectService.retrieveOrCreateCourtroom(eq("SWANSEA"), eq("1"), any(UserAccountEntity.class)))
            .thenReturn(courtroomEntity);

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);

        OffsetDateTime startedAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endedAt = OffsetDateTime.now();

        MediaEntity mediaEntity = createMediaEntity(startedAt, endedAt);
        mediaEntity.setId(10);
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

        HearingEntity hearing1 = HearingTestData.createSomeMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("1"),
            any(),
            any()
        )).thenReturn(hearing1);

        HearingEntity hearing2 = HearingTestData.createSomeMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("2"),
            any(),
            any()
        )).thenReturn(hearing2);

        HearingEntity hearing3 = HearingTestData.createSomeMinimalHearing();
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
    void linkAudioAndHearingDuplicateCases() {
        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, ENDED_AT);
        addAudioMetadataRequest.setCases(List.of("1", "2", "1"));
        MediaEntity mediaEntity = createMediaEntity(STARTED_AT, ENDED_AT);

        HearingEntity hearing1 = HearingTestData.createSomeMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("1"),
            any(),
            any()
        )).thenReturn(hearing1);

        HearingEntity hearing2 = HearingTestData.createSomeMinimalHearing();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            eq("2"),
            any(),
            any()
        )).thenReturn(hearing2);

        audioService.linkAudioToHearingInMetadata(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(2)).saveAndFlush(any());
        assertEquals(1, hearing1.getMediaList().size());
        assertEquals(1, hearing2.getMediaList().size());
    }
}