package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType.AUDIO_LINKING_TASK;

@ExtendWith(MockitoExtension.class)
class AudioLinkingAutomatedTaskTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private AudioLinkingAutomatedTask.EventProcessor eventProcessor;
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    @Spy
    private AudioLinkingAutomatedTask audioLinkingAutomatedTask;

    @Mock
    UserAccountEntity userAccount;

    @Test
    void positiveGetAutomatedTaskName() {
        assertThat(audioLinkingAutomatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.AUDIO_LINKING_TASK_NAME);
    }

    @Test
    void positiveRunTask() {

        List<Integer> eventIds = List.of(1, 2, 3);

        doReturn(eventIds).when(eventRepository).findAllByEventStatus(anyInt(), any());
        doNothing().when(eventProcessor).processEvent(any());
        doReturn(5).when(audioLinkingAutomatedTask).getAutomatedTaskBatchSize();

        audioLinkingAutomatedTask.runTask();


        verify(audioLinkingAutomatedTask, times(1))
            .getAutomatedTaskBatchSize();
        verify(eventRepository, times(1))
            .findAllByEventStatus(2, Limit.of(5));

        verify(eventProcessor, times(1))
            .processEvent(1);
        verify(eventProcessor, times(1))
            .processEvent(2);
        verify(eventProcessor, times(1))
            .processEvent(3);
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class EventProcessorTest {
        @Mock
        private MediaRepository mediaRepository;
        @Mock
        private HearingRepository hearingRepository;
        @Mock
        private MediaLinkedCaseRepository mediaLinkedCaseRepository;
        @Mock
        private EventService eventService;

        private AudioLinkingAutomatedTask.EventProcessor eventProcessor;

        @BeforeEach
        void beforeEach() {
            this.eventProcessor = spy(
                new AudioLinkingAutomatedTask.EventProcessor(
                    mediaRepository, hearingRepository, mediaLinkedCaseRepository,
                    eventService, userAccountRepository, Duration.ofSeconds(0)
                )
            );
            lenient().when(userAccountRepository.getReferenceById(SystemUsersEnum.AUDIO_LINKING_AUTOMATED_TASK.getId())).thenReturn(userAccount);
        }

        @Test
        void positiveProcessEvent() {
            doNothing().when(eventProcessor)
                .processMedia(any(), any(), any());

            EventEntity event = mock(EventEntity.class);
            when(eventService.getEventByEveId(1)).thenReturn(event);
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

            eventProcessor.processEvent(1);

            verify(eventProcessor, times(1))
                .processMedia(hearingEntities, mediaEntities.get(0), 1);
            verify(eventProcessor, times(1))
                .processMedia(hearingEntities, mediaEntities.get(1), 1);
            verify(eventProcessor, times(1))
                .processMedia(hearingEntities, mediaEntities.get(2), 1);
            verify(mediaRepository, times(1))
                .findAllByMediaTimeContains(123, timestamp, timestamp);
            verify(event, times(1))
                .setEventStatus(3);

            verify(eventService, times(1))
                .saveEvent(event);
            verify(eventService, times(1))
                .getEventByEveId(1);
        }

        @Test
        void positiveProcessEventWithBuffer() {
            doNothing().when(eventProcessor)
                .processMedia(any(), any(), any());
            doReturn(Duration.ofSeconds(10)).when(eventProcessor).getAudioBuffer();
            EventEntity event = mock(EventEntity.class);
            when(eventService.getEventByEveId(2)).thenReturn(event);
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

            eventProcessor.processEvent(2);


            verify(eventProcessor, times(1))
                .processMedia(hearingEntities, mediaEntities.get(0), 2);
            verify(eventProcessor, times(1))
                .processMedia(hearingEntities, mediaEntities.get(1), 2);
            verify(eventProcessor, times(1))
                .processMedia(hearingEntities, mediaEntities.get(2), 2);
            verify(mediaRepository, times(1))
                .findAllByMediaTimeContains(123, timestamp.plusSeconds(10), timestamp.minusSeconds(10));
            verify(event, times(1))
                .setEventStatus(3);
            verify(eventService, times(1))
                .getEventByEveId(2);
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
            eventProcessor.processMedia(hearingEntities, mediaEntity, 1);


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

            Set<HearingEntity> savedHearingEntities = new HashSet<>();
            savedHearingEntities.add(hearingEntity1);
            savedHearingEntities.add(hearingEntity2);
            savedHearingEntities.add(hearingEntity3);
            verify(hearingRepository, times(1))
                .saveAll(savedHearingEntities);

            Set<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
            savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1, userAccount, AUDIO_LINKING_TASK));
            savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity2, userAccount, AUDIO_LINKING_TASK));
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

            eventProcessor.processMedia(hearingEntities, mediaEntity, 1);

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

            Set<HearingEntity> savedHearingEntities = new HashSet<>();
            savedHearingEntities.add(hearingEntity1);
            verify(hearingRepository, times(1))
                .saveAll(savedHearingEntities);

            Set<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
            savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1, userAccount, AUDIO_LINKING_TASK));
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

            when(mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCaseEntity1))
                .thenReturn(false);

            when(mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCaseEntity2))
                .thenReturn(true);


            eventProcessor.processMedia(hearingEntities, mediaEntity, 1);


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


            Set<HearingEntity> savedHearingEntities = new HashSet<>();
            savedHearingEntities.add(hearingEntity1);
            savedHearingEntities.add(hearingEntity2);
            savedHearingEntities.add(hearingEntity3);
            verify(hearingRepository, times(1))
                .saveAll(savedHearingEntities);

            Set<MediaLinkedCaseEntity> savedMediaLinkedCaseEntity = new HashSet<>();
            savedMediaLinkedCaseEntity.add(new MediaLinkedCaseEntity(mediaEntity, courtCaseEntity1, userAccount, AUDIO_LINKING_TASK));
            verify(mediaLinkedCaseRepository, times(1))
                .saveAll(savedMediaLinkedCaseEntity);
        }
    }
}