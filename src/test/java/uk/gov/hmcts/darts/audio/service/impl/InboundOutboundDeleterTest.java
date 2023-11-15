package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.InboundUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundOutboundDeleterTest {

    @Mock
    private DataManagementDao dataManagementDao;
    private InboundUnstructuredDataStoreDeleter deleter;
    @Mock
    private BlobContainerClient blobContainerClient;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private BlobClient blobClient;
    @Mock
    private ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    private ObjectDirectoryStatusEntity markedForDeletionStatus;
    private ExternalLocationTypeEntity inboundLocation;
    private ExternalLocationTypeEntity unstructuredLocation;

    @Mock
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    public void setUp() {
        deleter = new InboundUnstructuredDataStoreStoreDeleterImpl(
            dataManagementDao,
            externalObjectDirectoryRepository,
            objectDirectoryStatusRepository,
            externalLocationTypeRepository,
            dataManagementConfiguration,
            userAccountRepository
        );


    }

    private void mockSystemUser() {
        Optional<UserAccountEntity> userEntity = Optional.of(new UserAccountEntity());
        when(userAccountRepository.findById(0)).thenReturn(userEntity);
    }

    private void mockUnstructuredLocation() {
        this.unstructuredLocation = new ExternalLocationTypeEntity();
        unstructuredLocation.setId(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId())).thenReturn(
            unstructuredLocation);
    }

    private void mockInboundLocation() {
        this.inboundLocation = new ExternalLocationTypeEntity();
        inboundLocation.setId(ExternalLocationTypeEnum.INBOUND.getId());
        when(externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.INBOUND.getId())).thenReturn(
            inboundLocation);
    }

    private void mockStatus() {
        this.markedForDeletionStatus = new ObjectDirectoryStatusEntity();
        markedForDeletionStatus.setId(ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId());
        when(objectDirectoryStatusRepository.getReferenceById(ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId())).thenReturn(
            markedForDeletionStatus);
    }

    @Test
    void deleteFromInboundAndUnstructuredDatastore() {
        mockStatus();

        mockInboundLocation();

        mockUnstructuredLocation();

        mockSystemUser();

        List<ExternalObjectDirectoryEntity> inboundData = createInboundData();

        List<ExternalObjectDirectoryEntity> unstructuredData = createUnstructuredData();


        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndMarkedForDeletion(
            inboundLocation,
            markedForDeletionStatus
        ))
            .thenReturn(inboundData);

        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndMarkedForDeletion(
            unstructuredLocation,
            markedForDeletionStatus
        ))
            .thenReturn(unstructuredData);


        when(dataManagementDao.getBlobContainerClient(any())).thenReturn(blobContainerClient);
        when(dataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);


        List<ExternalObjectDirectoryEntity> deletedItems = deleter.delete();

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


    }


    @Test
    void testDeleteWhenSystemUserDoesNotExist() {
        when(userAccountRepository.findById(0)).thenReturn(Optional.empty());
        assertThrows(DartsApiException.class, () ->
            deleter.delete());
    }


    private List<ExternalObjectDirectoryEntity> createUnstructuredData() {
        ExternalObjectDirectoryEntity unstructuredData = new ExternalObjectDirectoryEntity();
        unstructuredData.setStatus(markedForDeletionStatus);
        unstructuredData.setExternalLocationType(unstructuredLocation);
        unstructuredData.setId(1);
        ArrayList<ExternalObjectDirectoryEntity> unstructuredDataList = new ArrayList<>();
        unstructuredDataList.add(unstructuredData);
        return unstructuredDataList;
    }

    private List<ExternalObjectDirectoryEntity> createInboundData() {
        ExternalObjectDirectoryEntity inboundData = new ExternalObjectDirectoryEntity();
        inboundData.setStatus(markedForDeletionStatus);
        inboundData.setExternalLocationType(inboundLocation);
        inboundData.setId(2);

        ArrayList<ExternalObjectDirectoryEntity> inboundDataList = new ArrayList<>();
        inboundDataList.add(inboundData);
        return inboundDataList;
    }
}
