package uk.gov.hmcts.darts.event.service;


import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;

@FunctionalInterface
public interface CaseManagementRetentionService {
    CaseManagementRetentionEntity createCaseManagementRetention(EventEntity eventEntity, CourtCaseEntity courtCase,
                                                                DartsEventRetentionPolicy dartsEventRetentionPolicy);
}
