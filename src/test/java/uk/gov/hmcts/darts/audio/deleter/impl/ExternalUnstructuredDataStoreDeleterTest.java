package uk.gov.hmcts.darts.audio.deleter.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalUnstructuredDataStoreDeleterTest {

    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private EodHelperMocks eodHelperMocks;

    private ExternalUnstructuredDataStoreDeleter deleter;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks();
        deleter = new ExternalUnstructuredDataStoreDeleter(externalObjectDirectoryRepository, dataManagementApi);
    }

    @AfterEach
    void tearDown() {
        eodHelperMocks.close();
    }

    @Test
    void deleteFromDataStore_ShouldCallDeleteBlobDataFromUnstructuredContainer() throws Exception {
        String location = eodHelperMocks.getUnstructuredLocation().getDescription();
        doNothing().when(dataManagementApi).deleteBlobDataFromUnstructuredContainer(location);

        deleter.deleteFromDataStore(location);

        verify(dataManagementApi, times(1)).deleteBlobDataFromUnstructuredContainer(location);
    }

    @Test
    void findItemsToDelete_ShouldReturnItemsFromRepository() {
        ExternalObjectDirectoryEntity entity = mock(ExternalObjectDirectoryEntity.class);
        List<ExternalObjectDirectoryEntity> expected = List.of(entity);

        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(
            eodHelperMocks.getUnstructuredLocation(),
            eodHelperMocks.getMarkForDeletionStatus(),
            Limit.of(5)
        )).thenReturn(expected);

        var result = deleter.findItemsToDelete(5);

        assertEquals(expected, result);
    }
}