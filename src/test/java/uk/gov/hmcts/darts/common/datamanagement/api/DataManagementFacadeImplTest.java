package uk.gov.hmcts.darts.common.datamanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private ExternalLocationTypeEntity inboundLocationEntity;
    private ExternalLocationTypeEntity unstructuredLocationEntity;
    private ExternalLocationTypeEntity detsLocationEntity;
    private ExternalLocationTypeEntity armLocationEntity;

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

    @Test
    void testDownloadOfFacadeWithArm() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));

        ExternalObjectDirectoryEntity arm = createEodEntity(armLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(arm);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.ARM, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
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
                                                                               objectRecordStatusRepository, storageOrderHelper);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.UNSTRUCTURED, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

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
                                                                               objectRecordStatusRepository, storageOrderHelper);

        assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(annotationDocumentEntity)
        );
    }


    private BlobContainerDownloadable setupDownloadableContainer(DatastoreContainerType containerType,
                                                                 boolean processSuccess) throws Exception {
        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);

        Mockito.lenient().when(downloadable.getContainerName(containerType)).thenReturn(Optional.of("test"));
        if (processSuccess) {
            when(downloadable
                     .downloadBlobFromContainer(eq(containerType),
                                                Mockito.notNull())).thenReturn(new FileBasedDownloadResponseMetaData());
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