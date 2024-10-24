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
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
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
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.event.service.EventService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Transactional
public class DataAnonymisationServiceImpl implements DataAnonymisationService {

    private final AuditApi auditApi;
    private final UserIdentity userIdentity;

    private final CurrentTimeHelper currentTimeHelper;
    private final ExternalOutboundDataStoreDeleter outboundDataStoreDeleter;
    private final CaseRepository caseRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final LogApi logApi;
    private final EventService eventService;

    @Override
    public void anonymizeCourtCaseEntity(UserAccountEntity userAccount, CourtCaseEntity courtCase) {
        courtCase.getDefendantList().forEach(defendantEntity -> anonymizeDefendantEntity(userAccount, defendantEntity));
        courtCase.getDefenceList().forEach(defenceEntity -> anonymizeDefenceEntity(userAccount, defenceEntity));
        courtCase.getProsecutorList().forEach(prosecutorEntity -> anonymizeProsecutorEntity(userAccount, prosecutorEntity));
        courtCase.getHearings().forEach(hearingEntity -> anonymizeHearingEntity(userAccount, hearingEntity));

        courtCase.markAsExpired(userAccount);
        //This also saves defendant, defence and prosecutor entities
        tidyUpTransformedMediaEntities(userAccount, courtCase);
        auditApi.record(AuditActivity.CASE_EXPIRED, userAccount, courtCase);
        anonymizeCreatedModifiedBaseEntity(userAccount, courtCase);
        caseRepository.save(courtCase);

        //Required for Dynatrace dashboards
        logApi.caseDeletedDueToExpiry(courtCase.getId(), courtCase.getCaseNumber());
    }

    void anonymizeDefenceEntity(UserAccountEntity userAccount, DefenceEntity entity) {
        anonymizeName(userAccount, entity);
    }

    void anonymizeDefendantEntity(UserAccountEntity userAccount, DefendantEntity entity) {
        anonymizeName(userAccount, entity);
    }

    void anonymizeProsecutorEntity(UserAccountEntity userAccount, ProsecutorEntity entity) {
        anonymizeName(userAccount, entity);
    }

    void anonymizeHearingEntity(UserAccountEntity userAccount, HearingEntity hearingEntity) {
        hearingEntity.getTranscriptions().forEach(transcriptionEntity -> anonymizeTranscriptionEntity(userAccount, transcriptionEntity));
        hearingEntity.getEventList().forEach(eventEntity -> anonymizeEventEntity(userAccount, eventEntity));
    }


    @Override
    @Transactional
    public void obfuscateEventByIds(List<Integer> eveIds) {
        eveIds.stream()
            .map(eventService::getEventByEveId)
            .distinct()
            .forEach(this::anonymizeEvent);
    }

    @Override
    public void anonymizeEvent(EventEntity eventEntity) {
        anonymizeEventEntity(getUserAccount(), eventEntity);
        eventService.saveEvent(eventEntity);
    }

    void anonymizeEventEntity(UserAccountEntity userAccount, EventEntity eventEntity) {
        eventEntity.setEventText(UUID.randomUUID().toString());
        eventEntity.setDataAnonymised(true);
        anonymizeCreatedModifiedBaseEntity(userAccount, eventEntity);
    }

    void anonymizeTranscriptionEntity(UserAccountEntity userAccount, TranscriptionEntity transcriptionEntity) {
        transcriptionEntity.getTranscriptionCommentEntities()
            .forEach(transcriptionCommentEntity -> anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity));
        transcriptionEntity.getTranscriptionWorkflowEntities().forEach(this::anonymizeTranscriptionWorkflowEntity);
    }

    void anonymizeTranscriptionCommentEntity(UserAccountEntity userAccount, TranscriptionCommentEntity transcriptionCommentEntity) {
        transcriptionCommentEntity.setComment(UUID.randomUUID().toString());
        transcriptionCommentEntity.setDataAnonymised(true);
        anonymizeCreatedModifiedBaseEntity(userAccount, transcriptionCommentEntity);
    }

    void anonymizeTranscriptionWorkflowEntity(TranscriptionWorkflowEntity transcriptionWorkflowEntity) {
        transcriptionWorkflowEntity.close();

    }

    @Override
    public UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
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
        anonymizeCreatedModifiedBaseEntity(userAccount, mediaRequestEntity);
    }


    private <T extends CreatedModifiedBaseEntity & IsNamedEntity> void anonymizeName(UserAccountEntity userAccount, T entity) {
        entity.setName(UUID.randomUUID().toString());
        anonymizeCreatedModifiedBaseEntity(userAccount, entity);
    }

    private void anonymizeCreatedModifiedBaseEntity(UserAccountEntity userAccount, CreatedModifiedBaseEntity entity) {
        entity.setLastModifiedBy(userAccount);
        entity.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
    }
}
