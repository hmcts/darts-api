package uk.gov.hmcts.darts.audio.deleter.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.dets.api.DetsDataManagementApi;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExternalDetsDataStoreDeleterTest {

    @Mock
    private DetsDataManagementApi dataManagementApi;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private EodHelperMocks eodHelperMocks;
    private ExternalDetsDataStoreDeleter deleter;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks();
        deleter = new ExternalDetsDataStoreDeleter(externalObjectDirectoryRepository, dataManagementApi);
    }

    @AfterEach
    void tearDown() {
        eodHelperMocks.close();
    }

    @Test
    void deleteFromDataStore_ShouldCallDeleteBlobDataFromContainer() throws AzureDeleteBlobException {
        String location = eodHelperMocks.getDetsLocation().getDescription();
        doNothing().when(dataManagementApi).deleteBlobDataFromContainer(location);

        deleter.deleteFromDataStore(location);

        verify(dataManagementApi, times(1)).deleteBlobDataFromContainer(location);
    }

    @Test
    void deleteFromDataStore_ShouldThrowAzureDeleteBlobException() throws AzureDeleteBlobException {
        String location = eodHelperMocks.getDetsLocation().getDescription();
        doThrow(new AzureDeleteBlobException("Test exception"))
            .when(dataManagementApi).deleteBlobDataFromContainer(location);

        assertThrows(AzureDeleteBlobException.class, () -> deleter.deleteFromDataStore(location));

        verify(dataManagementApi, times(1)).deleteBlobDataFromContainer(location);
    }
}