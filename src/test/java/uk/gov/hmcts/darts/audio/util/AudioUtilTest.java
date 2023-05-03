package uk.gov.hmcts.darts.audio.util;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AudioUtilTest {

    @InjectMocks
    private AudioUtil audioUtil;

    @Test
    void shouldExecuteCommandsWhenCommandIsValid() throws Exception {
        assertTrue(audioUtil.execute(new CommandLine("hostname")));
    }

    @Test
    void shouldThrowExceptionWhenCommandIsInValid() {
        assertThrows(Exception.class, () -> audioUtil.execute(new CommandLine("Dummy Command")));
    }
}
