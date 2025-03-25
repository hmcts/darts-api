package uk.gov.hmcts.darts.task.runner.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AudioLinkingAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AudioLinkingAutomatedTask
    extends AbstractLockableAutomatedTask<AudioLinkingAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final EventRepository eventRepository;
    private final EventProcessor eventProcessor;
    private final AudioConfigurationProperties audioConfigurationProperties;
    @SuppressWarnings("checkstyle:ImmutableField")
    private List<Integer> handheldCourtroomIds = new ArrayList<>();

    protected AudioLinkingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        AudioLinkingAutomatedTaskConfig automatedTaskConfigurationProperties,
                                        LogApi logApi, LockService lockService,
                                        EventRepository eventRepository,
                                        EventProcessor eventProcessor,
                                        AudioConfigurationProperties audioConfigurationProperties) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.eventRepository = eventRepository;
        this.eventProcessor = eventProcessor;
        this.audioConfigurationProperties = audioConfigurationProperties;
        this.handheldCourtroomIds = audioConfigurationProperties.getHandheldAudioCourtroomNumbers().stream().map(Integer::parseInt).toList();

    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.AUDIO_LINKING_TASK_NAME;
    }

    @Override
    protected void runTask() {
        log.info("Running AudioLinkingAutomatedTask");
        Integer batchSize = getAutomatedTaskBatchSize();
        List<Integer> eveIds = eventRepository.findAllByEventStatusAndNotCourtrooms(
            EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber(),
            handheldCourtroomIds,
            Limit.of(batchSize));

        log.info("Found {} events to process out of a total batch size {}", eveIds.size(), batchSize);
        eveIds.forEach(eventProcessor::processEvent);
    }


    @Service
    @RequiredArgsConstructor(onConstructor = @__(@Autowired))
    public static class EventProcessor {
        private final MediaRepository mediaRepository;
        private final EventService eventService;
        private final MediaLinkedCaseHelper mediaLinkedCaseHelper;

        @Getter
        @Value("${darts.automated.task.audio-linking.pre-amble-duration:0s}")
        private final Duration preAmbleDuration;
        @Getter
        @Value("${darts.automated.task.audio-linking.post-amble-duration:0s}")
        private final Duration postAmbleDuration;
        private final UserIdentity userIdentity;

        @Transactional
        public void processEvent(Integer eveId) {
            log.info("Attempting to link media for event with eveId {}", eveId);
            try {
                UserAccountEntity userAccount = userIdentity.getUserAccount();

                EventEntity event = eventService.getEventByEveId(eveId);
                List<MediaEntity> mediaEntities = mediaRepository.findAllByCurrentMediaTimeContains(
                    event.getCourtroom().getId(),
                    event.getTimestamp().plus(getPreAmbleDuration()),
                    event.getTimestamp().minus(getPostAmbleDuration()));
                mediaEntities.forEach(
                    mediaEntity -> mediaLinkedCaseHelper.linkMediaByEvent(event, mediaEntity, MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount));
                event.setEventStatus(EventStatus.AUDIO_LINKED.getStatusNumber());
                event.setLastModifiedBy(userIdentity.getUserAccount());
                eventService.saveEvent(event);
            } catch (Exception e) {
                log.error("Error attempting to link media for event with eveId {}", eveId, e);
            }
        }
    }
}