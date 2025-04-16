package uk.gov.hmcts.darts.audio.component;

import org.apache.commons.exec.CommandLine;

import java.util.concurrent.ExecutionException;

@FunctionalInterface
public interface SystemCommandExecutor {
    Boolean execute(CommandLine command) throws ExecutionException, InterruptedException;
}
