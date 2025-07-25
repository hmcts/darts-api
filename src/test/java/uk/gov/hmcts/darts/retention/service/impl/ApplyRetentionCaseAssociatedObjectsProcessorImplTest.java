package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ApplyRetentionCaseAssociatedObjectsProcessorImplTest {

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl singleCaseProcessor;
    @InjectMocks
    private ApplyRetentionCaseAssociatedObjectsProcessorImpl applyRetentionCaseAssociatedObjectsProcessor;

    @Test
    void processApplyRetentionToCaseAssociatedObjects_onInterruptedException_shouldStopProcessingAndReturn() {
        doReturn(List.of(1, 2, 3, 4)).when(caseRepository).findIdsByIsRetentionUpdatedTrueAndRetentionRetriesLessThan(anyInt(), any());
        CourtCaseEntity caseEntity1 = mock(CourtCaseEntity.class);
        CourtCaseEntity caseEntity2 = mock(CourtCaseEntity.class);

        doReturn(Optional.ofNullable(caseEntity1)).when(caseRepository).findById(1);
        doReturn(1).when(caseEntity1).getId();
        doReturn(Optional.ofNullable(caseEntity2)).when(caseRepository).findById(2);
        doReturn(2).when(caseEntity2).getId();
        doReturn(1).when(caseEntity2).getRetentionRetries();

        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(2);

        applyRetentionCaseAssociatedObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(10);

        verify(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(1);
        verify(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(2);
        //Check to ensure that the processor stops processing after the InterruptedException
        verifyNoMoreInteractions(singleCaseProcessor);

        verify(caseEntity2).setRetentionRetries(2);
        verify(caseEntity2).setRetentionUpdated(true);
    }
}
