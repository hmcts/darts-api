package uk.gov.hmcts.darts.task.runner.impl;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.event.enums.EventStatus;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class AudioLinkingAutomatedTask extends AbstractLockableAutomatedTask
    implements AutoloadingManualTask {

    private final EventRepository eventRepository;
    private final MediaRepository mediaRepository;
    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;

    @Getter
    private final Integer audioBufferSeconds;
    private final HearingRepository hearingRepository;

    protected AudioLinkingAutomatedTask(AutomatedTaskRepository automatedTaskRepository,
                                        AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties,
                                        LogApi logApi, LockService lockService,
                                        EventRepository eventRepository,
                                        MediaRepository mediaRepository, MediaLinkedCaseRepository mediaLinkedCaseRepository,
                                        @Value("${darts.automated-tasks.audio-linking.audio-buffer-seconds:0}")
                                        Integer audioBufferSeconds, HearingRepository hearingRepository) {
        super(automatedTaskRepository, automatedTaskConfigurationProperties, logApi, lockService);
        this.eventRepository = eventRepository;
        this.mediaRepository = mediaRepository;
        this.mediaLinkedCaseRepository = mediaLinkedCaseRepository;
        this.audioBufferSeconds = audioBufferSeconds;
        this.hearingRepository = hearingRepository;
    }

    @Override
    public AutomatedTaskName getAutomatedTaskName() {
        return AutomatedTaskName.AUDIO_LINKING_TASK_NAME;
    }

    @Override
    @Transactional
    public void run() {
        super.run();
    }


    @Override
    protected void runTask() {
        log.info("Running AudioLinkingAutomatedTask");
        List<EventEntity> events = eventRepository.findAllByEventStatus(EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber(),
                                                                        Limit.of(getAutomatedTaskBatchSize()));
        final Set<MediaLinkedCaseEntity> mediaLinkedCaseEntities = new HashSet<>();
        final Set<HearingEntity> editedHearingEntities = new HashSet<>();

        events.forEach(event -> processEvent(event, editedHearingEntities, mediaLinkedCaseEntities));

        hearingRepository.saveAll(editedHearingEntities);
        mediaLinkedCaseRepository.saveAll(mediaLinkedCaseEntities);
        eventRepository.saveAll(events);
    }

    void processEvent(EventEntity event, Set<HearingEntity> editedHearingEntities, Set<MediaLinkedCaseEntity> mediaLinkedCaseEntities) {
        List<MediaEntity> mediaEntities = mediaRepository.findAllByMediaTimeContains(
            event.getCourtroom().getId(),
            event.getTimestamp().plusSeconds(getAudioBufferSeconds()),
            event.getTimestamp().minusSeconds(getAudioBufferSeconds()));
        mediaEntities.forEach(mediaEntity -> processMedia(event.getHearingEntities(), mediaEntity, mediaLinkedCaseEntities, editedHearingEntities));
        event.setEventStatus(EventStatus.AUDIO_LINKED.getStatusNumber());
    }

    void processMedia(List<HearingEntity> hearingEntities, MediaEntity mediaEntity,
                      Set<MediaLinkedCaseEntity> mediaLinkedCaseEntities,
                      Set<HearingEntity> hearingsToSave) {
        hearingEntities.forEach(hearingEntity -> {
            if (!hearingEntity.containsMedia(mediaEntity)) {
                hearingEntity.addMedia(mediaEntity);
                hearingsToSave.add(hearingEntity);
                CourtCaseEntity courtCase = hearingEntity.getCourtCase();
                if (!mediaLinkedCaseRepository.existsByMediaAndCourtCase(mediaEntity, courtCase)) {
                    mediaLinkedCaseEntities.add(new MediaLinkedCaseEntity(mediaEntity, courtCase));
                }
                log.info("Linking media {} to hearing {}", mediaEntity.getId(), hearingEntity.getId());
            }
        });
    }
}
