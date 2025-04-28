package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptionsItem;

import java.util.Optional;

class TranscriptionsUpdateSetHidingValidatorTest {

    private TranscriptionsUpdateSetHidingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TranscriptionsUpdateSetHidingValidator();
    }

    @Test
    void testValidateWhenSuccess() {
        UpdateTranscriptionsItem transcriptions = new UpdateTranscriptionsItem();
        transcriptions.setTranscriptionId(100L);
        transcriptions.setHideRequestFromRequestor(true);

        TranscriptionStatusEntity status = new TranscriptionStatusEntity();
        status.setId(TranscriptionStatusEnum.COMPLETE.getId());

        TranscriptionEntity entity = new TranscriptionEntity();
        entity.setTranscriptionStatus(status);

        Assertions.assertTrue(validator.validate(Optional.of(entity), transcriptions));
    }

    @Test
    void testValidateWhenFailureOnState() {
        UpdateTranscriptionsItem transcriptions = new UpdateTranscriptionsItem();
        transcriptions.setTranscriptionId(100L);
        transcriptions.setHideRequestFromRequestor(true);

        TranscriptionStatusEntity status = new TranscriptionStatusEntity();
        status.setId(TranscriptionStatusEnum.AWAITING_AUTHORISATION.getId());

        TranscriptionEntity entity = new TranscriptionEntity();
        entity.setTranscriptionStatus(status);

        Assertions.assertFalse(validator.validate(Optional.of(entity), transcriptions));
    }

    @Test
    void testValidateWhenEntityNotFound() {
        UpdateTranscriptionsItem transcriptions = new UpdateTranscriptionsItem();
        transcriptions.setTranscriptionId(100L);
        transcriptions.setHideRequestFromRequestor(true);

        Assertions.assertFalse(validator.validate(Optional.empty(), transcriptions));
    }

    @Test
    void testValidateWhenSuccessWithHideFalseRegardlessOfState() {
        UpdateTranscriptionsItem transcriptions = new UpdateTranscriptionsItem();
        transcriptions.setTranscriptionId(100L);
        transcriptions.setHideRequestFromRequestor(false);

        TranscriptionStatusEntity status = new TranscriptionStatusEntity();
        status.setId(TranscriptionStatusEnum.AWAITING_AUTHORISATION.getId());

        TranscriptionEntity entity = new TranscriptionEntity();
        entity.setTranscriptionStatus(status);

        Assertions.assertTrue(validator.validate(Optional.of(entity), transcriptions));
    }
}
