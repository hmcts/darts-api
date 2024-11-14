package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundTranscriptionAnnotationDeleterProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.config.InboundTranscriptionAnnotationDeleterAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class InboundTranscriptionAnnotationDeleterAutomatedTaskTest {

    @Mock
    private InboundTranscriptionAnnotationDeleterProcessor armResponseFilesProcessor;

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
        InboundTranscriptionAnnotationDeleterAutomatedTask unstructuredAnnotationTranscriptionDeleterAutomatedTask
            = new InboundTranscriptionAnnotationDeleterAutomatedTask(automatedTaskRepository,
                                                                     mock(InboundTranscriptionAnnotationDeleterAutomatedTaskConfig.class),
                                                                     armResponseFilesProcessor,
                                                                     logApi,
                                                                     lockService);
        // when
        unstructuredAnnotationTranscriptionDeleterAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).markForDeletion(anyInt());
    }
}