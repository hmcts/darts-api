package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioAsyncServiceImplTest {

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.now().minusHours(1);
    public static final OffsetDateTime ENDED_AT = OffsetDateTime.now();
    private static final String DUMMY_FILE_CONTENT = "DUMMY FILE CONTENT";
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private CourtLogEventRepository courtLogEventRepository;
    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;
    @Mock
    private MediaLinkedCaseHelper mediaLinkedCaseHelper;

    private AudioAsyncService audioAsyncService;

    @Mock
    private UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        audioAsyncService = new AudioAsyncServiceImpl(
            audioConfigurationProperties,
            courtLogEventRepository,
            hearingRepository,
            mediaLinkedCaseHelper);
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

        audioAsyncService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity, userAccount);
        verify(hearingRepository, times(0)).saveAndFlush(any());
        assertEquals(0, hearing.getMediaList().size());
    }

    @Test
    void linkAudioToHearingByEvent() {
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);
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

        audioAsyncService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity, userAccount);
        verify(hearingRepository, times(1)).saveAndFlush(any());
        assertEquals(1, hearing.getMediaList().size());
        verify(mediaLinkedCaseHelper).addCase(mediaEntity, courtCase, MediaLinkedCaseSourceType.ADD_AUDIO_EVENT_LINKING, userAccount);
    }

    @Test
    void linkAudioToInactiveHearingByEvent() {
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

        audioAsyncService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity, userAccount);
        verify(hearingRepository, times(1)).saveAndFlush(any());
        assertEquals(1, hearing.getMediaList().size());
        assertTrue(hearing.getHearingIsActual());
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

        audioAsyncService.linkAudioToHearingByEvent(addAudioMetadataRequest, mediaEntity, userAccount);
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
}