package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundAnnotationTranscriptionDeleterProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class InboundAnnotationTranscriptionDeleterAutomatedTaskTest {

    @Mock
    private InboundAnnotationTranscriptionDeleterProcessor armResponseFilesProcessor;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    @Test
    void runTask() {
        // given
        InboundAnnotationTranscriptionDeleterAutomatedTask unstructuredAnnotationTranscriptionDeleterAutomatedTask
            = new InboundAnnotationTranscriptionDeleterAutomatedTask(automatedTaskRepository,
                                                                     automatedTaskConfigurationProperties,
                                                                     armResponseFilesProcessor,
                                                                     logApi,
                                                                     lockService);
        // when
        unstructuredAnnotationTranscriptionDeleterAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).markForDeletion(anyInt());
    }
}