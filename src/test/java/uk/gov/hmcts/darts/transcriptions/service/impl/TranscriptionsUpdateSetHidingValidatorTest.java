package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptions;

import java.util.Optional;

public class TranscriptionsUpdateSetHidingValidatorTest {

    private TranscriptionsUpdateSetHidingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TranscriptionsUpdateSetHidingValidator();
    }

    @Test
    public void testValidateWhenSuccess() {
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(100);
        transcriptions.setHideRequestFromRequestor(true);

        TranscriptionStatusEntity status = new TranscriptionStatusEntity();
        status.setId(TranscriptionStatusEnum.COMPLETE.getId());

        TranscriptionEntity entity = new TranscriptionEntity();
        entity.setTranscriptionStatus(status);

        Assertions.assertTrue(validator.validate(Optional.of(entity), transcriptions));
    }

    @Test
    public void testValidateWhenFailureOnState() {
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(100);
        transcriptions.setHideRequestFromRequestor(true);

        TranscriptionStatusEntity status = new TranscriptionStatusEntity();
        status.setId(TranscriptionStatusEnum.AWAITING_AUTHORISATION.getId());

        TranscriptionEntity entity = new TranscriptionEntity();
        entity.setTranscriptionStatus(status);

        Assertions.assertFalse(validator.validate(Optional.of(entity), transcriptions));
    }

    @Test
    public void testValidateWhenEntityNotFound() {
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(100);
        transcriptions.setHideRequestFromRequestor(true);

        Assertions.assertFalse(validator.validate(Optional.empty(), transcriptions));
    }

    @Test
    public void testValidateWhenSuccessWithHideFalseRegardlessOfState() {
        UpdateTranscriptions transcriptions = new UpdateTranscriptions();
        transcriptions.setTranscriptionId(100);
        transcriptions.setHideRequestFromRequestor(false);

        TranscriptionStatusEntity status = new TranscriptionStatusEntity();
        status.setId(TranscriptionStatusEnum.AWAITING_AUTHORISATION.getId());

        TranscriptionEntity entity = new TranscriptionEntity();
        entity.setTranscriptionStatus(status);

        Assertions.assertTrue(validator.validate(Optional.of(entity), transcriptions));
    }
}
