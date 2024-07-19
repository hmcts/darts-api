package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

@Component
@RequiredArgsConstructor
public class EditPolicyTypeValidator implements Validator<String> {

    private final RetentionPolicyTypeRepository repository;

    @Override
    public void validate(String fixedPolicyKey) {
        // Check if database constraint "retention_policy_type_type_unq" is violated.
        if (repository.findByFixedPolicyKeyAndPolicyEndIsNull(fixedPolicyKey).isPresent()) {
            throw new DartsApiException(RetentionApiError.NON_UNIQUE_FIXED_POLICY_KEY);
        }
    }

}
