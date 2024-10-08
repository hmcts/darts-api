package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioLinkingAutomatedTaskTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private MediaLinkedCaseRepository mediaLinkedCaseRepository;
    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private AudioLinkingAutomatedTask audioLinkingAutomatedTask;


    @BeforeEach
    void beforeEach() {
        this.audioLinkingAutomatedTask = spy(
            new AudioLinkingAutomatedTask(
                null, null, null, null,
                eventRepository, mediaRepository, mediaLinkedCaseRepository,
                0, hearingRepository
            )
        );
    }

    @Test
    void positiveGetAutomatedTaskName() {
        assertThat(audioLinkingAutomatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.AUDIO_LINKING_TASK_NAME);
    }

    @Test
    void positiveRunTask() {

        List<EventEntity> events = List.of(mock(EventEntity.class), mock(EventEntity.class), mock(EventEntity.class));

        doReturn(events).when(eventRepository).findAllByEventStatus(anyInt(), any());
        doNothing().when(audioLinkingAutomatedTask).processEvent(any());
        doReturn(5).when(audioLinkingAutomatedTask).getAutomatedTaskBatchSize();

        audioLinkingAutomatedTask.runTask();


        verify(audioLinkingAutomatedTask, times(1))
            .getAutomatedTaskBatchSize();
        verify(eventRepository, times(1))
            .findAllByEventStatus(2, Limit.of(5));

        verify(audioLinkingAutomatedTask, times(1))
            .processEvent(events.get(0));
        verify(audioLinkingAutomatedTask, times(1))
            .processEvent(events.get(1));
        verify(audioLinkingAutomatedTask, times(1))
            .processEvent(events.get(2));
    }

    @Test
    void positiveProcessEvent() {
        doNothing().when(audioLinkingAutomatedTask)
            .processMedia(any(), any());

        EventEntity event = mock(EventEntity.class);
        List<HearingEntity> hearingEntities = List.of(mock(HearingEntity.class), mock(HearingEntity.class));
        when(event.getHearingEntities()).thenReturn(hearingEntities);
        OffsetDateTime timestamp = OffsetDateTime.now();
        when(event.getTimestamp()).thenReturn(timestamp);
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        when(courtroomEntity.getId()).thenReturn(123);
        when(event.getCourtroom()).thenReturn(courtroomEntity);


        List<MediaEntity> mediaEntities = List.of(
            mock(MediaEntity.class), mock(MediaEntity.class), mock(MediaEntity.class));
        doReturn(mediaEntities).when(mediaRepository)
            .findAllByMediaTimeContains(any(), any(), any());

        audioLinkingAutomatedTask.processEvent(event);

        verify(audioLinkingAutomatedTask, times(1))
            .processMedia(hearingEntities, mediaEntities.get(0));
        verify(audioLinkingAutomatedTask, times(1))
            .processMedia(hearingEntities, mediaEntities.get(1));
        verify(audioLinkingAutomatedTask, times(1))
            .processMedia(hearingEntities, mediaEntities.get(2));
        verify(mediaRepository, times(1))
            .findAllByMediaTimeContains(123, timestamp, timestamp);
        verify(event, times(1))
            .setEventStatus(3);

        verify(eventRepository, times(1))
            .save(event);
    }

    @Test
    void positiveProcessEventWithBuffer() {
        doNothing().when(audioLinkingAutomatedTask)
            .processMedia(any(), any());
        doReturn(10).when(audioLinkingAutomatedTask).getAudioBufferSeconds();
        EventEntity event = mock(EventEntity.class);
        List<HearingEntity> hearingEntities = List.of(mock(HearingEntity.class), mock(HearingEntity.class));
        when(event.getHearingEntities()).thenReturn(hearingEntities);
        OffsetDateTime timestamp = OffsetDateTime.now();
        when(event.getTimestamp()).thenReturn(timestamp);
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        when(courtroomEntity.getId()).thenReturn(123);
        when(event.getCourtroom()).thenReturn(courtroomEntity);

        List<MediaEntity> mediaEntities = List.of(
            mock(MediaEntity.class), mock(MediaEntity.class), mock(MediaEntity.class));
        doReturn(mediaEntities).when(mediaRepository)
            .findAllByMediaTimeContains(any(), any(), any());

        audioLinkingAutomatedTask.processEvent(event);


        verify(audioLinkingAutomatedTask, times(1))
            .processMedia(hearingEntities, mediaEntities.get(0));
        verify(audioLinkingAutomatedTask, times(1))
            .processMedia(hearingEntities, mediaEntities.get(1));
        verify(audioLinkingAutomatedTask, times(1))
            .processMedia(hearingEntities, mediaEntities.get(2));
        verify(mediaRepository, times(1))
            .findAllByMediaTimeContains(123, timestamp.plusSeconds(10), timestamp.minusSeconds(10));
        verify(event, times(1))
            .setEventStatus(3);

    }

    @Test
    void positiveProcessMedia() {
        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);


        when(hearingEntity1.containsMedia(any())).thenReturn(false);
        when(hearingEntity2.containsMedia(any())).thenReturn(false);
        when(hearingEntity3.containsMedia(any())).thenReturn(false);

        when(mediaLinkedCaseRepository.existsByMediaAndCourtCase(any(), any()))
            .thenReturn(false);


        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity2 = mock(CourtCaseEntity.class);

        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity2);


        List<HearingEntity> hearingEntities = List.of(hearingEntity1, hearingEntity2, hearingEntity3);

        MediaEntity mediaEntity = mock(MediaEntity.class);
        Set<MediaLinkedCaseEntity> mediaLinkedCaseEntities = new HashSet<>();
        Set<HearingEntity> editedHearingEntities = new HashSet<>();

        audioLinkingAutomatedTask.processMedia(hearingEntities, mediaEntity);


        verify(hearingEntity1, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity2, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity3, times(1)).containsMedia(mediaEntity);

        verify(hearingEntity1, times(1)).addMedia(mediaEntity);
        verify(hearingEntity2, times(1)).addMedia(mediaEntity);
        verify(hearingEntity3, times(1)).addMedia(mediaEntity);

        verify(hearingEntity1, times(1)).getCourtCase();
        verify(hearingEntity2, times(1)).getCourtCase();
        verify(hearingEntity3, times(1)).getCourtCase();

        verify(mediaLinkedCaseRepository, times(2)).existsByMediaAndCourtCase(mediaEntity, courtCaseEntity1);
        verify(mediaLinkedCaseRepository, times(1)).existsByMediaAndCourtCase(mediaEntity, courtCaseEntity2);

        HashSet<HearingEntity> savedHearingEntities = new HashSet<>();
        savedHearingEntities.add(hearingEntity1);
        savedHearingEntities.add(hearingEntity2);
        savedHearingEntities.add(hearingEntity3);
        verify(hearingRepository, times(1))
            .saveAll(savedHearingEntities);

        HashSet<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
        savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1));
        savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity2));
        verify(mediaLinkedCaseRepository, times(1))
            .saveAll(savedMediaLinkedCaseEntity);

    }

    @Test
    void positiveProcessMediaHearingAlreadyContainsMedia() {
        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);


        when(hearingEntity1.containsMedia(any())).thenReturn(false);
        when(hearingEntity2.containsMedia(any())).thenReturn(true);
        when(hearingEntity3.containsMedia(any())).thenReturn(true);

        when(mediaLinkedCaseRepository.existsByMediaAndCourtCase(any(), any()))
            .thenReturn(false);

        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);

        List<HearingEntity> hearingEntities = List.of(hearingEntity1, hearingEntity2, hearingEntity3);

        MediaEntity mediaEntity = mock(MediaEntity.class);

        audioLinkingAutomatedTask.processMedia(hearingEntities, mediaEntity);

        verify(hearingEntity1, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity2, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity3, times(1)).containsMedia(mediaEntity);

        verify(hearingEntity1, times(1)).addMedia(mediaEntity);
        verify(hearingEntity2, never()).addMedia(mediaEntity);
        verify(hearingEntity3, never()).addMedia(mediaEntity);

        verify(hearingEntity1, times(1)).getCourtCase();
        verify(hearingEntity2, never()).getCourtCase();
        verify(hearingEntity3, never()).getCourtCase();

        verify(mediaLinkedCaseRepository, times(1)).existsByMediaAndCourtCase(mediaEntity, courtCaseEntity1);

        HashSet<HearingEntity> savedHearingEntities = new HashSet<>();
        savedHearingEntities.add(hearingEntity1);
        verify(hearingRepository, times(1))
            .saveAll(savedHearingEntities);

        HashSet<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
        savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1));
        verify(mediaLinkedCaseRepository, times(1))
            .saveAll(savedMediaLinkedCaseEntity);
    }

    @Test
    void positiveProcessMediaHearingDoesNotContainsMediaButMediaLinkedCaseExists() {
        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);


        when(hearingEntity1.containsMedia(any())).thenReturn(false);
        when(hearingEntity2.containsMedia(any())).thenReturn(false);
        when(hearingEntity3.containsMedia(any())).thenReturn(false);


        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity2 = mock(CourtCaseEntity.class);

        when(hearingEntity1.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity2.getCourtCase()).thenReturn(courtCaseEntity1);
        when(hearingEntity3.getCourtCase()).thenReturn(courtCaseEntity2);


        List<HearingEntity> hearingEntities = List.of(hearingEntity1, hearingEntity2, hearingEntity3);

        MediaEntity mediaEntity = mock(MediaEntity.class);
        Set<MediaLinkedCaseEntity> mediaLinkedCaseEntities = new HashSet<>();
        Set<HearingEntity> editedHearingEntities = new HashSet<>();

        when(mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCaseEntity1))
            .thenReturn(false);

        when(mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCaseEntity2))
            .thenReturn(true);


        audioLinkingAutomatedTask.processMedia(hearingEntities, mediaEntity);


        verify(hearingEntity1, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity2, times(1)).containsMedia(mediaEntity);
        verify(hearingEntity3, times(1)).containsMedia(mediaEntity);

        verify(hearingEntity1, times(1)).addMedia(mediaEntity);
        verify(hearingEntity2, times(1)).addMedia(mediaEntity);
        verify(hearingEntity3, times(1)).addMedia(mediaEntity);

        verify(hearingEntity1, times(1)).getCourtCase();
        verify(hearingEntity2, times(1)).getCourtCase();
        verify(hearingEntity3, times(1)).getCourtCase();

        verify(mediaLinkedCaseRepository, times(2)).existsByMediaAndCourtCase(mediaEntity, courtCaseEntity1);
        verify(mediaLinkedCaseRepository, times(1)).existsByMediaAndCourtCase(mediaEntity, courtCaseEntity2);


        HashSet<HearingEntity> savedHearingEntities = new HashSet<>();
        savedHearingEntities.add(hearingEntity1);
        savedHearingEntities.add(hearingEntity2);
        savedHearingEntities.add(hearingEntity3);
        verify(hearingRepository, times(1))
            .saveAll(savedHearingEntities);

        HashSet<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
        savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1));
        verify(mediaLinkedCaseRepository, times(1))
            .saveAll(savedMediaLinkedCaseEntity);
    }
}
