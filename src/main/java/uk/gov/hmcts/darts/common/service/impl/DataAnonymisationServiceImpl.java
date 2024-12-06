package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DataAnonymisationEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.DataAnonymisationRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DataAnonymisationServiceImpl implements DataAnonymisationService {

    private final AuditApi auditApi;

    private final CurrentTimeHelper currentTimeHelper;
    private final ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final LogApi logApi;
    private final CaseService caseService;
    private final EventService eventService;
    private final DataAnonymisationRepository dataAnonymisationRepository;


    @Override
    @Transactional
    public void anonymiseCourtCaseById(UserAccountEntity userAccount, Integer courtCaseId, boolean isManuallyRequested) {
        anonymiseCourtCaseEntity(userAccount, caseService.getCourtCaseById(courtCaseId), isManuallyRequested);
    }

    void anonymiseCourtCaseEntity(UserAccountEntity userAccount, CourtCaseEntity courtCase, boolean isManuallyRequested) {
        courtCase.getDefendantList().forEach(defendantEntity -> anonymiseDefendantEntity(userAccount, defendantEntity));
        courtCase.getDefenceList().forEach(defenceEntity -> anonymiseDefenceEntity(userAccount, defenceEntity));
        courtCase.getProsecutorList().forEach(prosecutorEntity -> anonymiseProsecutorEntity(userAccount, prosecutorEntity));
        courtCase.getHearings().forEach(hearingEntity -> anonymiseHearingEntity(userAccount, hearingEntity, isManuallyRequested));
        courtCase.markAsExpired(userAccount);
        //This also saves defendant, defence and prosecutor entities
        tidyUpTransformedMediaEntities(userAccount, courtCase);
        auditApi.record(AuditActivity.CASE_EXPIRED, userAccount, courtCase);
        anonymiseCreatedModifiedBaseEntity(userAccount, courtCase);
        caseService.saveCase(courtCase);
        anonymiseAllEventsFromCase(userAccount, courtCase, isManuallyRequested);

        //Required for Dynatrace dashboards
        logApi.caseDeletedDueToExpiry(courtCase.getId(), courtCase.getCaseNumber());
    }

    void anonymiseDefenceEntity(UserAccountEntity userAccount, DefenceEntity entity) {
        anonymiseName(userAccount, entity);
    }

    void anonymiseDefendantEntity(UserAccountEntity userAccount, DefendantEntity entity) {
        anonymiseName(userAccount, entity);
    }

    void anonymiseProsecutorEntity(UserAccountEntity userAccount, ProsecutorEntity entity) {
        anonymiseName(userAccount, entity);
    }

    void anonymiseHearingEntity(UserAccountEntity userAccount, HearingEntity hearingEntity, boolean isManuallyRequested) {
        hearingEntity.getTranscriptions().forEach(transcriptionEntity -> anonymiseTranscriptionEntity(userAccount, transcriptionEntity, isManuallyRequested));
    }


    @Override
    @Transactional
    public void anonymiseEventByIds(UserAccountEntity userAccount, List<Integer> eveIds, boolean isManuallyRequested) {
        eveIds.stream()
            .map(eventService::getEventByEveId)
            .distinct()
            .forEach(eventEntity -> {
                anonymiseEvent(userAccount, eventEntity, isManuallyRequested);
            });
    }

    @Override
    public void anonymiseEvent(UserAccountEntity userAccount, EventEntity eventEntity, boolean isManuallyRequested) {
        anonymiseEventEntity(userAccount, eventEntity, false, isManuallyRequested);
    }

    void anonymiseEventEntity(UserAccountEntity userAccount, EventEntity eventEntity,
                              boolean onlyAnonymiseIfAllCasesExpired, boolean isManuallyRequested) {
        if (eventEntity.isDataAnonymised()) {
            log.debug("Event {} already anonymised skipping", eventEntity.getId());
            return;
        }
        if (onlyAnonymiseIfAllCasesExpired && !eventService.allAssociatedCasesAnonymised(eventEntity)) {
            log.debug("Event {} not anonymised as not all cases are expired", eventEntity.getId());
            return;
        }
        eventEntity.setEventText(UUID.randomUUID().toString());
        eventEntity.setDataAnonymised(true);
        anonymiseCreatedModifiedBaseEntity(userAccount, eventEntity);
        eventService.saveEvent(eventEntity);
        if (isManuallyRequested) {
            auditApi.record(AuditActivity.MANUAL_OBFUSCATION, userAccount, eventEntity.getId().toString());
            logApi.manualObfuscation(eventEntity);
        }
        registerDataAnonymisation(userAccount, eventEntity, isManuallyRequested);
    }

    void anonymiseTranscriptionEntity(UserAccountEntity userAccount, TranscriptionEntity transcriptionEntity, boolean isManuallyRequested) {
        transcriptionEntity.getTranscriptionCommentEntities()
            .forEach(transcriptionCommentEntity -> anonymiseTranscriptionCommentEntity(userAccount, transcriptionCommentEntity, isManuallyRequested));
        transcriptionEntity.getTranscriptionWorkflowEntities().forEach(this::anonymiseTranscriptionWorkflowEntity);
    }

    void anonymiseTranscriptionCommentEntity(UserAccountEntity userAccount, TranscriptionCommentEntity transcriptionCommentEntity,
                                             boolean isManuallyRequested) {
        if (transcriptionCommentEntity.isDataAnonymised()) {
            log.debug("Transcription comment {} already anonymised skipping", transcriptionCommentEntity.getId());
            return;
        }
        transcriptionCommentEntity.setComment(UUID.randomUUID().toString());
        transcriptionCommentEntity.setDataAnonymised(true);
        anonymiseCreatedModifiedBaseEntity(userAccount, transcriptionCommentEntity);
        registerDataAnonymisation(userAccount, transcriptionCommentEntity, isManuallyRequested);
    }

    void anonymiseTranscriptionWorkflowEntity(TranscriptionWorkflowEntity transcriptionWorkflowEntity) {
        transcriptionWorkflowEntity.close();
    }

    void tidyUpTransformedMediaEntities(UserAccountEntity userAccount, CourtCaseEntity courtCase) {
        Set<MediaRequestEntity> mediaRequests = courtCase
            .getHearings()
            .stream()
            .map(HearingEntity::getMediaRequests)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());


        Set<TransformedMediaEntity> transformedMediaEntities =
            mediaRequests.stream()
                .peek(mediaRequestEntity -> expiredMediaRequest(userAccount, mediaRequestEntity))
                .map(MediaRequestEntity::getTransformedMediaEntities)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        transformedMediaEntities.forEach(this::deleteTransformedMediaEntity);
    }

    void deleteTransformedMediaEntity(TransformedMediaEntity transformedMediaEntity) {
        if (transformedMediaEntity.getTransientObjectDirectoryEntities().stream()
            .peek(transientObjectDirectoryEntity -> transientObjectDirectoryEntity.setTransformedMedia(null))
            .filter(transientObjectDirectoryEntity -> !deleteTransientObjectDirectoryEntity(transientObjectDirectoryEntity))
            .count() == 0) {
            transformedMediaRepository.delete(transformedMediaEntity);
        }
    }

    boolean deleteTransientObjectDirectoryEntity(TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
        if (outboundDataStoreDeleter.delete(transientObjectDirectoryEntity)) {
            transientObjectDirectoryRepository.delete(transientObjectDirectoryEntity);
            return true;
        }
        return false;
    }

    void expiredMediaRequest(UserAccountEntity userAccount, MediaRequestEntity mediaRequestEntity) {
        mediaRequestEntity.setStatus(MediaRequestStatus.EXPIRED);
        anonymiseCreatedModifiedBaseEntity(userAccount, mediaRequestEntity);
    }

    void registerDataAnonymisation(UserAccountEntity userAccount, EventEntity eventEntity, boolean isManualRequest) {
        registerDataAnonymisation(userAccount, dataAnonymisationEntity -> dataAnonymisationEntity.setEvent(eventEntity), isManualRequest);
    }

    void registerDataAnonymisation(UserAccountEntity userAccount, TranscriptionCommentEntity transcriptionCommentEntity, boolean isManualRequest) {
        registerDataAnonymisation(userAccount, dataAnonymisationEntity -> dataAnonymisationEntity.setTranscriptionComment(transcriptionCommentEntity),
                                  isManualRequest);
    }

    void registerDataAnonymisation(UserAccountEntity userAccount, Consumer<DataAnonymisationEntity> preSaveConsumer, boolean isManualRequest) {
        OffsetDateTime currentTime = currentTimeHelper.currentOffsetDateTime();
        DataAnonymisationEntity dataAnonymisationEntity = new DataAnonymisationEntity();
        dataAnonymisationEntity.setRequestedBy(userAccount);
        dataAnonymisationEntity.setRequestedTs(currentTime);
        dataAnonymisationEntity.setApprovedBy(userAccount);
        dataAnonymisationEntity.setApprovedTs(currentTime);
        dataAnonymisationEntity.setIsManualRequest(isManualRequest);
        preSaveConsumer.accept(dataAnonymisationEntity);
        dataAnonymisationRepository.save(dataAnonymisationEntity);
    }


    private <T extends CreatedModifiedBaseEntity & IsNamedEntity> void anonymiseName(UserAccountEntity userAccount, T entity) {
        entity.setName(UUID.randomUUID().toString());
        anonymiseCreatedModifiedBaseEntity(userAccount, entity);
    }

    private void anonymiseCreatedModifiedBaseEntity(UserAccountEntity userAccount, CreatedModifiedBaseEntity entity) {
        entity.setLastModifiedBy(userAccount);
        entity.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
    }

    private void anonymiseAllEventsFromCase(UserAccountEntity userAccount, CourtCaseEntity courtCase, boolean isManuallyRequested) {
        eventService.getAllCourtCaseEventVersions(courtCase).forEach(eventEntity -> anonymiseEventEntity(userAccount, eventEntity, true, isManuallyRequested));
    }
}