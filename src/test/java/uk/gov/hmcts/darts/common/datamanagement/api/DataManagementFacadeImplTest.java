package uk.gov.hmcts.darts.common.datamanagement.api;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.helper.UnstructuredDataHelper;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.datamanagement.helper.StorageOrderHelper;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataManagementFacadeImplTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private StorageOrderHelper storageOrderHelper;
    @Mock
    private UnstructuredDataHelper unstructuredDataHelper;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private DataManagementService dataManagementService;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private BlobClient blobClient;

    private ExternalLocationTypeEntity inboundLocationEntity;
    private ExternalLocationTypeEntity unstructuredLocationEntity;
    private ExternalLocationTypeEntity detsLocationEntity;
    private ExternalLocationTypeEntity armLocationEntity;
    FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData();
    DownloadResponseMetaData downloadResponseMetaData = new FileBasedDownloadResponseMetaData();
    @TempDir
    private File tempDirectory;

    @BeforeEach
    void setup() {

        List<DatastoreContainerType> datastoreOrder = new ArrayList<>();
        datastoreOrder.add(DatastoreContainerType.UNSTRUCTURED);
        datastoreOrder.add(DatastoreContainerType.DETS);
        datastoreOrder.add(DatastoreContainerType.ARM);
        Mockito.lenient().when(storageOrderHelper.getStorageOrder()).thenReturn(datastoreOrder);

        inboundLocationEntity = new ExternalLocationTypeEntity();
        Integer id = ExternalLocationTypeEnum.INBOUND.getId();
        inboundLocationEntity.setId(id);
        Mockito.lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(inboundLocationEntity);

        unstructuredLocationEntity = new ExternalLocationTypeEntity();
        id = ExternalLocationTypeEnum.UNSTRUCTURED.getId();
        unstructuredLocationEntity.setId(id);
        Mockito.lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(unstructuredLocationEntity);

        detsLocationEntity = new ExternalLocationTypeEntity();
        id = ExternalLocationTypeEnum.DETS.getId();
        detsLocationEntity.setId(id);
        Mockito.lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(detsLocationEntity);

        armLocationEntity = new ExternalLocationTypeEntity();
        id = ExternalLocationTypeEnum.ARM.getId();
        armLocationEntity.setId(id);
        Mockito.lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(armLocationEntity);

        ObjectRecordStatusEntity storedStatus = new ObjectRecordStatusEntity();
        storedStatus.setId(2);
        Mockito.lenient().when(objectRecordStatusRepository.getReferenceById(anyInt())).thenReturn(storedStatus);

    }

    @AfterEach
    public void teardown() throws IOException {
        fileBasedDownloadResponseMetaData.close();
        downloadResponseMetaData.close();
        unstructuredDataHelper.waitForAllJobsToFinish();
    }

    @Test
    void testDownloadOfFacadeWithArm() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));

        ExternalObjectDirectoryEntity arm = createEodEntity(armLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(arm);

        UnstructuredDataHelper unstructuredDataHelperTest = getUnstructuredDataHelper();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelperTest,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.ARM, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
        unstructuredDataHelper.waitForAllJobsToFinish();

    }

    @Test
    void testDownloadOfFacadeWithUnstructured() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.UNSTRUCTURED, true));

        ExternalObjectDirectoryEntity dets = createEodEntity(unstructuredLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.UNSTRUCTURED, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testThrowErrorNoStoredEodEntities() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        ExternalObjectDirectoryEntity dets = createEodEntity(unstructuredLocationEntity);
        dets.getStatus().setId(ObjectRecordStatusEnum.FAILURE.getId());

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("Supplied list of EodEntities does not have any that are stored"));
    }

    @Test
    void testDownloadOfFacadeWithDetsEnabled() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.DETS, true));

        ExternalObjectDirectoryEntity dets = createEodEntity(detsLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.DETS, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testDownloadOfFacadeWithNoneProcessed() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.INBOUND, false));

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        // create the payload to be tested
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(inboundEntity);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("matching eodEntity not found for ARM"));
    }

    @Test
    void testDownloadOfFacadeWithUnstructuredAndArmProcessedInPriorityOrder() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        // create the payload entities to be tested
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);
        ExternalObjectDirectoryEntity armDirectoryEntity = createEodEntity(armLocationEntity);


        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(inboundEntity, armDirectoryEntity);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("checking container ARM"));
    }

    @Test
    void retrieveFileFromStorageListEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        List<ExternalObjectDirectoryEntity> entitiesToDownload = new ArrayList<>();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("Supplied list of EodEntities is empty"));
    }

    @Test
    void retrieveFileFromStorageMediaEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        MediaEntity mediaEntity = new MediaEntity();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(mediaEntity)
        );

        assertTrue(exception.getMessage().contains("No storedEodEntities found for mediaId"));
    }

    @Test
    void retrieveFileFromStorageMediaFail() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        MediaEntity mediaEntity = new MediaEntity();

        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);

        when(externalObjectDirectoryRepository.findByEntityAndStatus(any(MediaEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(List.of(inboundEntity));
        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(mediaEntity)
        );
    }

    @Test
    void retrieveFileFromStorageTranscriptionEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(transcriptionDocumentEntity)
        );

        assertTrue(exception.getMessage().contains("No storedEodEntities found for transcriptionDocumentId"));
    }

    @Test
    void retrieveFileFromStorageTranscriptionFail() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();

        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);

        when(externalObjectDirectoryRepository.findByEntityAndStatus(any(TranscriptionDocumentEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(List.of(inboundEntity));
        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(transcriptionDocumentEntity)
        );
    }


    @Test
    void retrieveFileFromStorageAnnotationDocumentEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        AnnotationDocumentEntity annotationDocumentEntity = new AnnotationDocumentEntity();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(annotationDocumentEntity)
        );

        assertTrue(exception.getMessage().contains("No storedEodEntities found for annotationDocumentId"));
    }

    @Test
    void retrieveFileFromStorageAnnotationFail() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        AnnotationDocumentEntity annotationDocumentEntity = new AnnotationDocumentEntity();

        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);

        when(externalObjectDirectoryRepository.findByEntityAndStatus(any(AnnotationDocumentEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(List.of(inboundEntity));
        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration);

        assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(annotationDocumentEntity)
        );
    }

    @Test
    void testUnstructuredDataHelperCreate() throws Exception {

        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn("unstructured");
        when(dataManagementService.saveBlobData((String) any(), (InputStream) any())).thenReturn(UUID.randomUUID());

        UnstructuredDataHelper unstructuredDataHelperTest = getUnstructuredDataHelper();

        MediaEntity mediaEntity = new MediaEntity();
        ExternalObjectDirectoryEntity eodEntity = createEodEntity(unstructuredLocationEntity);
        eodEntity.setMedia(mediaEntity);
        ExternalObjectDirectoryEntity eodEntityToDelete = createEodEntity(unstructuredLocationEntity);
        eodEntityToDelete.setMedia(mediaEntity);
        BinaryData data = BinaryData.fromString("Test String");
        String fileLocation = tempDirectory.getAbsolutePath();
        File targetFile = new File(fileLocation, UUID.randomUUID().toString());

        boolean created = unstructuredDataHelperTest.createUnstructured(
            eodEntityToDelete,
            eodEntity,
            data.toStream(),
            targetFile);

        assertTrue(created);
    }

    @Test
    void testUnstructuredDataHelperCreateFailed() throws Exception {

        UnstructuredDataHelper unstructuredDataHelperTest = getUnstructuredDataHelper();

        MediaEntity mediaEntity = new MediaEntity();
        ExternalObjectDirectoryEntity eodEntity = createEodEntity(unstructuredLocationEntity);
        eodEntity.setMedia(mediaEntity);
        ExternalObjectDirectoryEntity eodEntityToDelete = createEodEntity(unstructuredLocationEntity);
        eodEntityToDelete.setMedia(mediaEntity);
        BinaryData data = BinaryData.fromString("Test String");
        String fileLocation = tempDirectory.getAbsolutePath();
        File targetFile = new File(fileLocation, UUID.randomUUID().toString());

        boolean created = unstructuredDataHelperTest.createUnstructured(
            eodEntityToDelete,
            eodEntity,
            data.toStream(),
            targetFile);

        assertFalse(created);
    }

    @Test
    void testDownloadOfFacadeCreatesUnstructuredWhenUnstructuredNotFound() throws Exception {
        unstructuredDataHelper.waitForAllJobsToFinish();
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);
        ExternalObjectDirectoryEntity unstructuredEntity = createEodEntity(unstructuredLocationEntity);
        ExternalObjectDirectoryEntity armEntity = createEodEntity(armLocationEntity);

        when(externalObjectDirectoryRepository.findByEntityAndStatus(any(MediaEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(List.of(inboundEntity, unstructuredEntity, armEntity));

        MediaEntity mediaEntity = new MediaEntity();
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));
        UnstructuredDataHelper unstructuredDataHelperFacade = getUnstructuredDataHelper();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelperFacade,
                                                                               dataManagementConfiguration);

        downloadResponseMetaData = dmFacade.retrieveFileFromStorage(mediaEntity);
        assertEquals(DatastoreContainerType.ARM, downloadResponseMetaData.getContainerTypeUsedToDownload());
        assertNotEquals(0, unstructuredDataHelperFacade.getJobsList().size());
    }

    @NotNull
    private UnstructuredDataHelper getUnstructuredDataHelper() {
        return new UnstructuredDataHelper(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            userAccountRepository,
            dataManagementService,
            dataManagementConfiguration
        );
    }

    private BlobContainerDownloadable setupDownloadableContainer(DatastoreContainerType containerType,
                                                                 boolean processSuccess) throws Exception {
        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);

        BinaryData data = BinaryData.fromString("Test String");

        fileBasedDownloadResponseMetaData.markInputStream(data.toStream());

        Mockito.lenient().when(downloadable.getContainerName(containerType)).thenReturn(Optional.of("test"));
        if (processSuccess) {
            when(downloadable
                     .downloadBlobFromContainer(eq(containerType),
                                                Mockito.notNull())).thenReturn(fileBasedDownloadResponseMetaData);
        } else {
            Mockito.lenient().when(downloadable
                                       .downloadBlobFromContainer(eq(containerType),
                                                                  Mockito.notNull())).thenThrow(new FileNotDownloadedException());
        }
        return downloadable;
    }

    private ExternalObjectDirectoryEntity createEodEntity(ExternalLocationTypeEntity locationTypeEntity) {
        ExternalObjectDirectoryEntity entity = new ExternalObjectDirectoryEntity();

        entity.setExternalLocationType(locationTypeEntity);

        ObjectRecordStatusEntity storedStatus = new ObjectRecordStatusEntity();
        storedStatus.setId(2);
        entity.setStatus(storedStatus);

        return entity;
    }
}
