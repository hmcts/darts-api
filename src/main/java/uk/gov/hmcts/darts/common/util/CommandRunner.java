package uk.gov.hmcts.darts.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Slf4j
public class CommandRunner implements Callable<String> {

    private final CommandLine systemCommand;

    public CommandRunner(CommandLine systemCommand) {
        this.systemCommand = systemCommand;
    }

    @Override
    public String call() throws ExecutionException {
        try {
            Executor executor = new DefaultExecutor();
            return String.valueOf(executor.execute(systemCommand));
        } catch (Exception e) {
            log.error("Execution of System Command Failed" + e);
            throw new ExecutionException("Execution of System Command Failed", e);
        }
    }
}
