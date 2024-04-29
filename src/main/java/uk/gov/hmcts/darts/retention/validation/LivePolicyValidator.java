package uk.gov.hmcts.darts.retention.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class LivePolicyValidator implements Validator<OffsetDateTime> {

    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void validate(OffsetDateTime startDate) {
        if (startDate.isBefore(currentTimeHelper.currentOffsetDateTime())) {
            throw new DartsApiException(RetentionApiError.LIVE_POLICIES_CANNOT_BE_EDITED);
        }
    }

}
