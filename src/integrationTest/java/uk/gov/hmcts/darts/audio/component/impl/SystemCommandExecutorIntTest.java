package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.concurrent.ExecutionException;

@Slf4j
@Profile({"intAtsTest"})
class SystemCommandExecutorIntTest extends IntegrationBase {

    @Autowired
    private SystemCommandExecutorImpl systemCommandExecutor;

    @Test
    void executeWithFfmpegHelpCommand() throws ExecutionException, InterruptedException {
        String command = "ffmpeg -h";
        CommandLine commandLine = CommandLine.parse(command);
        systemCommandExecutor.execute(commandLine);
    }
}
