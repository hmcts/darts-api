package uk.gov.hmcts.darts.task.service;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.AudioTransformationServiceGivenBuilder;
import uk.gov.hmcts.darts.audio.service.ExternalDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.InboundUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.impl.ExternalDataStoreDeleterImpl;
import uk.gov.hmcts.darts.audio.service.impl.OutboundDataStoreDeleterImpl;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.AudioTestData;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.DELETED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@SuppressWarnings({"PMD.ExcessiveImports"})
class ExternalDataStoreDeleterTest extends IntegrationBase {


    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;
    @Autowired
    private AudioTransformationServiceGivenBuilder audioBuilder;
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobContainerClient blobContainerClient;
    @MockBean
    private DataManagementDao dataManagementDao;
    @Autowired
    private DataManagementConfiguration dataManagementConfiguration;
    @Autowired
    private InboundUnstructuredDataStoreDeleter inboundUnstructuredDataStoreDeleter;
    @Autowired
    private OutboundDataStoreDeleterImpl outboundDataStoreDeleter;
    private UserAccountEntity requestor;
    private HearingEntity hearing;


    @BeforeEach
    void setUp() {
        this.requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        this.hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            LocalDate.now()
        );
    }

    @Test
    void deleteMarkedForDeletionDataFromDataStores() {
        Mockito.when(dataManagementDao.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        Mockito.when(dataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        ExternalObjectDirectoryEntity unstructuredEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity1(),
            UNSTRUCTURED.getId(), MARKED_FOR_DELETION
        );


        ExternalObjectDirectoryEntity inboundEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity2(),
            INBOUND.getId(), MARKED_FOR_DELETION
        );

        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            OffsetDateTime.now(),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        TransientObjectDirectoryEntity outboundEntity = createTransientDirectoryAndObjectStatus(
            currentMediaRequest, MARKED_FOR_DELETION);

        ExternalDataStoreDeleter dataStoreDeleter = new ExternalDataStoreDeleterImpl(
            inboundUnstructuredDataStoreDeleter, outboundDataStoreDeleter
        );

        dataStoreDeleter.delete();
        verifyEntitiesChanged(List.of(unstructuredEntity, inboundEntity), List.of(outboundEntity));
    }


    @Test
    void dontDeleteWhenStatusIsNotMarkedForDeletionDataFromDataStores() {
        Mockito.when(dataManagementDao.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        Mockito.when(dataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        ExternalObjectDirectoryEntity unstructuredEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity1(),
            UNSTRUCTURED.getId(), STORED
        );


        ExternalObjectDirectoryEntity inboundEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity2(),
            INBOUND.getId(), STORED
        );

        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            OffsetDateTime.now(),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        TransientObjectDirectoryEntity outboundEntity = createTransientDirectoryAndObjectStatus(
            currentMediaRequest, STORED);

        ExternalDataStoreDeleter dataStoreDeleter = new ExternalDataStoreDeleterImpl(
            inboundUnstructuredDataStoreDeleter, outboundDataStoreDeleter
        );

        dataStoreDeleter.delete();
        verifyEntitiesNotChanged(List.of(unstructuredEntity, inboundEntity), List.of(outboundEntity));
    }

    private void verifyEntitiesNotChanged(List<ExternalObjectDirectoryEntity> inboundUnstructuredList, List<TransientObjectDirectoryEntity> outboundList) {
        assertExternalObjectDirectoryStateNotChanged(inboundUnstructuredList);
        assertTransientObjectDirectoryStateNotChanged(outboundList);
    }

    private void assertTransientObjectDirectoryStateNotChanged(List<TransientObjectDirectoryEntity> outboundList) {
        for (TransientObjectDirectoryEntity entity : outboundList) {
            TransientObjectDirectoryEntity savedEntity = dartsDatabase.getTransientObjectDirectoryRepository().findById(
                entity.getId()).get();

            assertEquals(
                entity.getStatus().getId(),
                savedEntity.getStatus().getId()
            );

            assertEquals(
                entity.getLastModifiedBy().getId(),
                savedEntity.getLastModifiedBy().getId()
            );

            assertEquals(entity.getLastModifiedDateTime(), savedEntity.getLastModifiedDateTime());
        }
    }

    private void assertExternalObjectDirectoryStateNotChanged(List<ExternalObjectDirectoryEntity> unstructuredEntity) {
        for (ExternalObjectDirectoryEntity entity : unstructuredEntity) {
            ExternalObjectDirectoryEntity savedEntity = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                entity.getId()).get();
            assertEquals(
                entity.getStatus().getId(),
                savedEntity.getStatus().getId()
            );

            assertEquals(
                entity.getLastModifiedBy().getId(),
                savedEntity.getLastModifiedBy().getId()
            );

            assertEquals(entity.getLastModifiedDateTime(), savedEntity.getLastModifiedDateTime());
        }
    }

    private void verifyEntitiesChanged(List<ExternalObjectDirectoryEntity> inboundUnstructuredList,
                                       List<TransientObjectDirectoryEntity> outboundList) {

        assertExternalObjectDirectoryStateChanged(inboundUnstructuredList);
        assertTransientObjectDirectoryStateChanged(outboundList);
    }

    private void assertExternalObjectDirectoryStateChanged(List<ExternalObjectDirectoryEntity> inboundUnstructuredList) {
        for (ExternalObjectDirectoryEntity entity : inboundUnstructuredList) {
            ExternalObjectDirectoryEntity savedEntity = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                entity.getId()).get();
            assertEquals(
                DELETED.getId(),
                savedEntity.getStatus().getId()
            );

            assertEquals(
                SystemUsersEnum.DEFAULT.getId(),
                savedEntity.getLastModifiedBy().getId()
            );
        }
    }

    private void assertTransientObjectDirectoryStateChanged(List<TransientObjectDirectoryEntity> outboundList) {
        for (TransientObjectDirectoryEntity entity : outboundList) {
            TransientObjectDirectoryEntity savedEntity = dartsDatabase.getTransientObjectDirectoryRepository().findById(
                entity.getId()).get();

            assertEquals(
                DELETED.getId(),
                savedEntity.getStatus().getId()
            );

            assertEquals(
                SystemUsersEnum.DEFAULT.getId(),
                savedEntity.getLastModifiedBy().getId()
            );
        }

    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity, Integer dataStoreId, ObjectDirectoryStatusEnum status) {
        var externalObjectDirectoryEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            mediaEntity,
            dartsDatabase.getObjectDirectoryStatusEntity(status),
            dartsDatabase.getExternalLocationTypeRepository().getReferenceById(dataStoreId),
            UUID.randomUUID()
        );

        return dartsDatabase.save(externalObjectDirectoryEntity);
    }

    private TransientObjectDirectoryEntity createTransientDirectoryAndObjectStatus(MediaRequestEntity currentMediaRequest, ObjectDirectoryStatusEnum status) {
        var blobId = UUID.randomUUID();


        return dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                currentMediaRequest,
                dartsDatabase.getObjectDirectoryStatusEntity(status),
                blobId
            ));

    }
}
