package uk.gov.hmcts.darts.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureCopyUtil {

    private final DataManagementConfiguration config;

    public void copy(String source, String destination) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(config.getAzCopyExecutable(), "copy", source, destination, config.getAzCopyPreserveAccessTier());

            var startTime = Instant.now();
            log.info("Copy of blob started at {}", startTime);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitValue = process.waitFor();
            var endTime = Instant.now();
            log.info("Copy of blob completed at {}. Total duration in seconds: {}. Exit value: {}",
                     endTime, Duration.between(startTime, endTime).getSeconds(), exitValue);
            if (exitValue != 0) {
                String result = new String(process.getInputStream().readAllBytes());
                String errorMessage = String.format(
                    "Failed to execute azcopy from source: '%s' to destination '%s'- error exit value. Command: '%s'. Result: %s",
                    source,
                    destination,
                    builder.command(),
                    result);
                log.error(errorMessage);
                throw new DartsException(errorMessage);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DartsException("Failed to execute azure copy - interrupted", ie);
        } catch (Exception e) {
            throw new DartsException("Failed to execute azure copy", e);
        }
    }
}
