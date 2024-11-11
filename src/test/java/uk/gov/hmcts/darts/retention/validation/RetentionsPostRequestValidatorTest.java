package uk.gov.hmcts.darts.retention.validation;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetentionsPostRequestValidatorTest {

    @Test
    void ok_retentionDate() {
        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2020, 6, 1));
        postRetentionRequest.setComments("theComment");

        RetentionsPostRequestValidator.validate(postRetentionRequest);
    }

    @Test
    void ok_permanentRetention() {
        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setIsPermanentRetention(true);
        postRetentionRequest.setComments("theComment");

        RetentionsPostRequestValidator.validate(postRetentionRequest);
    }

    @Test
    void fail_bothPermanentRetentionAndDate() {
        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setRetentionDate(LocalDate.of(2020, 6, 1));
        postRetentionRequest.setIsPermanentRetention(true);
        postRetentionRequest.setComments("theComment");

        DartsApiException exception = assertThrows(DartsApiException.class, () -> RetentionsPostRequestValidator.validate(postRetentionRequest));
        assertEquals("RETENTION_102", exception.getError().getType());
        assertEquals("Both 'is_permanent_retention' and 'retention_date' cannot be set, must be either one or the other.", exception.getDetail());
    }

    @Test
    void fail_RetentionDateFalse() {
        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setIsPermanentRetention(false);
        postRetentionRequest.setComments("theComment");

        DartsApiException exception = assertThrows(DartsApiException.class, () -> RetentionsPostRequestValidator.validate(postRetentionRequest));
        assertEquals("RETENTION_102", exception.getError().getType());
        assertEquals("Either 'is_permanent_retention' or 'retention_date' must be set.", exception.getDetail());
    }

    @Test
    void fail_neitherPermanentRetentionAndDate() {
        PostRetentionRequest postRetentionRequest = new PostRetentionRequest();
        postRetentionRequest.setCaseId(1);
        postRetentionRequest.setComments("theComment");

        DartsApiException exception = assertThrows(DartsApiException.class, () -> RetentionsPostRequestValidator.validate(postRetentionRequest));
        assertEquals("RETENTION_102", exception.getError().getType());
        assertEquals("Either 'is_permanent_retention' or 'retention_date' must be set.", exception.getDetail());
    }

}
