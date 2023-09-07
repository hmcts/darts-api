package uk.gov.hmcts.darts.cases.validator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.PatchRequestObject;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatchCaseRequestValidatorTest {

    @Test
    void validateOk() {
        PatchRequestObject patchRequestObject = new PatchRequestObject();
        patchRequestObject.setRetainUntil(OffsetDateTime.of(2020, 6, 20, 14, 10, 0, 0, ZoneOffset.UTC));
        PatchCaseRequestValidator.validate(patchRequestObject);
    }

    @Test
    void validateFail() {
        PatchRequestObject patchRequestObject = new PatchRequestObject();
        patchRequestObject.setRetainUntil(null);
        DartsApiException exception = assertThrows(DartsApiException.class, () ->
            PatchCaseRequestValidator.validate(patchRequestObject));
        assertEquals(
            "The request does not contain any values that are supported by the PATCH operation.",
            exception.getMessage()
        );
    }
}
