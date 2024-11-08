package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.UnstructuredTranscriptionAndAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class UnstructuredAnnotationTranscriptionDeleterAutomatedTaskTest {
    @Mock
    private UnstructuredTranscriptionAndAnnotationDeleterProcessor armResponseFilesProcessor;

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
        UnstructuredAnnotationTranscriptionDeleterAutomatedTask unstructuredAnnotationTranscriptionDeleterAutomatedTask
            = spy(new UnstructuredAnnotationTranscriptionDeleterAutomatedTask(automatedTaskRepository,
                                                                          automatedTaskConfigurationProperties,
                                                                          armResponseFilesProcessor,
                                                                          logApi,
                                                                          lockService));
        doReturn(123).when(unstructuredAnnotationTranscriptionDeleterAutomatedTask).getAutomatedTaskBatchSize();
        // when
        unstructuredAnnotationTranscriptionDeleterAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).markForDeletion(123);
        Mockito.verify(unstructuredAnnotationTranscriptionDeleterAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();
    }
}