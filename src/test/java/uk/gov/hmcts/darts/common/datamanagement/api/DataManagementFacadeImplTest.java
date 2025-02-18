package uk.gov.hmcts.darts.common.datamanagement.api;

import com.azure.core.util.BinaryData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.darts.arm.service.impl.ArmApiServiceImpl;
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
import uk.gov.hmcts.darts.common.entity.ObjectRetrievalQueueEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRetrievalQueueRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponseImpl;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.test.common.FileStore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@Slf4j
class DataManagementFacadeImplTest {

    public static final String SOME_TEMP_WORKSPACE = "some/temp/workspace";
    public static final String DOWNLOAD_RESPONSE_META_DATA = "DownloadResponseMetaData: {}";
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
    private DataManagementService dataManagementService;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private ArmApiServiceImpl armApiService;
    @Mock
    private ObjectRetrievalQueueRepository objectRetrievalQueueRepository;
    @Mock
    private DownloadResponseMetaData downloadResponseMetaDataMock;
    @Mock
    private BlobClientUploadResponseImpl blobClientUploadResponseImpl;

    private ExternalLocationTypeEntity inboundLocationEntity;
    private ExternalLocationTypeEntity unstructuredLocationEntity;
    private ExternalLocationTypeEntity detsLocationEntity;
    private ExternalLocationTypeEntity armLocationEntity;
    FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData = new FileBasedDownloadResponseMetaData();
    DownloadResponseMetaData downloadResponseMetaData = new FileBasedDownloadResponseMetaData();
    @TempDir
    private File tempDirectory;

    @SneakyThrows
    @BeforeEach
    void setup() {

        List<DatastoreContainerType> datastoreOrder = new ArrayList<>();
        datastoreOrder.add(DatastoreContainerType.UNSTRUCTURED);
        datastoreOrder.add(DatastoreContainerType.DETS);
        datastoreOrder.add(DatastoreContainerType.ARM);
        lenient().when(storageOrderHelper.getStorageOrder()).thenReturn(datastoreOrder);

        inboundLocationEntity = new ExternalLocationTypeEntity();
        Integer id = ExternalLocationTypeEnum.INBOUND.getId();
        inboundLocationEntity.setId(id);
        lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(inboundLocationEntity);

        unstructuredLocationEntity = new ExternalLocationTypeEntity();
        id = ExternalLocationTypeEnum.UNSTRUCTURED.getId();
        unstructuredLocationEntity.setId(id);
        lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(unstructuredLocationEntity);

        detsLocationEntity = new ExternalLocationTypeEntity();
        id = ExternalLocationTypeEnum.DETS.getId();
        detsLocationEntity.setId(id);
        lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(detsLocationEntity);

        armLocationEntity = new ExternalLocationTypeEntity();
        id = ExternalLocationTypeEnum.ARM.getId();
        armLocationEntity.setId(id);
        lenient().when(externalLocationTypeRepository.getReferenceById(id)).thenReturn(armLocationEntity);

        ObjectRecordStatusEntity storedStatus = new ObjectRecordStatusEntity();
        storedStatus.setId(2);
        lenient().when(objectRecordStatusRepository.getReferenceById(anyInt())).thenReturn(storedStatus);

        Resource resource = mock(Resource.class);

        lenient().when(objectRecordStatusRepository.getReferenceById(anyInt())).thenReturn(storedStatus);

        lenient().when(dataManagementConfiguration.getTempBlobWorkspace()).thenReturn("/tmp");
        lenient().when(resource.getInputStream()).thenReturn(toInputStream("testInputStream", UTF_8));
        lenient().when(downloadResponseMetaDataMock.getResource()).thenReturn(resource);
        lenient().when(downloadResponseMetaDataMock.getContainerTypeUsedToDownload()).thenReturn(DatastoreContainerType.ARM);
    }

    @AfterEach
    @SuppressWarnings("SignatureDeclareThrowsException")
    public void teardown() throws IOException {
        fileBasedDownloadResponseMetaData.close();
        downloadResponseMetaData.close();

        FileStore.getFileStore().remove();

        try (Stream<Path> files = Files.list(tempDirectory.toPath())) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void testDownloadOfFacadeWithArm() throws Exception {

        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));

        ExternalObjectDirectoryEntity arm = createEodEntity(armLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = List.of(arm);

        UnstructuredDataHelper unstructuredDataHelperTest = getUnstructuredDataHelper();

        when(armApiService.downloadArmData(arm.getExternalRecordId(), arm.getExternalFileId())).thenReturn(downloadResponseMetaDataMock);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelperTest,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.ARM, downloadResponseMetaDataAutoClose.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testDownloadOfFacadeWithUnstructured() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.UNSTRUCTURED, true));

        ExternalObjectDirectoryEntity dets = createEodEntity(unstructuredLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = List.of(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.UNSTRUCTURED, downloadResponseMetaDataAutoClose.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testThrowErrorNoStoredEodEntities() {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        ExternalObjectDirectoryEntity dets = createEodEntity(unstructuredLocationEntity);
        dets.getStatus().setId(ObjectRecordStatusEnum.FAILURE.getId());

        List<ExternalObjectDirectoryEntity> entitiesToDownload = List.of(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );

        assertTrue(exception.getMessage().contains("Supplied list of EodEntities does not have any that are stored"));
    }

    @Test
    void testDownloadOfFacadeWithDetsEnabled() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        BlobContainerDownloadable downloadable = mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.DETS, true));

        ExternalObjectDirectoryEntity dets = createEodEntity(detsLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = List.of(dets);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
            assertEquals(DatastoreContainerType.DETS, downloadResponseMetaDataAutoClose.getContainerTypeUsedToDownload());
        }
    }

    @Test
    void testDownloadOfFacadeWithNoneProcessed() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();

        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.INBOUND, false));

        BlobContainerDownloadable downloadable = mock(BlobContainerDownloadable.class);
        blobContainerDownloadables.add(downloadable);

        // create the payload to be tested
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);

        List<ExternalObjectDirectoryEntity> entitiesToDownload = List.of(inboundEntity);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
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

        when(armApiService.downloadArmData(any(), any())).thenThrow(FileNotDownloadedException.class);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
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
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(entitiesToDownload)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );

        assertTrue(exception.getMessage().contains("Supplied list of EodEntities is empty"));
    }

    @Test
    void retrieveFileFromStorageMediaEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setContentObjectId("2");
        mediaEntity.setClipId("clip-id");
        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(mediaEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );

        verify(objectRetrievalQueueRepository, times(1)).saveAndFlush(any());
        assertTrue(exception.getMessage().contains("No storedEodEntities found for mediaId"));
    }

    @Test
    void duplicateObjectRetrievalQueueNotAddedWhenRetrieveFileFromStorageMediaEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setContentObjectId("2");
        mediaEntity.setClipId("clip-id");

        ObjectRetrievalQueueEntity objectRetrievalQueueEntity = new ObjectRetrievalQueueEntity();

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        when(objectRetrievalQueueRepository.findMatchingObjectRetrievalQueueItem(mediaEntity,
                                                                                 null,
                                                                                 mediaEntity.getId().toString(),
                                                                                 mediaEntity.getContentObjectId(),
                                                                                 mediaEntity.getClipId())
        ).thenReturn(Optional.of(objectRetrievalQueueEntity));
        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(mediaEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );

        verify(objectRetrievalQueueRepository, times(0)).saveAndFlush(any());
        assertTrue(exception.getMessage().contains("No storedEodEntities found for mediaId"));
    }

    @Test
    void insertMediaEntityInObjectRetrievalQueueWhenNotFoundIn() {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(1);
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setContentObjectId("2");
        mediaEntity.setClipId("clip-id");
        mediaEntity.setCreatedBy(userAccount);
        mediaEntity.setLastModifiedBy(userAccount);

        ObjectRetrievalQueueEntity objectRetrievalQueueEntity = createObjectRetrievalQueueEntity(mediaEntity, userAccount);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(mediaEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );

        verify(objectRetrievalQueueRepository, times(1)).saveAndFlush(objectRetrievalQueueEntity);
    }

    private static @NotNull ObjectRetrievalQueueEntity createObjectRetrievalQueueEntity(MediaEntity mediaEntity, UserAccountEntity userAccount) {
        ObjectRetrievalQueueEntity objectRetrievalQueueEntity = new ObjectRetrievalQueueEntity();
        objectRetrievalQueueEntity.setMedia(mediaEntity);
        objectRetrievalQueueEntity.setCreatedBy(userAccount);
        objectRetrievalQueueEntity.setLastModifiedBy(userAccount);
        objectRetrievalQueueEntity.setParentObjectId(String.valueOf(mediaEntity.getId()));
        objectRetrievalQueueEntity.setContentObjectId(mediaEntity.getContentObjectId());
        objectRetrievalQueueEntity.setClipId(mediaEntity.getClipId());
        return objectRetrievalQueueEntity;
    }

    @Test
    void insertTranscriptionEntityInObjectRetrievalQueueWhenNotFoundIn() {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(1);
        TranscriptionDocumentEntity transcriptionDocumentEntity = createTranscriptionDocumentEntity(userAccount);

        ObjectRetrievalQueueEntity objectRetrievalQueueEntity = getObjectRetrievalQueueEntity(transcriptionDocumentEntity,
                                                                                              userAccount);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(transcriptionDocumentEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );

        verify(objectRetrievalQueueRepository, times(1)).saveAndFlush(objectRetrievalQueueEntity);
    }

    private static @NotNull ObjectRetrievalQueueEntity getObjectRetrievalQueueEntity(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                                     UserAccountEntity userAccount) {
        ObjectRetrievalQueueEntity objectRetrievalQueueEntity = new ObjectRetrievalQueueEntity();
        objectRetrievalQueueEntity.setTranscriptionDocument(transcriptionDocumentEntity);
        objectRetrievalQueueEntity.setCreatedBy(userAccount);
        objectRetrievalQueueEntity.setLastModifiedBy(userAccount);
        objectRetrievalQueueEntity.setParentObjectId(String.valueOf(transcriptionDocumentEntity.getId()));
        objectRetrievalQueueEntity.setContentObjectId(transcriptionDocumentEntity.getContentObjectId());
        objectRetrievalQueueEntity.setClipId(transcriptionDocumentEntity.getClipId());
        return objectRetrievalQueueEntity;
    }

    private static @NotNull TranscriptionDocumentEntity createTranscriptionDocumentEntity(UserAccountEntity userAccount) {
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(1);
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setId(1);
        transcriptionDocumentEntity.setContentObjectId("2");
        transcriptionDocumentEntity.setClipId("clip-id");
        transcriptionDocumentEntity.setUploadedBy(userAccount);
        transcriptionDocumentEntity.setLastModifiedBy(userAccount);
        return transcriptionDocumentEntity;
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
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(mediaEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );
    }

    @Test
    void retrieveFileFromStorageTranscriptionEmpty() throws Exception {
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, false));

        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setId(1);
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcription);
        transcriptionDocumentEntity.setContentObjectId("2");
        transcriptionDocumentEntity.setClipId("clip-id");

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelper,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(transcriptionDocumentEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
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
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(transcriptionDocumentEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
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
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        // make the assertion on the response
        var exception = assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(annotationDocumentEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
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
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        assertThrows(
            FileNotDownloadedException.class,
            () -> {
                try (DownloadResponseMetaData downloadResponseMetaDataAutoClose = dmFacade.retrieveFileFromStorage(annotationDocumentEntity)) {
                    // add for try close with resources
                    log.info(DOWNLOAD_RESPONSE_META_DATA, downloadResponseMetaDataAutoClose);
                }
            }
        );
    }

    @Test
    void testUnstructuredDataHelperCreate() {
        when(blobClientUploadResponseImpl.getBlobName()).thenReturn(UUID.randomUUID());

        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn("unstructured");
        when(dataManagementService.saveBlobData(anyString(), (InputStream) any())).thenReturn(blobClientUploadResponseImpl);

        UnstructuredDataHelper unstructuredDataHelperTest = getUnstructuredDataHelper();

        MediaEntity mediaEntity = new MediaEntity();
        ExternalObjectDirectoryEntity eodEntity = createEodEntity(unstructuredLocationEntity);
        eodEntity.setMedia(mediaEntity);
        ExternalObjectDirectoryEntity eodEntityToDelete = createEodEntity(unstructuredLocationEntity);
        eodEntityToDelete.setMedia(mediaEntity);
        BinaryData data = BinaryData.fromString("Test String");

        boolean created = unstructuredDataHelperTest.createUnstructuredDataFromEod(
            eodEntityToDelete,
            eodEntity,
            data.toStream());

        assertTrue(created);
    }

    @Test
    void testUnstructuredDataHelperCreateFailed() {

        UnstructuredDataHelper unstructuredDataHelperTest = getUnstructuredDataHelper();

        MediaEntity mediaEntity = new MediaEntity();
        ExternalObjectDirectoryEntity eodEntity = createEodEntity(unstructuredLocationEntity);
        eodEntity.setMedia(mediaEntity);
        ExternalObjectDirectoryEntity eodEntityToDelete = createEodEntity(unstructuredLocationEntity);
        eodEntityToDelete.setMedia(mediaEntity);
        BinaryData data = BinaryData.fromString("Test String");

        boolean created = unstructuredDataHelperTest.createUnstructuredDataFromEod(
            eodEntityToDelete,
            eodEntity,
            data.toStream());

        assertFalse(created);
    }

    @Test
    void testDownloadOfFacadeCreatesUnstructuredWhenUnstructuredNotFound() throws Exception {
        ExternalObjectDirectoryEntity inboundEntity = createEodEntity(inboundLocationEntity);
        ExternalObjectDirectoryEntity unstructuredEntity = createEodEntity(unstructuredLocationEntity);
        ExternalObjectDirectoryEntity armEntity = createEodEntity(armLocationEntity);

        when(externalObjectDirectoryRepository.findByEntityAndStatus(any(MediaEntity.class), any(ObjectRecordStatusEntity.class)))
            .thenReturn(List.of(inboundEntity, unstructuredEntity, armEntity));

        MediaEntity mediaEntity = new MediaEntity();
        final List<BlobContainerDownloadable> blobContainerDownloadables = new ArrayList<>();
        blobContainerDownloadables.add(setupDownloadableContainer(DatastoreContainerType.ARM, true));
        UnstructuredDataHelper unstructuredDataHelperFacade = getUnstructuredDataHelper();

        when(armApiService.downloadArmData(armEntity.getExternalRecordId(), armEntity.getExternalFileId())).thenReturn(downloadResponseMetaDataMock);

        // execute the code
        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(blobContainerDownloadables, externalObjectDirectoryRepository,
                                                                               objectRecordStatusRepository, storageOrderHelper, unstructuredDataHelperFacade,
                                                                               dataManagementConfiguration, armApiService, objectRetrievalQueueRepository);

        downloadResponseMetaData = dmFacade.retrieveFileFromStorage(mediaEntity);
        assertEquals(DatastoreContainerType.ARM, downloadResponseMetaData.getContainerTypeUsedToDownload());
    }

    @Test
    void createCopyInUnstructuredDatastore_whenDeleteFileOnCompletionIsTrue_shouldDeleteFile() throws IOException {
        ExternalObjectDirectoryEntity eodEntityToUpload = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity eodEntityToDelete = mock(ExternalObjectDirectoryEntity.class);
        File targetFile = Files.createTempFile("test", "txt").toFile();


        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(null, null,
                                                                               null, null, unstructuredDataHelper,
                                                                               null, null, null);

        assertThat(targetFile).exists();
        dmFacade.createCopyInUnstructuredDatastore(eodEntityToUpload, eodEntityToDelete, targetFile, true);

        verify(unstructuredDataHelper).createUnstructuredDataFromEod(eq(eodEntityToDelete), eq(eodEntityToUpload), any(FileInputStream.class));
        assertThat(targetFile).doesNotExist();
    }

    @Test
    void createCopyInUnstructuredDatastore_whenDeleteFileOnCompletionIsFalse_shouldNotDeleteFile() throws IOException {
        ExternalObjectDirectoryEntity eodEntityToUpload = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity eodEntityToDelete = mock(ExternalObjectDirectoryEntity.class);
        File targetFile = Files.createTempFile("test", "txt").toFile();


        final DataManagementFacadeImpl dmFacade = new DataManagementFacadeImpl(null, null,
                                                                               null, null, unstructuredDataHelper,
                                                                               null, null, null);

        assertThat(targetFile).exists();
        dmFacade.createCopyInUnstructuredDatastore(eodEntityToUpload, eodEntityToDelete, targetFile, false);

        verify(unstructuredDataHelper).createUnstructuredDataFromEod(eq(eodEntityToDelete), eq(eodEntityToUpload), any(FileInputStream.class));
        assertThat(targetFile).exists();
    }

    @Test
    //False positive as resources are mocked
    @SuppressWarnings("PMD.CloseResource")
    void processUnstructuredData_downloadResponseMetaDataIsFileBased_shouldNotCreateSecondTempFile() throws IOException {
        DatastoreContainerType datastoreContainerType = DatastoreContainerType.ARM;
        FileBasedDownloadResponseMetaData downloadResponseMetaData = mock(FileBasedDownloadResponseMetaData.class);
        ExternalObjectDirectoryEntity eodEntityToUpload = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity eodEntityToDelete = mock(ExternalObjectDirectoryEntity.class);

        File targetFile = mock(File.class);
        when(downloadResponseMetaData.getFileToBeDownloadedTo()).thenReturn(targetFile);


        final DataManagementFacadeImpl dmFacade = spy(new DataManagementFacadeImpl(null, null,
                                                                                   null, null, null,
                                                                                   null, null, null));

        doNothing().when(dmFacade).createCopyInUnstructuredDatastore(any(), any(), any(), anyBoolean());

        dmFacade
            .processUnstructuredData(datastoreContainerType, downloadResponseMetaData, eodEntityToUpload, eodEntityToDelete);

        verify(downloadResponseMetaData).getFileToBeDownloadedTo();
        verify(dmFacade).createCopyInUnstructuredDatastore(eodEntityToUpload, eodEntityToDelete, targetFile, false);
    }

    @Test
    //False positive as resources are mocked
    @SuppressWarnings("PMD.CloseResource")
    void processUnstructuredData_downloadResponseMetaDataIsNotFileBased_shouldCreateTempFile() throws IOException {
        DatastoreContainerType datastoreContainerType = DatastoreContainerType.ARM;
        DownloadResponseMetaData downloadResponseMetaData = mock(DownloadResponseMetaData.class);
        ExternalObjectDirectoryEntity eodEntityToUpload = mock(ExternalObjectDirectoryEntity.class);
        ExternalObjectDirectoryEntity eodEntityToDelete = mock(ExternalObjectDirectoryEntity.class);

        Resource resource = mock(Resource.class);
        when(downloadResponseMetaData.getResource()).thenReturn(resource);

        InputStream stream = new ByteArrayInputStream("Test String".getBytes(UTF_8));
        when(resource.getInputStream()).thenReturn(stream);

        DataManagementConfiguration dataManagementConfiguration = mock(DataManagementConfiguration.class);
        when(dataManagementConfiguration.getTempBlobWorkspace()).thenReturn(SOME_TEMP_WORKSPACE);
        final DataManagementFacadeImpl dmFacade = spy(new DataManagementFacadeImpl(null, null,
                                                                                   null, null, null,
                                                                                   dataManagementConfiguration, null, null));

        doNothing().when(dmFacade).createCopyInUnstructuredDatastore(any(), any(), any(), anyBoolean());

        dmFacade
            .processUnstructuredData(datastoreContainerType, downloadResponseMetaData, eodEntityToUpload, eodEntityToDelete);

        ArgumentCaptor<File> targetFileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        verify(dataManagementConfiguration).getTempBlobWorkspace();
        verify(downloadResponseMetaData).getResource();
        verify(resource).getInputStream();
        verify(dmFacade).createCopyInUnstructuredDatastore(eq(eodEntityToUpload), eq(eodEntityToDelete), targetFileArgumentCaptor.capture(), eq(true));

        File targetFileActual = targetFileArgumentCaptor.getValue();
        assertThat(targetFileActual.getAbsolutePath()).matches(".*/" + SOME_TEMP_WORKSPACE + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
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
                                                                 boolean processSuccess) throws IOException, FileNotDownloadedException {
        BlobContainerDownloadable downloadable = mock(BlobContainerDownloadable.class);

        BinaryData data = BinaryData.fromString("Test String");

        fileBasedDownloadResponseMetaData.setInputStream(data.toStream(), dataManagementConfiguration);

        lenient().when(downloadable.getContainerName(containerType)).thenReturn(Optional.of("test"));
        if (processSuccess) {
            lenient().when(downloadable
                               .downloadBlobFromContainer(eq(containerType),
                                                          Mockito.notNull())).thenReturn(fileBasedDownloadResponseMetaData);
        } else {
            lenient().when(downloadable
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