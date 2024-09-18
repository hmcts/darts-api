package uk.gov.hmcts.darts.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DataAnonymisationServiceImpl implements DataAnonymisationService {

    private final AuditApi auditApi;
    private final UserIdentity userIdentity;

    @Override
    public void anonymizeCourtCaseEntity(UserAccountEntity userAccount, CourtCaseEntity courtCase, boolean isManual) {
        courtCase.getDefendantList().forEach(defendantEntity -> anonymizeDefendantEntity(userAccount, defendantEntity));
        courtCase.getDefenceList().forEach(defenceEntity -> anonymizeDefenceEntity(userAccount, defenceEntity));
        courtCase.getProsecutorList().forEach(prosecutorEntity -> anonymizeProsecutorEntity(userAccount, prosecutorEntity));
        courtCase.getHearings().forEach(hearingEntity -> anonymizeHearingEntity(userAccount, hearingEntity, isManual));

        courtCase.markAsExpired(userAccount);
        auditApi.record(AuditActivity.CASE_EXPIRED, userAccount, courtCase);

        //Required for Dynatrace dashboards
        log.info("Case expired: cas_id={}, case_number={}", courtCase.getId(), courtCase.getCaseNumber());
    }

    @Override
    public void anonymizeDefenceEntity(UserAccountEntity userAccount, DefenceEntity entity) {
        anonymizeName(userAccount, entity);
    }

    @Override
    public void anonymizeDefendantEntity(UserAccountEntity userAccount, DefendantEntity entity) {
        anonymizeName(userAccount, entity);
    }

    @Override
    public void anonymizeProsecutorEntity(UserAccountEntity userAccount, ProsecutorEntity entity) {
        anonymizeName(userAccount, entity);
    }

    @Override
    public void anonymizeHearingEntity(UserAccountEntity userAccount, HearingEntity hearingEntity, boolean isManual) {
        hearingEntity.getTranscriptions().forEach(transcriptionEntity -> anonymizeTranscriptionEntity(userAccount, transcriptionEntity, isManual));
        hearingEntity.getEventList().forEach(eventEntity -> anonymizeEventEntity(userAccount, eventEntity, isManual));
    }

    @Override
    public void anonymizeEventEntity(UserAccountEntity userAccount, EventEntity eventEntity, boolean isManual) {
        eventEntity.setEventText(UUID.randomUUID().toString());
        eventEntity.setLastModifiedBy(userAccount);
        if (isManual) {
            eventEntity.setDataAnonymised(true);
        }
    }

    @Override
    public void anonymizeTranscriptionEntity(UserAccountEntity userAccount, TranscriptionEntity transcriptionEntity,
                                             boolean isManual) {
        transcriptionEntity.getTranscriptionCommentEntities().forEach(
            transcriptionCommentEntity -> anonymizeTranscriptionCommentEntity(userAccount, transcriptionCommentEntity, isManual));
        transcriptionEntity.getTranscriptionWorkflowEntities().forEach(this::anonymizeTranscriptionWorkflowEntity);
    }

    @Override
    public void anonymizeTranscriptionCommentEntity(UserAccountEntity userAccount, TranscriptionCommentEntity transcriptionCommentEntity,
                                                    boolean isManual) {
        transcriptionCommentEntity.setComment(UUID.randomUUID().toString());
        transcriptionCommentEntity.setLastModifiedBy(userAccount);
        if (isManual) {
            transcriptionCommentEntity.setDataAnonymised(true);
        }
    }

    @Override
    public void anonymizeTranscriptionWorkflowEntity(TranscriptionWorkflowEntity transcriptionWorkflowEntity) {
        transcriptionWorkflowEntity.close();

    }

    @Override
    public UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
    }

    private <T extends CreatedModifiedBaseEntity & IsNamedEntity> void anonymizeName(UserAccountEntity userAccount, T entity) {
        entity.setName(UUID.randomUUID().toString());
        entity.setLastModifiedBy(userAccount);
    }
}
