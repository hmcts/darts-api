package uk.gov.hmcts.darts.util;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureCopyUtil {

    private final DataManagementConfiguration config;

    public void copy(String source, String destination) {
        StringBuilder runCommand = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(config.getAzCopyExecutable());
            command.add("copy");
            command.add(source);
            command.add(destination);
            if (StringUtils.isNotEmpty(config.getAzCopyPreserveAccessTier())) {
                command.add(config.getAzCopyPreserveAccessTier());
            }
            if (StringUtils.isNotEmpty(config.getAzCopyLogLevel())) {
                command.add(config.getAzCopyLogLevel());
            }
            if (StringUtils.isNotEmpty(config.getAzCopyCheckLength())) {
                command.add(config.getAzCopyCheckLength());
            }
            if (StringUtils.isNotEmpty(config.getAzCopyOutputLevel())) {
                command.add(config.getAzCopyOutputLevel());
            }
            builder.command(command);

            buildCensoredRunCommand(command, runCommand);
            var startTime = Instant.now();
            log.info("Copy of blob started at {} - {}", startTime, runCommand);
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
                    runCommand,
                    result);
                log.error(errorMessage);
                throw new DartsException(errorMessage);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DartsException("Failed to execute azure copy - interrupted " + runCommand, ie);
        } catch (Exception e) {
            throw new DartsException("Failed to execute azure copy " + runCommand, e);
        }
    }

    private static void buildCensoredRunCommand(List<String> command, StringBuilder runCommand) {
        for (int index = 0; index < command.size(); index++) {
            if (index == 2) {
                runCommand.append("source ").append(" ");
            } else if (index == 3) {
                runCommand.append("destination ").append(" ");
            } else {
                runCommand.append(command.get(index)).append(" ");
            }
        }
    }
}
