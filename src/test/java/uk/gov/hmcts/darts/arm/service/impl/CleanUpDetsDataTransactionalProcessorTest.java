package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanUpDetsDataTransactionalProcessorTest {

    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;

    @Test
    void callDetsCleanUpStoredProcedure_shouldSetTimeoutBeforeCallingProcedure_whenProcessingChunk() {
        CleanUpDetsDataProcessorImpl.CleanUpDetsDataTransactionalProcessor processor =
            new CleanUpDetsDataProcessorImpl.CleanUpDetsDataTransactionalProcessor(objectStateRecordRepository);
        OffsetDateTime minimumStoredAge = OffsetDateTime.parse("2026-11-01T00:00:00Z");
        List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> results =
            List.of(new CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse(1L, "dets-location"));
        when(objectStateRecordRepository.cleanUpDetsDataProcedure(100, minimumStoredAge)).thenReturn(results);

        assertThat(processor.callDetsCleanUpStoredProcedure(100, minimumStoredAge, 3600)).isSameAs(results);

        InOrder ordered = inOrder(objectStateRecordRepository);
        ordered.verify(objectStateRecordRepository).setCleanupStatementTimeout("3600s");
        ordered.verify(objectStateRecordRepository).cleanUpDetsDataProcedure(100, minimumStoredAge);
        ordered.verifyNoMoreInteractions();
    }
}
