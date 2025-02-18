package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;
import uk.gov.hmcts.darts.common.util.CommandRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
public class SystemCommandExecutorImpl implements SystemCommandExecutor {

    @SuppressWarnings("PMD.DoNotUseThreads")
    @Override
    public Boolean execute(CommandLine command) throws ExecutionException, InterruptedException {
        try {
            log.debug("Command line {} - {}", command.getExecutable(), StringUtils.join(command.getArguments(), " "));
            Future<String> future;
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                future = executor.submit(new CommandRunner(command));
            }
            future.get();
        } catch (ExecutionException e) {
            log.error("Failed to execute system command {} due to ", command, e);
            throw e;
        }
        return Boolean.TRUE;
    }
}
