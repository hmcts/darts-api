package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AzureCopyUtilTest {

    @Test
    void testExceptionMessageDoesNotIncludeSourceOrDestinationInfoToAvoidSecretsLeak() {
        AzureCopyUtil azureCopyUtil = new AzureCopyUtil();
        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        assertThatThrownBy(() ->
            azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)
        ).isInstanceOf(IOException.class)
            .hasMessageNotContaining(sourceSasUrl)
            .hasMessageNotContaining(destinationSasUrl);
    }

}