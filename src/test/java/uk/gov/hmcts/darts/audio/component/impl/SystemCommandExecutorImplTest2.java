package uk.gov.hmcts.darts.audio.component.impl;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SystemCommandExecutorImplTest2 {

    private SystemCommandExecutor systemCommandExecutor;

    @BeforeEach
    void setUp() {
        systemCommandExecutor = new SystemCommandExecutorImpl();
    }

    @Test
    void executeWithFfmpegHelpCommand() throws ExecutionException, InterruptedException {

        String command = "ffmpeg -h";
        CommandLine commandLine = CommandLine.parse(command);
        boolean result = systemCommandExecutor.execute(commandLine);
        assertTrue(result);
    }

    @Test
    void executeWithFfmpegAudioCommand() {
        String command = "ffmpeg -i /path/to/audio/original0.mp2 -i /path/to/audio/original1.mp2"
            + " -filter_complex [0:a][1:a]concat=n=2:v=0:a=1 /path/to/output/audio.mp2";
        CommandLine commandLine = CommandLine.parse(command);
        assertThrows(ExecutionException.class, () -> systemCommandExecutor.execute(commandLine));

    }
}
