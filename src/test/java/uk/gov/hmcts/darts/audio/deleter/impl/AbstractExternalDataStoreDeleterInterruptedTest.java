package uk.gov.hmcts.darts.audio.deleter.impl;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.AbstractExternalDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.DoNotUseThreads"})
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
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            externalObjectDirectoryEntity.setStatus(eodHelperMocks.getMarkForDeletionStatus());
            externalObjectDirectoryEntity.setExternalLocation(eodHelperMocks.getInboundLocation().getDescription());
            return List.of(externalObjectDirectoryEntity);
        }
    }

    @AfterEach
    void clearInterruptFlag() {
        // Ensure the interrupt flag doesn't bleed into other tests
        Thread.interrupted(); // clears the flag
    }

    @Test
    void delete_shouldRethrowInterruptedException() throws IllegalAccessException {
        var deleter = new TestDeleter(repository);
        // Inject the mock entityDeleter via reflection (safe, no setAccessible)
        ExternalDataStoreEntityDeleter entityDeleter = Mockito.mock(ExternalDataStoreEntityDeleter.class);
        FieldUtils.writeField(deleter, "entityDeleter", entityDeleter, true);
        when(entityDeleter.deleteEntity(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return ((TestDeleter) args[0]).deleteInternal((ExternalObjectDirectoryEntity) args[1]);
        });

        Thread.currentThread().interrupt();

        assertThrows(InterruptedException.class, () -> deleter.delete(1));

        verifyNoInteractions(repository);
    }
}
