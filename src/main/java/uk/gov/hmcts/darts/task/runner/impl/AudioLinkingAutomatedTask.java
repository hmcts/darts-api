package uk.gov.hmcts.darts.task.runner.impl;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class AudioLinkingAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final EventRepository eventRepository;
    private final EventProcessor eventProcessor;

    protected AudioLinkingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
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
        List<Integer> eventIds = eventRepository.findAllByEventStatus(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber(),
                                                                      Limit.of(getAutomatedTaskBatchSize()));
        eventIds.forEach(eventProcessor::processEvent);
    }


    @Service
    @RequiredArgsConstructor(onConstructor = @__(@Autowired))
    public static class EventProcessor {
        private final MediaRepository mediaRepository;
        private final HearingRepository hearingRepository;
        private final MediaLinkedCaseRepository mediaLinkedCaseRepository;
        private final EventService eventService;
        private final UserAccountRepository userAccountRepository;

        @Getter
        @Value("${darts.automated-tasks.audio-linking.audio-buffer:0s}")
        private final Duration audioBuffer;


        @Transactional(value = Transactional.TxType.REQUIRES_NEW)
        void processEvent(Integer eventId) {
            try {
                EventEntity event = eventService.getEventByEveId(eventId);
                List<MediaEntity> mediaEntities = mediaRepository.findAllByMediaTimeContains(
                    event.getCourtroom().getId(),
                    event.getTimestamp().plus(getAudioBuffer()),
                    event.getTimestamp().minus(getAudioBuffer()));
                mediaEntities.forEach(mediaEntity -> processMedia(event.getHearingEntities(), mediaEntity));
                event.setEventStatus(EventStatus.AUDIO_LINKED.getStatusNumber());
                eventService.saveEvent(event);
            } catch (Exception e) {
                log.error("Error processing event {}", eventId, e);
            }
        }

        void processMedia(List<HearingEntity> hearingEntities, MediaEntity mediaEntity) {
            Set<HearingEntity> hearingsToSave = new HashSet<>();
            Set<MediaLinkedCaseEntity> mediaLinkedCaseEntities = new HashSet<>();
            UserAccountEntity userAccount = userAccountRepository.getReferenceById(SystemUsersEnum.AUDIO_LINKING_AUTOMATED_TASK.getId());
            hearingEntities.forEach(hearingEntity -> {
                try {
                    if (!hearingEntity.containsMedia(mediaEntity)) {
                        hearingEntity.addMedia(mediaEntity);
                        hearingsToSave.add(hearingEntity);
                        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
                        if (!mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCase)) {
                            mediaLinkedCaseEntities.add(new MediaLinkedCaseEntity(
                                mediaEntity, courtCase, userAccount, MediaLinkedCaseSourceType.AUDIO_LINKING_TASK));
                        }
                        log.info("Linking media {} to hearing {}", mediaEntity.getId(), hearingEntity.getId());
                    }
                } catch (Exception e) {
                    log.error("Error linking media {} to hearing {}", mediaEntity.getId(), hearingEntity.getId(), e);
                }
            });
            hearingRepository.saveAll(hearingsToSave);
            mediaLinkedCaseRepository.saveAll(mediaLinkedCaseEntities);
        }
    }
}