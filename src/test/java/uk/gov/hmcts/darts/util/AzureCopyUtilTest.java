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

    public static final String NOT_EXISTING_PATH_AZCOPY = "not/existing/path/azcopy";
    @Mock
    private DataManagementConfiguration configuration;

    @Test
    void copy_withExceptionMessageDoesNotIncludeSourceOrDestinationInfoToAvoidSecretsLeak() {

        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);
        when(configuration.getAzCopyExecutable()).thenReturn(NOT_EXISTING_PATH_AZCOPY);
        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        assertThatThrownBy(() ->
                               azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)
        ).isInstanceOf(DartsException.class)
            .hasMessageNotContaining(sourceSasUrl)
            .hasMessageNotContaining(destinationSasUrl);
    }

    @Test
    void copy_withInterruptedExceptionMessageDoesNotIncludeSourceOrDestinationInfoToAvoidSecretsLeak() {

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

        when(configuration.getAzCopyExecutable()).thenReturn(NOT_EXISTING_PATH_AZCOPY);
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

        when(configuration.getAzCopyExecutable()).thenReturn(NOT_EXISTING_PATH_AZCOPY);
        String logLevel = "--log-level=ERROR";
        when(configuration.getAzCopyLogLevel()).thenReturn(logLevel);

        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        // when
        assertThatThrownBy(() -> azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)).isInstanceOf(DartsException.class)
            .hasMessageContaining(logLevel);
    }

    @Test
    void copy_withOutputLevelQuiet() {
        AzureCopyUtil azureCopyUtil = new AzureCopyUtil(configuration);

        when(configuration.getAzCopyExecutable()).thenReturn(NOT_EXISTING_PATH_AZCOPY);
        String outputLevel = "--output-level=quiet";
        when(configuration.getAzCopyOutputLevel()).thenReturn(outputLevel);

        String sourceSasUrl = "someSasUrl";
        String destinationSasUrl = "someOtherSasUrl";

        // when
        assertThatThrownBy(() -> azureCopyUtil.copy(sourceSasUrl, destinationSasUrl)).isInstanceOf(DartsException.class)
            .hasMessageContaining(outputLevel);
    }
}