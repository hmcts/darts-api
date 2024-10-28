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
            Process p = builder.start();
            int exitValue = p.waitFor();
            var endTime = Instant.now();
            log.info("Copy of blob completed at {}. Total duration in seconds: {}", endTime, Duration.between(startTime, endTime).getSeconds());
            if (exitValue != 0) {
                String result = new String(p.getInputStream().readAllBytes());
                throw new DartsException(
                    String.format("Failed to execute azcopy from source: '%s' to destination '%s'- error exit value. Command: '%s'. Result: %s",
                                  source,
                                  destination,
                                  builder.command(),
                                  result));
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DartsException("Failed to execute azure copy - interrupted", ie);
        } catch (Exception e) {
            throw new DartsException("Failed to execute azure copy", e);
        }
    }
}
