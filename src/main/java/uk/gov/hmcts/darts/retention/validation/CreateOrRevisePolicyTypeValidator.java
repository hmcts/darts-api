package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;
import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;

@Component
@RequiredArgsConstructor
public class CreateOrRevisePolicyTypeValidator implements Validator<AdminPostRetentionRequest> {

    private final CurrentTimeHelper currentTimeHelper;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    @Override
    public void validate(AdminPostRetentionRequest request) {
        final String fixedPolicyKey = request.getFixedPolicyKey();

        if (retentionPolicyTypeRepository.findFirstByPolicyNameAndFixedPolicyKeyNot(request.getName(), fixedPolicyKey).isPresent()) {
            throw new DartsApiException(RetentionApiError.NON_UNIQUE_POLICY_NAME);
        }

        if (retentionPolicyTypeRepository.findFirstByDisplayNameAndFixedPolicyKeyNot(request.getDisplayName(), fixedPolicyKey).isPresent()) {
            throw new DartsApiException(RetentionApiError.NON_UNIQUE_POLICY_DISPLAY_NAME);
        }

        if (request.getPolicyStartAt().isBefore(currentTimeHelper.currentOffsetDateTime())) {
            throw new DartsApiException(RetentionApiError.POLICY_START_MUST_BE_FUTURE);
        }
    }

}
