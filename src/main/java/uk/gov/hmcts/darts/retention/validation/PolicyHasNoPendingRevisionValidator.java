package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PolicyHasNoPendingRevisionValidator implements Validator<String> {

    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void validate(String fixedPolicyKey) {
        List<RetentionPolicyTypeEntity> entitiesByFixedPolicyKey = retentionPolicyTypeRepository.findByFixedPolicyKeyOrderByPolicyStartDesc(
            fixedPolicyKey);
        if (entitiesByFixedPolicyKey.size() > 1) {
            RetentionPolicyTypeEntity currentPolicyForKey = entitiesByFixedPolicyKey.getFirst();
            if (currentPolicyForKey.getPolicyStart().isAfter(currentTimeHelper.currentOffsetDateTime())) {
                throw new DartsApiException(RetentionApiError.TARGET_POLICY_HAS_PENDING_REVISION,
                                            Map.of("pending_revision_id", currentPolicyForKey.getId()));
            }
        }
    }

}
