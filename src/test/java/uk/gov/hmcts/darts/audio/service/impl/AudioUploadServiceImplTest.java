package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.impl.AddAudioRequestMapperImpl;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioUploadServiceImplTest {

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    public static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";
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
    private DataManagementApi dataManagementApi;
    @Mock
    private LogApi logApi;
    private AudioUploadServiceImpl audioService;
    @Mock
    private AudioAsyncService audioAsyncService;
    @Mock
    private MediaLinkedCaseHelper mediaLinkedCaseHelper;
    private AddAudioRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = spy(new AddAudioRequestMapperImpl(retrieveCoreObjectService, userIdentity, mediaLinkedCaseHelper, mediaRepository));
        FileContentChecksum fileContentChecksum = new FileContentChecksum();
        audioService = spy(new AudioUploadServiceImpl(
            externalObjectDirectoryRepository,
            externalLocationTypeRepository,
            mediaRepository,
            retrieveCoreObjectService,
            hearingRepository,
            mapper,
            dataManagementApi,
            userIdentity,
            logApi,
            audioAsyncService));
        ReflectionTestUtils.setField(audioService, "smallFileSizeMaxLength", Duration.ofSeconds(2));
        ReflectionTestUtils.setField(audioService, "smallFileSize", 1024);

        lenient().doAnswer(invocation -> invocation.getArgument(0)).when(mediaRepository).saveAndFlush(any());
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