package uk.gov.hmcts.darts.cases.validator;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.PatchRequestObject;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

@UtilityClass
public class PatchCaseRequestValidator {

    public void validate(PatchRequestObject patchRequestObject) {
        if (patchRequestObject.getRetainUntil() == null) {
            throw new DartsApiException(CaseApiError.PATCH_CRITERIA_NOT_MET);
        }
    }
}
