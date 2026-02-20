package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDetsDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupDetsDataServiceImplTest {

    private EodHelperMocks eodHelperMocks;
    private static final String EXTERNAL_LOCATION = "1234";

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private ExternalDetsDataStoreDeleter detsDataStoreDeleter;

    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;

    @Mock
    private CleanupDetsDataServiceImpl.CleanupDetsEodTransactionalService cleanupDetsEodTransactionalService;

    private CleanupDetsDataServiceImpl cleanupDetsDataService;

    private OffsetDateTime now;
    private ExternalObjectDirectoryEntity detsEod;
    private ObjectStateRecordEntity objectStateRecordEntity;


    @BeforeEach
    void setUp() {
        now = OffsetDateTime.parse("2025-01-01T10:00:00Z");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(now);
        eodHelperMocks = new EodHelperMocks();
        cleanupDetsDataService = new CleanupDetsDataServiceImpl(
            externalObjectDirectoryRepository,
            currentTimeHelper,
            cleanupDetsEodTransactionalService
        );

        detsEod = new ExternalObjectDirectoryEntity();
        detsEod.setId(1L);
        detsEod.setExternalLocation(EXTERNAL_LOCATION);
        detsEod.setOsrUuid(200L);

        objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(200L);
        objectStateRecordEntity.setFlagFileDetsCleanupStatus(false);
        objectStateRecordEntity.setDateFileDetsCleanup(null);
    }

    @Test
    void cleanupDetsData_callsRepositoryAndTransactionalServiceForEachId() {
        // given
        int batchSize = 10;
        Duration inArm = Duration.ofDays(7);
        var lastModifiedBefore = now.minus(inArm);
        when(externalObjectDirectoryRepository.findEodIdsInOtherStorageLastModifiedBefore(
            any(), any(), any(), eq(lastModifiedBefore), Limit.of(eq(batchSize))
        )).thenReturn(List.of(1L, 2L, 3L));

        // when
        cleanupDetsDataService.cleanupDetsData(batchSize, inArm);

        // then
        verify(externalObjectDirectoryRepository).findEodIdsInOtherStorageLastModifiedBefore(
            any(), any(), any(), eq(lastModifiedBefore), Limit.of(batchSize)
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
        when(externalObjectDirectoryRepository.findEodIdsInOtherStorageLastModifiedBefore(
            anyInt(), anyInt(), anyInt(), any(), any()
        )).thenReturn(List.of());

        // when
        cleanupDetsDataService.cleanupDetsData(10, Duration.ofDays(7));

        // then
        verify(cleanupDetsEodTransactionalService, never()).cleanupDetsEod(anyLong());
    }

    @Test
    void cleanupDetsEod_whenRecordMissing_logsAndReturns() throws AzureDeleteBlobException {
        // given
        when(externalObjectDirectoryRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        cleanupDetsDataService.cleanupDetsData(999, Duration.ofDays(7));

        // then
        verify(detsDataStoreDeleter, never()).deleteFromDataStore(anyString());
        verify(externalObjectDirectoryRepository, never()).deleteById(anyLong());
        verify(objectStateRecordRepository, never()).save(any());
    }

    @Test
    void cleanupDetsEod_successfulPath_deletesFromStore_andMarksOsr() throws AzureDeleteBlobException {
        // given
        when(externalObjectDirectoryRepository.findById(100L)).thenReturn(Optional.of(detsEod));
        when(objectStateRecordRepository.findById(200L)).thenReturn(Optional.of(objectStateRecordEntity));

        // when
        cleanupDetsDataService.cleanupDetsData(100, Duration.ofDays(7));

        // then
        verify(detsDataStoreDeleter).deleteFromDataStore(EXTERNAL_LOCATION);
        verify(externalObjectDirectoryRepository).deleteById(100L);
        verify(objectStateRecordRepository).findById(200L);
        verify(objectStateRecordRepository).save(objectStateRecordEntity);

        assertThat(objectStateRecordEntity.getFlagFileDetsCleanupStatus()).isTrue();
        assertThat(objectStateRecordEntity.getDateFileDetsCleanup()).isEqualTo(now);
    }

    @Test
    void cleanupDetsEod_whenAzureDeleteFails_doesNotDeleteOrSave() throws AzureDeleteBlobException {
        // given
        when(externalObjectDirectoryRepository.findById(100L)).thenReturn(Optional.of(detsEod));
        doThrow(new AzureDeleteBlobException("boom", null)).when(detsDataStoreDeleter)
            .deleteFromDataStore(EXTERNAL_LOCATION);

        // when
        cleanupDetsDataService.cleanupDetsData(100, Duration.ofDays(7));

        // then
        verify(externalObjectDirectoryRepository, never()).deleteById(anyLong());
        verify(objectStateRecordRepository, never()).save(any());
    }

    @Test
    void cleanupDetsEod_whenGenericFailureAfterDeleteFromStore_doesNotSaveOsr() throws AzureDeleteBlobException {
        // given
        when(externalObjectDirectoryRepository.findById(100L)).thenReturn(Optional.of(detsEod));
        // delete from store ok
        doNothing().when(detsDataStoreDeleter).deleteFromDataStore(anyString());
        // fail on repository delete to hit generic catch
        doThrow(new RuntimeException("db down")).when(externalObjectDirectoryRepository).deleteById(100L);

        // when
        cleanupDetsDataService.cleanupDetsData(100, Duration.ofDays(7));

        // then
        verify(objectStateRecordRepository, never()).save(any());
    }

    @AfterEach
    void close() {
        eodHelperMocks.close();
    }
}
