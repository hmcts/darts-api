package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.concurrent.ExecutionException;

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
}
