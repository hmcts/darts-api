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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioLinkingAutomatedTaskTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private AudioLinkingAutomatedTask.EventProcessor eventProcessor;
    @Mock
    private UserIdentity userIdentity;

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

        @Mock
        private MediaLinkedCaseHelper mediaLinkedCaseHelper;

        @BeforeEach
        void beforeEach() {

            this.eventProcessor = spy(
                new AudioLinkingAutomatedTask.EventProcessor(
                    mediaRepository,
                    eventService, mediaLinkedCaseHelper,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(2),
                    userIdentity)
            );
            lenient().when(userIdentity.getUserAccount()).thenReturn(userAccount);
        }

        @Test
        void processEvent_shouldLinkAudio_whenUsingNoBufferTime() {
            doNothing().when(mediaLinkedCaseHelper)
                .linkMediaByEvent(any(), any(), any(), any());

            EventEntity event = mock(EventEntity.class);
            when(eventService.getEventByEveId(1)).thenReturn(event);
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

            verify(mediaLinkedCaseHelper, times(1))
                .linkMediaByEvent(event, mediaEntities.get(0), MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
            verify(mediaLinkedCaseHelper, times(1))
                .linkMediaByEvent(event, mediaEntities.get(1), MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
            verify(mediaLinkedCaseHelper, times(1))
                .linkMediaByEvent(event, mediaEntities.get(2), MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
            verify(mediaRepository, times(1))
                .findAllByMediaTimeContains(123,
                                            timestamp.minus(Duration.ofSeconds(1)),
                                            timestamp.plus(Duration.ofSeconds(2)));
            verify(event, times(1))
                .setEventStatus(3);

            verify(eventService, times(1))
                .saveEvent(event);
            verify(eventService, times(1))
                .getEventByEveId(1);
        }

        @Test
        void processEvent_shouldLinkAudio_accountingForBufferTime() {
            doNothing().when(mediaLinkedCaseHelper)
                .linkMediaByEvent(any(), any(), any(), any());
            doReturn(Duration.ofSeconds(10)).when(eventProcessor).getPreAmbleDuration();
            doReturn(Duration.ofSeconds(20)).when(eventProcessor).getPostAmbleDuration();
            EventEntity event = mock(EventEntity.class);
            when(eventService.getEventByEveId(2)).thenReturn(event);
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

            verify(mediaLinkedCaseHelper, times(1))
                .linkMediaByEvent(event, mediaEntities.get(0), MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
            verify(mediaLinkedCaseHelper, times(1))
                .linkMediaByEvent(event, mediaEntities.get(1), MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
            verify(mediaLinkedCaseHelper, times(1))
                .linkMediaByEvent(event, mediaEntities.get(2), MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
            verify(mediaRepository, times(1))
                .findAllByMediaTimeContains(123, timestamp.minus(Duration.ofSeconds(10)), timestamp.plus(Duration.ofSeconds(20)));
            verify(event, times(1))
                .setEventStatus(3);
            verify(eventService, times(1))
                .getEventByEveId(2);
        }

    }
}