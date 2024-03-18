package uk.gov.hmcts.darts.audio.component.impl;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SystemCommandExecutorImplTest {

    @InjectMocks
    private SystemCommandExecutorImpl systemCommandExecutor;

    @Test
    void shouldExecuteCommandsWhenCommandIsValid() throws Exception {
        assertTrue(systemCommandExecutor.execute(new CommandLine("hostname")));
    }

    @Test
    void shouldThrowExceptionWhenCommandIsInValid() {
        assertThrows(Exception.class, () -> systemCommandExecutor.execute(new CommandLine("Dummy Command")));
    }

    @Test
    void executeWithFfmpegHelpCommand() throws ExecutionException, InterruptedException {

        String command = "ffmpeg -h";
        CommandLine commandLine = CommandLine.parse(command);
        boolean result = systemCommandExecutor.execute(commandLine);
        assertTrue(result);
    }

}
