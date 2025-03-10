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
    private DataManagementConfiguration configuration;

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

    @Test
    void copy_withPreserveAccessTier() {
        // given
        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);

        when(configuration.getAzCopyExecutable()).thenReturn("/usr/bin/azcopy");
        String preserveAccessTier = "--s2s-preserve-access-tier=false";
        when(configuration.getAzCopyPreserveAccessTier()).thenReturn(preserveAccessTier);

        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        // when
        assertThatThrownBy(() -> azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)).isInstanceOf(DartsException.class)
            .hasMessageContaining(preserveAccessTier);
    }

    @Test
    void copy_withLogLevel() {
        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);

        when(configuration.getAzCopyExecutable()).thenReturn("/usr/bin/azcopy");
        String logLevel = "--log-level=ERROR";
        when(configuration.getAzCopyLogLevel()).thenReturn(logLevel);

        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        // when
        assertThatThrownBy(() -> azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)).isInstanceOf(DartsException.class)
            .hasMessageContaining(logLevel);
    }

    @Test
    void copy_withCheckLength() {
        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);

        when(configuration.getAzCopyExecutable()).thenReturn("/usr/bin/azcopy");
        String checkLevel = "--check-length=false";
        when(configuration.getAzCopyCheckLength()).thenReturn(checkLevel);

        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        // when
        assertThatThrownBy(() -> azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)).isInstanceOf(DartsException.class)
            .hasMessageContaining(checkLevel);
    }

    @Test
    void copy_withOutputLevelQuiet() {
        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);

        when(configuration.getAzCopyExecutable()).thenReturn("/usr/bin/azcopy");
        String outputLevel = "--output-level=quiet";
        when(configuration.getAzCopyOutputLevel()).thenReturn(outputLevel);

        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        // when
        assertThatThrownBy(() -> azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)).isInstanceOf(DartsException.class)
            .hasMessageContaining(outputLevel);
    }
}