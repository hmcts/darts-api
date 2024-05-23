package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzCopyUtilTest {

    @Test
    void testExceptionMessageDoesNotIncludeSourceOrDestinationInfoToAvoidSecretsLeak() {
        AzCopyUtil azCopyUtil = new AzCopyUtil();
        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        assertThatThrownBy(() ->
            azCopyUtil.copy(sourceSasUrl, destinationSasUrl)
        ).isInstanceOf(IOException.class)
            .hasMessageNotContaining(sourceSasUrl)
            .hasMessageNotContaining(destinationSasUrl);
    }

}