package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseManagementRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.service.CaseManagementRetentionService;

import java.text.MessageFormat;
import java.util.List;

import static uk.gov.hmcts.darts.event.exception.EventError.EVENT_DATA_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
public class CaseManagementRetentionServiceImpl implements CaseManagementRetentionService {

    private final CaseManagementRetentionRepository caseManagementRetentionRepository;

    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public CaseManagementRetentionEntity createCaseManagementRetention(EventEntity eventEntity, CourtCaseEntity courtCase,
                                                                       DartsEventRetentionPolicy dartsEventRetentionPolicy) {
        CaseManagementRetentionEntity caseManagementRetentionEntity = new CaseManagementRetentionEntity();
        caseManagementRetentionEntity.setCourtCase(courtCase);
        caseManagementRetentionEntity.setEventEntity(eventEntity);

        caseManagementRetentionEntity.setRetentionPolicyTypeEntity(getRetentionPolicy(dartsEventRetentionPolicy.getCaseRetentionFixedPolicy()));
        caseManagementRetentionEntity.setTotalSentence(dartsEventRetentionPolicy.getCaseTotalSentence());

        return caseManagementRetentionRepository.save(caseManagementRetentionEntity);
    }

    private RetentionPolicyTypeEntity getRetentionPolicy(String fixedPolicyKey) {
        List<RetentionPolicyTypeEntity> retentionPolicyList = retentionPolicyTypeRepository.findCurrentWithFixedPolicyKey(
            fixedPolicyKey, currentTimeHelper.currentOffsetDateTime());
        if (retentionPolicyList.isEmpty()) {
            throw new DartsApiException(EVENT_DATA_NOT_FOUND,
                                        MessageFormat.format("Could not find a retention policy for fixedPolicyKey ''{0}''", fixedPolicyKey));
        } else if (retentionPolicyList.size() > 1) {
            throw new DartsApiException(EVENT_DATA_NOT_FOUND,
                                        MessageFormat.format("More than 1 retention policy found for fixedPolicyKey ''{0}''", fixedPolicyKey));
        }
        return retentionPolicyList.getFirst();
    }
}
