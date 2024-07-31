package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

@ExtendWith(MockitoExtension.class)
class InboundAnnotationTranscriptionDeleterAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;

    @Mock
    private InboundAnnotationTranscriptionDeleterProcessor armResponseFilesProcessor;

    @Mock
    private LogApi logApi;

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    @Test
    void runTask() {
        // given
        InboundAnnotationTranscriptionDeleterAutomatedTask unstructuredAnnotationTranscriptionDeleterAutomatedTask
            = new InboundAnnotationTranscriptionDeleterAutomatedTask(automatedTaskRepository,
                                                                          lockProvider,
                                                                          automatedTaskConfigurationProperties,
                                                                          armResponseFilesProcessor,
                                                                          logApi);
        // when
        unstructuredAnnotationTranscriptionDeleterAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).markForDeletion();
    }
}