package uk.gov.hmcts.darts.audio.validator;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.darts.audio.validation.AudioMediaPatchRequestValidator;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

public class AudioMediaPatchRequestValidatorTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private MediaRequestRepository mediaRequestRepository;

    @InjectMocks
    private AudioMediaPatchRequestValidator audioMediaPatchRequestValidator;

    @Test
    public void successfulValidation() {

    }
}