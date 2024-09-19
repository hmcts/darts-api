package uk.gov.hmcts.darts.common.service;

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

public interface DataAnonymisationService {
    default void anonymizeCourtCaseEntity(CourtCaseEntity courtCase) {
        anonymizeCourtCaseEntity(getUserAccount(), courtCase);
    }

    void anonymizeCourtCaseEntity(UserAccountEntity userAccount, CourtCaseEntity courtCase);

    void anonymizeDefenceEntity(UserAccountEntity userAccount, DefenceEntity entity);

    void anonymizeDefendantEntity(UserAccountEntity userAccount, DefendantEntity entity);

    void anonymizeProsecutorEntity(UserAccountEntity userAccount, ProsecutorEntity entity);

    void anonymizeHearingEntity(UserAccountEntity userAccount, HearingEntity hearingEntity);

    void anonymizeEventEntity(UserAccountEntity userAccount, EventEntity eventEntity);

    void anonymizeTranscriptionEntity(UserAccountEntity userAccount, TranscriptionEntity transcriptionEntity);

    void anonymizeTranscriptionCommentEntity(UserAccountEntity userAccount, TranscriptionCommentEntity transcriptionCommentEntity);

    void anonymizeTranscriptionWorkflowEntity(TranscriptionWorkflowEntity transcriptionWorkflowEntity);

    UserAccountEntity getUserAccount();

}
