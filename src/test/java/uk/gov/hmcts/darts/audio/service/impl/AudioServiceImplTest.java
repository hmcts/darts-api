package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.impl.AddAudioRequestMapperImpl;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.sse.SentServerEventsHeartBeatEmitter;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioServiceImplTest {

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    public static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";

    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2023-01-01T12:00:00Z");
    private static final OffsetDateTime END_TIME = OffsetDateTime.parse("2023-01-01T13:00:00Z");

    @Captor
    ArgumentCaptor<MediaEntity> mediaEntityArgumentCaptor;
    @Captor
    ArgumentCaptor<BinaryData> inboundBlobStorageArgumentCaptor;
    @Mock
    SentServerEventsHeartBeatEmitter heartBeatEmitter;
    @Captor
    ArgumentCaptor<Throwable> exceptionCaptor;
    @Mock
    SseEmitter emitter;
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
    private AudioService audioService;

    @BeforeEach
    void setUp() {
        AddAudioRequestMapper mapper = new AddAudioRequestMapperImpl(retrieveCoreObjectService);
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
            audioConfigurationProperties, heartBeatEmitter
        );
    }

    @Test
    void previewShouldReturnExpectedData() throws IOException, ExecutionException, InterruptedException {

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        Path mediaPath = Path.of("/path/to/audio/sample2-5secs.mp2");
        when(mediaRepository.findById(1)).thenReturn(Optional.of(mediaEntity));
        when(audioTransformationService.saveMediaToWorkspace(mediaEntity)).thenReturn(mediaPath);

        AudioFileInfo audioFileInfo = AudioFileInfo.builder()
            .startTime(START_TIME.toInstant())
            .endTime(END_TIME.toInstant())
            .channel(1)
            .mediaFile("testAudio.mp2")
            .path(Path.of("test"))
            .build();
        when(audioOperationService.reEncode(anyString(), any())).thenReturn(audioFileInfo);

        byte[] testStringInBytes = DUMMY_FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        when(fileOperationService.convertFileToBinaryData(any())).thenReturn(data);

        try (InputStream inputStream = audioService.preview(mediaEntity.getId())) {
            byte[] bytes = inputStream.readAllBytes();
            assertEquals(DUMMY_FILE_CONTENT, new String(bytes));
        }
    }


    @SuppressWarnings("PMD.CloseResource")
    @Test
    void previewFluxShouldReturnError() throws IOException, ExecutionException, InterruptedException {

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        Path mediaPath = Path.of("/path/to/audio/sample2-5secs.mp2");
        when(mediaRepository.findById(1)).thenReturn(Optional.of(mediaEntity));
        when(audioTransformationService.saveMediaToWorkspace(mediaEntity)).thenReturn(mediaPath);

        AudioFileInfo audioFileInfo = AudioFileInfo.builder()
            .startTime(START_TIME.toInstant())
            .endTime(END_TIME.toInstant())
            .channel(1)
            .path(Path.of("test"))
            .build();
        when(audioOperationService.reEncode(anyString(), any())).thenReturn(audioFileInfo);

        BinaryData data = mock(BinaryData.class);
        InputStream inputStream = mock(InputStream.class);
        when(fileOperationService.convertFileToBinaryData(any())).thenReturn(data);
        when(data.toStream()).thenReturn(inputStream);
        when(inputStream.read(any())).thenThrow(new IOException());

        audioService.startStreamingPreview(
            mediaEntity.getId(),
            "bytes=0-1024", emitter
        );
        CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(invocationOnMock -> {
            Object result = invocationOnMock.callRealMethod();
            latch.countDown();
            return result;
        }).when(emitter).completeWithError(exceptionCaptor.capture());

        boolean result = latch.await(2, TimeUnit.SECONDS);
        if (result) {
            assertEquals("Failed to process audio request", exceptionCaptor.getValue().getMessage());
        } else {
            fail("Emitter did not complete with errors");
        }

    }

    @Test
    void previewShouldThrowExceptionWhenMediaIdCannotBeFound() {

        var mediaRequestId = 1;

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(mediaRequestId);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);
        mediaEntity.setChannel(1);

        when(mediaRepository.findById(mediaRequestId)).thenReturn(Optional.empty());

        var exception = assertThrows(
            DartsApiException.class,
            () -> audioService.preview(mediaRequestId)
        );

        assertEquals(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED, exception.getError());
    }


    @Test
    void addAudio() throws IOException {
        HearingEntity hearingEntity = new HearingEntity();
        when(retrieveCoreObjectService.retrieveOrCreateHearing(
            anyString(),
            anyString(),
            anyString(),
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

        audioService.addAudio(audioFile, addAudioMetadataRequest);

        verify(dataManagementApi).saveBlobDataToInboundContainer(inboundBlobStorageArgumentCaptor.capture());
        var binaryData = inboundBlobStorageArgumentCaptor.getValue();
        assertEquals(BinaryData.fromStream(audioFile.getInputStream()).toString(), binaryData.toString());


        verify(mediaRepository).save(mediaEntityArgumentCaptor.capture());
        verify(hearingRepository, times(3)).saveAndFlush(any());
        MediaEntity savedMedia = mediaEntityArgumentCaptor.getValue();
        assertEquals(startedAt, savedMedia.getStart());
        assertEquals(endedAt, savedMedia.getEnd());
        assertEquals(1, savedMedia.getChannel());
        assertEquals(2, savedMedia.getTotalChannels());
        assertEquals("SWANSEA", savedMedia.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals("1", savedMedia.getCourtroom().getName());
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
            any()
        )).thenReturn(hearing);
        audioService.linkAudioToHearingInMetadata(addAudioMetadataRequest, mediaEntity);
        verify(hearingRepository, times(3)).saveAndFlush(any());
        assertEquals(3, hearing.getMediaList().size());
    }
}
