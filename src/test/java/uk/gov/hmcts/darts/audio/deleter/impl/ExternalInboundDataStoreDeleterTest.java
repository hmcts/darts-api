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
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalInboundDataStoreDeleterTest {

    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private EodHelperMocks eodHelperMocks;
    private ExternalInboundDataStoreDeleter deleter;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks();
        this.deleter = spy(new ExternalInboundDataStoreDeleter(
            externalObjectDirectoryRepository,
            dataManagementApi
        ));

    }

    @AfterEach
    void tearDown() {
        eodHelperMocks.close();
    }

    @Test
    void deleteFromDataStore_ShouldThrowAzureDeleteBlobException() throws AzureDeleteBlobException {

        TestExternalObjectDirectoryEntity testEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .transcriptionDocumentEntity(PersistableFactory.getTranscriptionDocument()
                                             .someMinimalBuilder()
                                             .build())
            .externalLocation(eodHelperMocks.getInboundLocation().getDescription())
            .status(eodHelperMocks.getMarkForDeletionStatus())
            .id(1L)
            .build();

        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(
            eodHelperMocks.getInboundLocation(),
            eodHelperMocks.getMarkForDeletionStatus(),
            org.springframework.data.domain.Limit.of(100)
        )).thenReturn(List.of(testEod));

        doThrow(new AzureDeleteBlobException("Test exception"))
            .when(dataManagementApi).deleteBlobDataFromInboundContainer(anyString());

        assertNotNull(deleter.delete(100));
    }

    @Test
    void deleteFromDataStore_ShouldReturnTrue() throws AzureDeleteBlobException {

        TestExternalObjectDirectoryEntity testEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .transcriptionDocumentEntity(PersistableFactory.getTranscriptionDocument()
                                             .someMinimalBuilder()
                                             .build())
            .externalLocation(eodHelperMocks.getInboundLocation().getDescription())
            .status(eodHelperMocks.getMarkForDeletionStatus())
            .id(1L)
            .build();

        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(
            eodHelperMocks.getInboundLocation(),
            eodHelperMocks.getMarkForDeletionStatus(),
            org.springframework.data.domain.Limit.of(100)
        )).thenReturn(List.of(testEod));

        doNothing().when(dataManagementApi).deleteBlobDataFromInboundContainer(anyString());

        var result = deleter.delete(100);

        assertNotNull(result);
    }
    
}