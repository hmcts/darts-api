package uk.gov.hmcts.darts.audio.deleter.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.AbstractExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AbstractExternalDataStoreDeleterInterruptedTest {

    @Mock
    private ExternalObjectDirectoryRepository repository;

    private static EodHelperMocks eodHelperMocks;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks();
    }

    @AfterEach
    void tearDown() {
        eodHelperMocks.close();
    }

    // Minimal concrete for testing the base-class behaviour
    private static class TestDeleter extends AbstractExternalDataStoreDeleter<ExternalObjectDirectoryEntity, ExternalObjectDirectoryRepository> {
        TestDeleter(ExternalObjectDirectoryRepository repository) {
            super(repository);
        }

        @Override
        protected void deleteFromDataStore(String externalLocation) throws Exception {
            // This will throw InterruptedException immediately if the thread is already interrupted
            Thread.sleep(1_000);
        }

        @Override
        protected Collection<ExternalObjectDirectoryEntity> findItemsToDelete(int batchSize) {
            ExternalObjectDirectoryEntity e = new ExternalObjectDirectoryEntity();
            e.setStatus(eodHelperMocks.getMarkForDeletionStatus());
            e.setExternalLocation(eodHelperMocks.getInboundLocation().getDescription());
            return List.of(e);
        }
    }

    @AfterEach
    void clearInterruptFlag() {
        // Ensure the interrupt flag doesn't bleed into other tests
        Thread.interrupted(); // clears the flag
    }

    @Test
    void delete_shouldRethrowInterruptedException() {
        var deleter = new TestDeleter(repository);

        Thread.currentThread().interrupt();

        // Should rethrow the InterruptedException
        assertThrows(InterruptedException.class, () -> deleter.delete(1));

        verifyNoInteractions(repository);
    }
}
