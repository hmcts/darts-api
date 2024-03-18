package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.concurrent.ExecutionException;

@Import(SystemCommandExecutorImpl.class)
@SpringBootTest
@Slf4j
@ActiveProfiles("intAtsTest")
class SystemCommandExecutorIntTest extends IntegrationBase {

    @Autowired
    private SystemCommandExecutor systemCommandExecutor;

    @Test
    void executeWithFfmpegHelpCommand() throws ExecutionException, InterruptedException {

        String command = "ffmpeg -h";
        CommandLine commandLine = CommandLine.parse(command);
        systemCommandExecutor.execute(commandLine);
    }
}
