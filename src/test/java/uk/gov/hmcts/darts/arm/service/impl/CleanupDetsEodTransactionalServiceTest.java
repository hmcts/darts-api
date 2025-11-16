package uk.gov.hmcts.darts.arm.service.impl;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDetsDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupDetsEodTransactionalServiceTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private ExternalDetsDataStoreDeleter detsDataStoreDeleter;
    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;

    private CleanupDetsDataServiceImpl.CleanupDetsEodTransactionalService cleanupDetsEodTransactionalService;

    @Captor
    private ArgumentCaptor<ObjectStateRecordEntity> osrCaptor;

    private final OffsetDateTime now = OffsetDateTime.parse("2025-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        lenient().when(currentTimeHelper.currentOffsetDateTime()).thenReturn(now);
        cleanupDetsEodTransactionalService = new CleanupDetsDataServiceImpl.CleanupDetsEodTransactionalService(
            externalObjectDirectoryRepository,
            currentTimeHelper,
            detsDataStoreDeleter,
            objectStateRecordRepository
        );
    }

    @Test
    void cleanupDetsEod_whenRecordNotFound_logsWarningAndReturns() {
        when(externalObjectDirectoryRepository.findById(1L)).thenReturn(Optional.empty());

        cleanupDetsEodTransactionalService.cleanupDetsEod(1L);

        verify(externalObjectDirectoryRepository).findById(1L);
        verifyNoMoreInteractions(detsDataStoreDeleter, objectStateRecordRepository);
    }

    @Test
    void cleanupDetsEod_whenDeleteSucceeds_updatesStateRecord() throws AzureDeleteBlobException {
        var detsEod = createExternalObjectDirectoryEntity();
        when(externalObjectDirectoryRepository.findById(1L)).thenReturn(Optional.of(detsEod));

        var osr = mock(ObjectStateRecordEntity.class);
        when(objectStateRecordRepository.findById(99L)).thenReturn(Optional.of(osr));

        cleanupDetsEodTransactionalService.cleanupDetsEod(1L);

        verify(detsDataStoreDeleter).deleteFromDataStore("location");
        verify(externalObjectDirectoryRepository).deleteById(1L);
        verify(objectStateRecordRepository).findById(99L);
        verify(osr).setFlagFileDetsCleanupStatus(true);
        verify(osr).setDateFileDetsCleanup(now);
        verify(objectStateRecordRepository).save(osr);
    }

    private static @NotNull ExternalObjectDirectoryEntity createExternalObjectDirectoryEntity() {
        var detsEod = new ExternalObjectDirectoryEntity();
        detsEod.setExternalLocation("location");
        detsEod.setOsrUuid(99L);
        detsEod.setId(1L);
        return detsEod;
    }

    @Test
    void cleanupDetsEod_whenDeleteThrowsAzureException_logsError() throws AzureDeleteBlobException {
        var detsEod = createExternalObjectDirectoryEntity();
        when(externalObjectDirectoryRepository.findById(1L)).thenReturn(Optional.of(detsEod));

        doThrow(new AzureDeleteBlobException("fail")).when(detsDataStoreDeleter).deleteFromDataStore("location");

        cleanupDetsEodTransactionalService.cleanupDetsEod(1L);

        verify(detsDataStoreDeleter).deleteFromDataStore("location");
        // No further calls after AzureDeleteBlobException
        verify(externalObjectDirectoryRepository, never()).deleteById(anyLong());
        verify(objectStateRecordRepository, never()).findById(anyLong());
    }

    @Test
    void cleanupDetsEod_whenOtherException_logsError() throws AzureDeleteBlobException {
        var detsEod = createExternalObjectDirectoryEntity();
        when(externalObjectDirectoryRepository.findById(1L)).thenReturn(Optional.of(detsEod));

        doThrow(new RuntimeException("fail")).when(detsDataStoreDeleter).deleteFromDataStore("location");

        cleanupDetsEodTransactionalService.cleanupDetsEod(1L);

        verify(detsDataStoreDeleter).deleteFromDataStore("location");
        // No further calls after exception
        verify(externalObjectDirectoryRepository, never()).deleteById(anyLong());
        verify(objectStateRecordRepository, never()).findById(anyLong());
    }
}

