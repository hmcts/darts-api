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
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.MediaLinkedCaseHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AudioLinkingAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class AudioLinkingAutomatedTask 
    extends AbstractLockableAutomatedTask<AudioLinkingAutomatedTaskConfig>
    implements AutoloadingManualTask {

    private final EventRepository eventRepository;
    private final EventProcessor eventProcessor;

    protected AudioLinkingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        AudioLinkingAutomatedTaskConfig automatedTaskConfigurationProperties,
                                        LogApi logApi, LockService lockService,
                                        EventRepository eventRepository,
                                        EventProcessor eventProcessor) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.eventRepository = eventRepository;
        this.eventProcessor = eventProcessor;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.AUDIO_LINKING_TASK_NAME;
    }

    @Override
    protected void runTask() {
        log.info("Running AudioLinkingAutomatedTask");
        List<Integer> eveIds = eventRepository.findAllByEventStatus(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber(),
                                                                    Limit.of(getAutomatedTaskBatchSize()));
        eveIds.forEach(eventProcessor::processEvent);
    }


    @Service
    @RequiredArgsConstructor(onConstructor = @__(@Autowired))
    public static class EventProcessor {
        private final MediaRepository mediaRepository;
        private final EventService eventService;
        private final UserAccountRepository userAccountRepository;
        private final MediaLinkedCaseHelper mediaLinkedCaseHelper;

        @Getter
        @Value("${darts.automated-tasks.audio-linking.audio-buffer:0s}")
        private final Duration audioBuffer;


        @Transactional
        public void processEvent(Integer eveId) {
            log.info("Attempting to link media for event with eveId {}", eveId);
            try {
                UserAccountEntity userAccount = userAccountRepository.getReferenceById(SystemUsersEnum.AUDIO_LINKING_AUTOMATED_TASK.getId());

                EventEntity event = eventService.getEventByEveId(eveId);
                List<MediaEntity> mediaEntities = mediaRepository.findAllByMediaTimeContains(
                    event.getCourtroom().getId(),
                    event.getTimestamp().plus(getAudioBuffer()),
                    event.getTimestamp().minus(getAudioBuffer()));
                mediaEntities.forEach(mediaEntity -> {
                    mediaLinkedCaseHelper.linkMediaByEvent(event, mediaEntity, MediaLinkedCaseSourceType.AUDIO_LINKING_TASK, userAccount);
                });
                event.setEventStatus(EventStatus.AUDIO_LINKED.getStatusNumber());
                eventService.saveEvent(event);
            } catch (Exception e) {
                log.error("Error attempting to link media for event with eveId {}", eveId, e);
            }
        }
    }
}