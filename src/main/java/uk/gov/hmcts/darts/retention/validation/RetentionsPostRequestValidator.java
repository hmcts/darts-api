package uk.gov.hmcts.darts.retention.validation;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.retention.exception.RetentionApiError;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

@UtilityClass
public class RetentionsPostRequestValidator {

    public void validate(PostRetentionRequest postRetentionRequest) {
        if (BooleanUtils.isTrue(postRetentionRequest.getIsPermanentRetention())) {
            if (postRetentionRequest.getRetentionDate() != null) {
                throw new DartsApiException(
                    RetentionApiError.INVALID_REQUEST,
                    "Both 'is_permanent_retention' and 'retention_date' cannot be set, must be either one or the other."
                );
            }
        } else {
            if (postRetentionRequest.getRetentionDate() == null) {
                throw new DartsApiException(RetentionApiError.INVALID_REQUEST, "Either 'is_permanent_retention' or 'retention_date' must be se.");
            }
        }
    }
}
