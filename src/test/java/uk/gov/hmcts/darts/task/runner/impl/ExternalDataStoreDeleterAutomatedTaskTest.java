package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.ExternalDataStoreDeleter;

@ExtendWith(MockitoExtension.class)
class ExternalDataStoreDeleterAutomatedTaskTest {

    @Mock
    ExternalDataStoreDeleter deleter;

    @Mock
    LockProvider provider;

    @Test
    void runTask() {
        ExternalDataStoreDeleterAutomatedTask externalDataStoreDeleterAutomatedTask =
            new ExternalDataStoreDeleterAutomatedTask(null, provider, null, deleter);

        externalDataStoreDeleterAutomatedTask.runTask();

        Mockito.verify(deleter, Mockito.times(1)).delete();
    }
}
