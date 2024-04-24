package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RevisePolicyTypeValidator implements Validator<String> {

    private final RetentionPolicyTypeRepository repository;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void validate(String fixedPolicyKey) {
        Optional<RetentionPolicyTypeEntity> existingPolicyOptional = repository.findFirstByFixedPolicyKeyOrderByPolicyStartDesc(
            fixedPolicyKey);

        if (existingPolicyOptional.isEmpty()) {
            throw new DartsApiException(RetentionApiError.FIXED_POLICY_KEY_NOT_FOUND);
        }
        RetentionPolicyTypeEntity existingPolicy = existingPolicyOptional.get();

        if (existingPolicy.getPolicyStart().isAfter(currentTimeHelper.currentOffsetDateTime())) {
            throw new DartsApiException(RetentionApiError.POLICY_START_DATE_MUST_BE_PAST);
        }
    }

}
