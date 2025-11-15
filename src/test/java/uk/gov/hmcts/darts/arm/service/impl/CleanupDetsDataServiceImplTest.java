package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupDetsDataServiceImplTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private CleanupDetsDataServiceImpl.CleanupDetsEodTransactionalService cleanupDetsEodTransactionalService;

    private CleanupDetsDataServiceImpl cleanupDetsDataService;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.parse("2025-01-01T10:00:00Z");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(now);

        cleanupDetsDataService = new CleanupDetsDataServiceImpl(
            externalObjectDirectoryRepository,
            currentTimeHelper,
            cleanupDetsEodTransactionalService
        );
    }

    @Test
    void cleanupDetsData_callsRepositoryAndTransactionalServiceForEachId() {
        // given
        int batchSize = 10;
        Duration inArm = Duration.ofDays(7);
        var lastModifiedBefore = now.minus(inArm);
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorageLastModifiedBefore(
            any(), any(), any(), eq(lastModifiedBefore), eq(batchSize)
        )).thenReturn(List.of(1L, 2L, 3L));

        // when
        cleanupDetsDataService.cleanupDetsData(batchSize, inArm);

        // then
        verify(externalObjectDirectoryRepository).findEodsNotInOtherStorageLastModifiedBefore(
            any(), any(), any(), eq(lastModifiedBefore), eq(batchSize)
        );
        verify(cleanupDetsEodTransactionalService, times(3)).cleanupDetsEod(anyLong());
        verify(cleanupDetsEodTransactionalService).cleanupDetsEod(1L);
        verify(cleanupDetsEodTransactionalService).cleanupDetsEod(2L);
        verify(cleanupDetsEodTransactionalService).cleanupDetsEod(3L);
        verifyNoMoreInteractions(cleanupDetsEodTransactionalService);
    }

    @Test
    void cleanupDetsData_whenNoIdsFound_doesNothingFurther() {
        // given
        when(externalObjectDirectoryRepository.findEodsNotInOtherStorageLastModifiedBefore(
            any(), any(), any(), any(), anyInt()
        )).thenReturn(List.of());

        // when
        cleanupDetsDataService.cleanupDetsData(10, Duration.ofDays(7));

        // then
        verify(cleanupDetsEodTransactionalService, never()).cleanupDetsEod(anyLong());
    }
}
