package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

@Component
@RequiredArgsConstructor
public class CreatePolicyTypeValidator implements Validator<String> {

    private final RetentionPolicyTypeRepository repository;

    @Override
    public void validate(String fixedPolicyKey) {
        if (repository.findFirstByFixedPolicyKeyOrderByPolicyStartDesc(fixedPolicyKey).isPresent()) {
            throw new DartsApiException(RetentionApiError.NON_UNIQUE_FIXED_POLICY_KEY);
        }
    }

}
