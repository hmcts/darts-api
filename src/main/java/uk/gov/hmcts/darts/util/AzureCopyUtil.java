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
import java.util.StringJoiner;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureCopyUtil {

    private static final int SUCCESS_EXIT_CODE = 0;
    private static final String WHITESPACE = " ";
    private static final String COPY_COMMAND = "copy";
    private static final int INDEX_OF_SOURCE = 2;
    private static final int INDEX_OF_DESTINATION = 3;
    private final DataManagementConfiguration config;

    public void copy(String source, String destination) {
        StringJoiner runCommand = new StringJoiner(WHITESPACE);
        try {
            List<String> command = buildCommand(source, destination);
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            buildCensoredRunCommand(command, runCommand);
            var startTime = Instant.now();
            log.info("Copy of blob started at {}", startTime);
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
            throw new DartsException("Failed to execute azure copy - interrupted " + runCommand, ie);
        } catch (Exception e) {
            throw new DartsException("Failed to execute azure copy " + runCommand, e);
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

    private List<String> buildCommand(String source, String destination) {
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

    private void buildCensoredRunCommand(List<String> command, StringJoiner runCommand) {
        for (int index = SUCCESS_EXIT_CODE; index < command.size(); index++) {
            if (index == INDEX_OF_SOURCE) {
                runCommand.add("SOURCE");
            } else if (index == INDEX_OF_DESTINATION) {
                runCommand.add("DESTINATION");
            } else {
                runCommand.add(command.get(index));
            }

        }
    }
}
