package uk.gov.hmcts.darts.common.util;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.DoNotUseThreads")
class CommandRunnerTest {

    @Test
    void callSuccessWhenCorrectCommandIsSent() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = executorService.submit(new CommandRunner(new CommandLine("hostname")));
        String threadResponse = future.get();
        assertEquals("0", threadResponse);
    }

    @Test
    void callFailWhenWrongCommandIsSent() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = executorService.submit(new CommandRunner(new CommandLine("Dummy Command")));
        assertThrows(ExecutionException.class, future::get);
    }
}
