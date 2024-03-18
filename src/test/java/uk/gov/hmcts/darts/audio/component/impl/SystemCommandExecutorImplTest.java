package uk.gov.hmcts.darts.audio.component.impl;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
