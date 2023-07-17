package uk.gov.hmcts.darts.audio.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;
import uk.gov.hmcts.darts.common.util.CommandRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
@Profile("!intTest")
public class SystemCommandExecutorImpl implements SystemCommandExecutor {

    @SuppressWarnings("PMD.DoNotUseThreads")
    @Override
    public Boolean execute(CommandLine command) throws ExecutionException, InterruptedException {
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(new CommandRunner(command));
            future.get();
        } catch (ExecutionException e) {
            log.error("Failed to execute system command {} due to ", command, e);
            throw e;
        }
        return Boolean.TRUE;
    }
}
