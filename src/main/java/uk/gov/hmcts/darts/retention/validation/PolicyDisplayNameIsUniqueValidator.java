package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.BiValidator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

@Component
@RequiredArgsConstructor
public class PolicyDisplayNameIsUniqueValidator implements BiValidator<String, String> {

    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Override
    public void validate(String displayName, String fixedPolicyKey) {
        if (retentionPolicyTypeRepository.findFirstByDisplayNameAndFixedPolicyKeyNot(displayName, fixedPolicyKey).isPresent()) {
            throw new DartsApiException(RetentionApiError.NON_UNIQUE_POLICY_DISPLAY_NAME);
        }
    }

}
