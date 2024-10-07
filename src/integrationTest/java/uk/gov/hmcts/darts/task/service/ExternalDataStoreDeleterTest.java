package uk.gov.hmcts.darts.task.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.deleter.impl.dets.DetsDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.dets.DetsExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.audio.deleter.impl.dets.ExternalDetsDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.InboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.InboundExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.OutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.OutboundExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.UnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.UnstructuredExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.AudioTransformationServiceGivenBuilder;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersAccountUUIDEnum;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaRequestTestData;

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
    @Mock
    private BlobServiceClient blobServiceClient;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    @MockBean
    private DataManagementAzureClientFactory dataManagementFactory;

    @Autowired
    private InboundExternalObjectDirectoryDeletedFinder inboundExternalObjectDirectoryDeletedFinder;

    @Autowired
    private UnstructuredExternalObjectDirectoryDeletedFinder unstructuredExternalObjectDirectoryDeletedFinder;

    @Autowired
    private OutboundExternalObjectDirectoryDeletedFinder outboundExternalObjectDirectoryDeletedFinder;
    @Autowired
    private DetsExternalObjectDirectoryDeletedFinder detsExternalObjectDirectoryDeletedFinder;
    @Autowired
    private InboundDataStoreDeleter inboundDataStoreDeleter;

    @Autowired
    private UnstructuredDataStoreDeleter unstructuredDataStoreDeleter;

    @Autowired
    private OutboundDataStoreDeleter outboundDataStoreDeleter;

    @Autowired
    private DetsDataStoreDeleter detsDataStoreDeleter;

    @MockBean
    private DetsApiService detsApiService;

    private UserAccountEntity requestor;
    private HearingEntity hearing;

    private ExternalInboundDataStoreDeleter externalInboundDataStoreDeleter;
    private ExternalUnstructuredDataStoreDeleter externalUnstructuredDataStoreDeleter;
    private ExternalOutboundDataStoreDeleter externalOutboundDataStoreDeleter;
    private ExternalDetsDataStoreDeleter externalDetsDataStoreDeleter;

    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    @BeforeEach
    void setUp() {
        this.requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        this.hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            LocalDateTime.now()
        );

        SystemUserHelper systemUserHelper = new SystemUserHelper(dartsDatabase.getUserAccountRepository(), automatedTaskConfigurationProperties);
        systemUserHelper.setSystemUserGuidMap(Collections.singletonMap(
            "housekeeping",
            SystemUsersAccountUUIDEnum.HOUSE_KEEPING.getUuid()
        ));


        externalInboundDataStoreDeleter = new ExternalInboundDataStoreDeleter(
            dartsDatabase.getExternalObjectDirectoryRepository(),
            inboundExternalObjectDirectoryDeletedFinder,
            inboundDataStoreDeleter,
            transformedMediaRepository
        );

        externalUnstructuredDataStoreDeleter = new ExternalUnstructuredDataStoreDeleter(
            dartsDatabase.getExternalObjectDirectoryRepository(),
            unstructuredExternalObjectDirectoryDeletedFinder,
            unstructuredDataStoreDeleter,
            transformedMediaRepository
        );

        externalOutboundDataStoreDeleter = new ExternalOutboundDataStoreDeleter(
            dartsDatabase.getTransientObjectDirectoryRepository(),
            outboundExternalObjectDirectoryDeletedFinder,
            outboundDataStoreDeleter,
            transformedMediaRepository
        );

        externalDetsDataStoreDeleter = new ExternalDetsDataStoreDeleter(
            dartsDatabase.getExternalObjectDirectoryRepository(),
            detsExternalObjectDirectoryDeletedFinder,
            detsDataStoreDeleter,
            transformedMediaRepository
        );

    }

    @SneakyThrows
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Test
    void deleteMarkedForDeletionDataFromDataStores() {
        audioBuilder.setupTest();
        Mockito.when(dataManagementFactory.getBlobServiceClient(anyString())).thenReturn(blobServiceClient);
        Mockito.when(dataManagementFactory.getBlobContainerClient(anyString(), eq(blobServiceClient))).thenReturn(blobContainerClient);
        Mockito.when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);

        MediaRequestEntity currentMediaRequest = getMediaRequestTestData().createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        ExternalObjectDirectoryEntity unstructuredEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity1(),
            UNSTRUCTURED.getId(), MARKED_FOR_DELETION
        );

        ExternalObjectDirectoryEntity inboundEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity2(),
            INBOUND.getId(), MARKED_FOR_DELETION
        );

        ExternalObjectDirectoryEntity detsEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity1(),
            DETS.getId(), MARKED_FOR_DELETION
        );

        TransientObjectDirectoryEntity outboundEntity = createTransientDirectoryAndObjectStatus(
            currentMediaRequest, MARKED_FOR_DELETION);

        externalInboundDataStoreDeleter.delete();
        externalUnstructuredDataStoreDeleter.delete();
        externalOutboundDataStoreDeleter.delete();
        externalDetsDataStoreDeleter.delete();

        verifyEntitiesDeleted(List.of(inboundEntity, unstructuredEntity, detsEntity), List.of(outboundEntity));
        verify(detsApiService).deleteBlobDataFromContainer(any(UUID.class));
    }

    @SneakyThrows
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Test
    void dontDeleteWhenStatusIsNotMarkedForDeletionDataFromDataStores() {
        audioBuilder.setupTest();
        Mockito.when(dataManagementFactory.getBlobServiceClient(anyString())).thenReturn(blobServiceClient);
        Mockito.when(dataManagementFactory.getBlobContainerClient(anyString(), eq(blobServiceClient))).thenReturn(blobContainerClient);
        Mockito.when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);

        MediaRequestEntity currentMediaRequest = getMediaRequestTestData().createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(currentMediaRequest);

        TransientObjectDirectoryEntity outboundEntity = createTransientDirectoryAndObjectStatus(currentMediaRequest, STORED);
        ExternalObjectDirectoryEntity unstructuredEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity1(),
            UNSTRUCTURED.getId(),
            STORED
        );
        ExternalObjectDirectoryEntity inboundEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity2(),
            INBOUND.getId(), STORED
        );

        ExternalObjectDirectoryEntity detsEntity = createExternalObjectDirectory(
            audioBuilder.getMediaEntity1(),
            DETS.getId(), STORED
        );

        externalInboundDataStoreDeleter.delete();
        externalUnstructuredDataStoreDeleter.delete();
        externalOutboundDataStoreDeleter.delete();
        externalDetsDataStoreDeleter.delete();

        verifyEntitiesNotChanged(List.of(unstructuredEntity, inboundEntity, detsEntity), List.of(outboundEntity));
        verify(detsApiService, never()).deleteBlobDataFromContainer(any(UUID.class));
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

            assertEquals(entity.getLastModifiedDateTime().toInstant(), savedEntity.getLastModifiedDateTime().toInstant());
        }
    }

    private void assertExternalObjectDirectoryStateNotChanged(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities) {
        for (ExternalObjectDirectoryEntity eod : externalObjectDirectoryEntities) {
            ExternalObjectDirectoryEntity savedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(eod.getId()).get();

            assertEquals(eod.getStatus().getId(), savedEod.getStatus().getId());
            assertEquals(eod.getLastModifiedBy().getId(), savedEod.getLastModifiedBy().getId());
            assertEquals(eod.getLastModifiedDateTime().toInstant(), savedEod.getLastModifiedDateTime().toInstant());
        }
    }

    private void verifyEntitiesDeleted(List<ExternalObjectDirectoryEntity> inboundUnstructuredList,
                                       List<TransientObjectDirectoryEntity> outboundList) {
        assertExternalObjectDirectoryDeleted(inboundUnstructuredList);
        assertTransientObjectDirectoryDeleted(outboundList);
    }


    private void assertExternalObjectDirectoryDeleted(List<ExternalObjectDirectoryEntity> inboundUnstructuredList) {
        for (ExternalObjectDirectoryEntity entity : inboundUnstructuredList) {
            Optional<ExternalObjectDirectoryEntity> savedEntity = dartsDatabase.getExternalObjectDirectoryRepository().findById(
                entity.getId());
            assertTrue(savedEntity.isEmpty());
        }
    }

    private void assertTransientObjectDirectoryDeleted(List<TransientObjectDirectoryEntity> outboundList) {
        for (TransientObjectDirectoryEntity entity : outboundList) {
            Optional<TransientObjectDirectoryEntity> savedEntity = dartsDatabase.getTransientObjectDirectoryRepository().findById(
                entity.getId());
            assertTrue(savedEntity.isEmpty());
        }
    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity, Integer dataStoreId, ObjectRecordStatusEnum status) {
        var externalObjectDirectoryEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            mediaEntity,
            dartsDatabase.getObjectRecordStatusEntity(status),
            dartsDatabase.getExternalLocationTypeRepository().getReferenceById(dataStoreId),
            UUID.randomUUID()
        );

        return dartsDatabase.save(externalObjectDirectoryEntity);
    }

    private TransientObjectDirectoryEntity createTransientDirectoryAndObjectStatus(MediaRequestEntity currentMediaRequest, ObjectRecordStatusEnum status) {
        var blobId = UUID.randomUUID();


        return dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                currentMediaRequest,
                dartsDatabase.getObjectRecordStatusEntity(status),
                blobId
            ));

    }
}