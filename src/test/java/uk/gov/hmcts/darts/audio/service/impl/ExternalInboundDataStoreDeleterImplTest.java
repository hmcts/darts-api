package uk.gov.hmcts.darts.audio.service.impl;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalInboundDataStoreDeleterImplTest {
    private ExternalInboundDataStoreDeleter deleter;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private ObjectRecordStatusEntity markedForDeletionStatus;

    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    private EodHelperMocks eodHelperMocks;

    @BeforeEach
    public void setUp() {
        eodHelperMocks = new EodHelperMocks();
        mockStatus();
        List<ExternalObjectDirectoryEntity> inboundData = createInboundData();
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(
            eodHelperMocks.getInboundLocation(),
            eodHelperMocks.getMarkForDeletionStatus(),
            Limit.of(100)))
            .thenReturn(inboundData);

        this.deleter = new ExternalInboundDataStoreDeleter(
            externalObjectDirectoryRepository,
            dataManagementApi
        );
    }

    @AfterEach
    public void afterEach() {
        eodHelperMocks.close();
    }

    private void mockStatus() {
        markedForDeletionStatus = new ObjectRecordStatusEntity();
        markedForDeletionStatus.setId(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
    }

    @Test
    void deleteFromInboundDatastore() {
        Collection<ExternalObjectDirectoryEntity> deletedItems = deleter.delete(100);

        assertThat(
            deletedItems,
            containsInAnyOrder(
                allOf(
                    Matchers.hasProperty("id", is(1))
                ),
                allOf(
                    Matchers.hasProperty("id", is(2))
                )
            )
        );
        assertEquals(2, deletedItems.size());
        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndObjectStatus(
            eodHelperMocks.getInboundLocation(),
            eodHelperMocks.getMarkForDeletionStatus(),
            Limit.of(100));
    }

    @Test
    void deleteFromInboundDatastoreShouldNotThrowAzureDeleteBlobException() throws AzureDeleteBlobException {
        doThrow(AzureDeleteBlobException.class).when(dataManagementApi).deleteBlobDataFromInboundContainer(any(String.class));

        assertDoesNotThrow(() -> deleter.delete(100));
        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndObjectStatus(
            eodHelperMocks.getInboundLocation(),
            eodHelperMocks.getMarkForDeletionStatus(),
            Limit.of(100));
    }

    private List<ExternalObjectDirectoryEntity> createInboundData() {
        ExternalObjectDirectoryEntity inboundData1 = new ExternalObjectDirectoryEntity();
        inboundData1.setStatus(markedForDeletionStatus);
        inboundData1.setExternalLocationType(new ExternalLocationTypeEntity());
        inboundData1.setExternalLocation(UUID.randomUUID().toString());
        inboundData1.setId(1);
        inboundData1.setVerificationAttempts(1);

        ExternalObjectDirectoryEntity inboundData2 = new ExternalObjectDirectoryEntity();
        inboundData2.setStatus(markedForDeletionStatus);
        inboundData2.setExternalLocationType(new ExternalLocationTypeEntity());
        inboundData2.setExternalLocation(UUID.randomUUID().toString());
        inboundData2.setId(2);
        inboundData2.setVerificationAttempts(2);

        List<ExternalObjectDirectoryEntity> inboundDataList = new ArrayList<>();
        inboundDataList.add(inboundData1);
        inboundDataList.add(inboundData2);
        return inboundDataList;
    }

}
