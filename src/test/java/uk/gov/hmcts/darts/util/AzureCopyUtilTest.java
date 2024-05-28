package uk.gov.hmcts.darts.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureCopyUtilTest {

    @Mock
    DataManagementConfiguration configuration;

    @Test
    void testExceptionMessageDoesNotIncludeSourceOrDestinationInfoToAvoidSecretsLeak() {

        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);
        when(configuration.getAzCopyExecutable()).thenReturn("not/existing/path/azcopy");
        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        assertThatThrownBy(() ->
                               azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)
        ).isInstanceOf(DartsException.class)
            .hasMessageNotContaining(sourceSasUrl)
            .hasMessageNotContaining(destinationSasUrl);
    }

    @Test
    void testInterruptedExceptionMessageDoesNotIncludeSourceOrDestinationInfoToAvoidSecretsLeak() {

        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);
        when(configuration.getAzCopyExecutable()).thenAnswer(invocation -> {
            throw new InterruptedException();
        });
        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        assertThatThrownBy(() ->
                               azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)
        ).isInstanceOf(DartsException.class)
            .hasMessageNotContaining(sourceSasUrl)
            .hasMessageNotContaining(destinationSasUrl);
    }

}