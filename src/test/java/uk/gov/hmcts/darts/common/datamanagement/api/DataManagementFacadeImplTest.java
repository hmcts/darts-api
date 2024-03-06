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
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

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
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        ExternalObjectDirectoryEntity arm = createEodEntity(armLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(arm);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               externalLocationTypeRepository, objectRecordStatusRepository, storageOrderHelper,
                                                                               configuration);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.ARM, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testDownloadOfFacadeWithUnstructured() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.UNSTRUCTURED, true));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        ExternalObjectDirectoryEntity dets = createEodEntity(unstructuredLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               externalLocationTypeRepository, objectRecordStatusRepository, storageOrderHelper,
                                                                               configuration);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.UNSTRUCTURED, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testDownloadOfFacadeWithDetsEnabled() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.DETS, true));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        ExternalObjectDirectoryEntity dets = createEodEntity(detsLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               externalLocationTypeRepository, objectRecordStatusRepository, storageOrderHelper,
                                                                               configuration);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaData = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.DETS, downloadResponseMetaData.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testDownloadOfFacadeWithDetsDisabled() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.DETS, false));
        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(false);

        ExternalObjectDirectoryEntity dets = createEodEntity(detsLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               externalLocationTypeRepository, objectRecordStatusRepository, storageOrderHelper,
                                                                               configuration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("Ignoring container as its been turned off DETS"));

    }

    @Test
    void testDownloadOfFacadeWithNoneProcessed() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.INBOUND, false));

        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        Mockito.when(configuration.isFetchFromDetsEnabled()).thenReturn(true);

        // create the payload to be tested
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(inboundEntity);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               externalLocationTypeRepository, objectRecordStatusRepository, storageOrderHelper,
                                                                               configuration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("matching eodEntity not found for ARM"));
    }

    @Test
    void testDownloadOfFacadeWithUnstructuredAndArmProcessedInPriorityOrder() throws Exception {
        final DetsDataManagementConfiguration configuration = Mockito.mock(DetsDataManagementConfiguration.class);
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        // setup the containers to use
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        // create the payload entities to be tested
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);
        ExternalObjectDirectoryEntity armDirectoryEntity = createEodEntity(armLocationEntity);


        List<ExternalObjectDirectoryEntity> entitiesToDownload = Arrays.asList(inboundEntity, armDirectoryEntity);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               externalLocationTypeRepository, objectRecordStatusRepository, storageOrderHelper,
                                                                               configuration);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> dmFacade.retrieveFileFromStorage(entitiesToDownload)
        );

        assertTrue(exception.getMessage().contains("checking container ARM"));
    }

    private BlobContainerDownloadable setupDownloadableContainer(DatastoreContainerType containerType,
                                                                 boolean processSuccess) throws Exception {
        BlobContainerDownloadable downloadable = Mockito.mock(BlobContainerDownloadable.class);

        Mockito.lenient().when(downloadable.getContainerName(containerType)).thenReturn(Optional.of("test"));
        if (processSuccess) {
            Mockito.when(downloadable
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