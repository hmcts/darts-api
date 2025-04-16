package uk.gov.hmcts.darts.util;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureCopyUtil {

    private static final int SUCCESS_EXIT_CODE = 0;
    private static final String COPY_COMMAND = "copy";
    private final DataManagementConfiguration config;

    @SuppressWarnings("PMD.DoNotUseThreads")//TODO - refactor to avoid using Thread.sleep() when this is next edited
    public void copy(String source, String destination) {
        try {
            List<String> command = buildCommand(source, destination);
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            var startTime = Instant.now();
            log.info("Copy of blob started at {}", startTime);
            //TODO: remove this log.info once we are confident that the command is being built correctly
            log.info("Executing command: {}", builder.command());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitValue = process.waitFor();
            var endTime = Instant.now();
            log.info("Copy of blob completed at {}. Total duration in seconds: {}. Exit value: {}",
                     endTime, Duration.between(startTime, endTime).getSeconds(), exitValue);
            if (exitValue != SUCCESS_EXIT_CODE) {
                handleError(source, destination, process, builder);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DartsException("Failed to execute azure copy - interrupted ", ie);
        } catch (Exception e) {
            throw new DartsException("Failed to execute azure copy ", e);
        }
    }

    private void handleError(String source, String destination, Process process, ProcessBuilder builder) throws IOException {
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

    List<String> buildCommand(String source, String destination) {
        List<String> command = new ArrayList<>();
        command.add(config.getAzCopyExecutable());
        command.add(COPY_COMMAND);
        command.add(source);
        command.add(destination);
        if (StringUtils.isNotEmpty(config.getAzCopyPreserveAccessTier())) {
            command.add(config.getAzCopyPreserveAccessTier());
        }
        if (StringUtils.isNotEmpty(config.getAzCopyLogLevel())) {
            command.add(config.getAzCopyLogLevel());
        }
        if (StringUtils.isNotEmpty(config.getAzCopyOutputLevel())) {
            command.add(config.getAzCopyOutputLevel());
        }
        return command;
    }
}
