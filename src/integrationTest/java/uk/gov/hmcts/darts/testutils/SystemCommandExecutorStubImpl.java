package uk.gov.hmcts.darts.testutils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * This class is a test implementation of SystemCommandExecutor, intended to mimic the side effects of specified system
 * commands without actually executing those commands. This is useful for scenarios where the executable is not present
 * on the host system but the side effects of the command are required by subsequent test steps.
 */
@Component
@Slf4j
@Profile("intTest")
public class SystemCommandExecutorStubImpl implements SystemCommandExecutor {

    public static final int DUMMY_FILE_SIZE_BYTES = 10_240;

    @Override
    public Boolean execute(CommandLine command) {
        log.warn("### This implementation is intended only for integration tests. If you see this log message elsewhere"
                     + " you should ask questions! ###");

        String executable = command.getExecutable();
        List<String> arguments = Arrays.asList(command.getArguments());
        if (executable.contains("ffmpeg")) {
            // The output path is assumed to always be the final argument
            var fileOutputPath = Path.of(arguments.get(arguments.size() - 1));
            if (isFfmpegConcatOperation(arguments)) {
                log.debug("Stubbing ffmpeg concat operation");
            } else if (isFfmpegTrimOperation(arguments)) {
                log.debug("Stubbing ffmpeg trim operation");
            }
            writeDummyFile(fileOutputPath);
            return true;
        }

        return false;
    }

    private boolean isFfmpegConcatOperation(List<String> arguments) {
        return arguments.stream()
            .anyMatch(argument -> argument.contains("concat="));
    }

    private boolean isFfmpegTrimOperation(List<String> arguments) {
        return arguments.contains("-ss") && arguments.contains("-to");
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private void writeDummyFile(Path path) {
        log.debug("Writing dummy file to: {}", path);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, new byte[DUMMY_FILE_SIZE_BYTES]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
