package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Slf4j
class SystemCommandExecutorIntTest extends IntegrationBase {

    @Autowired
    private SystemCommandExecutor systemCommandExecutor;
    
    @Test
    void executeWithFfmpegHelpCommand() throws ExecutionException, InterruptedException {

        String command = "ffmpeg -h";
        CommandLine commandLine = CommandLine.parse(command);
        boolean result = systemCommandExecutor.execute(commandLine);
        assertTrue(result);
    }

    @Test
    void executeWithFfmpegVersionCommand() throws ExecutionException, InterruptedException {
        String command = "ffmpeg -version";
        CommandLine commandLine = CommandLine.parse(command);
        boolean result = systemCommandExecutor.execute(commandLine);
        assertTrue(result);
    }


    @Test
    void executeWithFfmpegAudioCommand() {
        String command = "ffmpeg -i original0.mp2 -i original1.mp2"
            + " -filter_complex [0:a][1:a]concat=n=2:v=0:a=1 audio.mp2";
        CommandLine commandLine = CommandLine.parse(command);
        assertThrows(ExecutionException.class, () -> systemCommandExecutor.execute(commandLine));

    }

    @Test
    void executeWithFfmpegHelpCommandWithFullPath() throws ExecutionException, InterruptedException {

        String command = "/usr/bin/ffmpeg -h";
        CommandLine commandLine = CommandLine.parse(command);
        boolean result = systemCommandExecutor.execute(commandLine);
        assertTrue(result);
    }

    @Test
    void executeWithFfmpegVersionCommandWithFullPath() throws ExecutionException, InterruptedException {
        String command = "/usr/bin/ffmpeg -version";
        CommandLine commandLine = CommandLine.parse(command);
        boolean result = systemCommandExecutor.execute(commandLine);
        assertTrue(result);
    }


    @Test
    void executeWithFfmpegAudioCommandWithFullPath() {
        String command = "/usr/bin/ffmpeg -i /path/to/audio/original0.mp2 -i /path/to/audio/original1.mp2"
            + " -filter_complex [0:a][1:a]concat=n=2:v=0:a=1 /path/to/output/audio.mp2";
        CommandLine commandLine = CommandLine.parse(command);
        assertThrows(ExecutionException.class, () -> systemCommandExecutor.execute(commandLine));

    }
}
