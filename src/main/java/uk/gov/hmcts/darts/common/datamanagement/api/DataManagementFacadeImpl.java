package uk.gov.hmcts.darts.common.datamanagement.api;

import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.audio.helper.UnstructuredDataHelper;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.datamanagement.helper.StorageOrderHelper;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRetrievalQueueEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRetrievalQueueRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods",//TODO - refactor to reduce methods when this class is next edited
    "PMD.GodClass"//TODO - refactor to reduce class size when this class is next edited
})
public class DataManagementFacadeImpl implements DataManagementFacade {

    private final List<BlobContainerDownloadable> supportedDownloadableContainers;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final StorageOrderHelper storageOrderHelper;
    private final UnstructuredDataHelper unstructuredDataHelper;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final ArmApiService armApiService;
    private final ObjectRetrievalQueueRepository objectRetrievalQueueRepository;

    @Override
    public DownloadResponseMetaData retrieveFileFromStorage(MediaEntity mediaEntity) throws FileNotDownloadedException {
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        List<ExternalObjectDirectoryEntity> storedEodEntities = externalObjectDirectoryRepository.findByEntityAndStatus(mediaEntity, storedStatus);
        if (CollectionUtils.isEmpty(storedEodEntities)) {
            String errorMessage = MessageFormat.format("No storedEodEntities found for mediaId {0,number,#}", mediaEntity.getId());
            log.error(errorMessage);

            createOrUpdateRetrievalQueue(mediaEntity,
                                         null,
                                         mediaEntity.getId().toString(),
                                         mediaEntity.getContentObjectId(),
                                         mediaEntity.getClipId());

            throw new FileNotDownloadedException(errorMessage);
        }
        try {
            return getDataFromStorage(storedEodEntities);
        } catch (FileNotDownloadedException fnde) {
            log.error("Could not retrieve file from any storage for mediaId {}", mediaEntity.getId());
            throw fnde;
        }
    }

    @Override
    public DownloadResponseMetaData retrieveFileFromStorage(TranscriptionDocumentEntity transcriptionDocumentEntity) throws FileNotDownloadedException {
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        List<ExternalObjectDirectoryEntity> storedEodEntities = externalObjectDirectoryRepository.findByEntityAndStatus(transcriptionDocumentEntity,
                                                                                                                        storedStatus);
        if (CollectionUtils.isEmpty(storedEodEntities)) {
            String errorMessage = MessageFormat.format("No storedEodEntities found for transcriptionDocumentId {0,number,#}",
                                                       transcriptionDocumentEntity.getId());
            log.error(errorMessage);

            createOrUpdateRetrievalQueue(null,
                                         transcriptionDocumentEntity,
                                         transcriptionDocumentEntity.getTranscription().getId().toString(),
                                         transcriptionDocumentEntity.getContentObjectId(),
                                         transcriptionDocumentEntity.getClipId());

            throw new FileNotDownloadedException(errorMessage);
        }
        try {
            return getDataFromStorage(storedEodEntities);
        } catch (FileNotDownloadedException fnde) {
            log.error("Could not retrieve file from any storage for transcriptionDocumentId {}", transcriptionDocumentEntity.getId());
            throw fnde;
        }
    }

    @Override
    public DownloadResponseMetaData retrieveFileFromStorage(AnnotationDocumentEntity annotationDocumentEntity) throws FileNotDownloadedException {
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        List<ExternalObjectDirectoryEntity> storedEodEntities = externalObjectDirectoryRepository.findByEntityAndStatus(annotationDocumentEntity, storedStatus);
        if (CollectionUtils.isEmpty(storedEodEntities)) {
            String errorMessage = MessageFormat.format("No storedEodEntities found for annotationDocumentId {0,number,#}", annotationDocumentEntity.getId());
            log.error(errorMessage);
            throw new FileNotDownloadedException(errorMessage);
        }
        try {
            return getDataFromStorage(storedEodEntities);
        } catch (FileNotDownloadedException fnde) {
            log.error("Could not retrieve file from any storage for annotationDocumentId {}", annotationDocumentEntity.getId());
            throw fnde;
        }
    }

    @Override
    public DownloadResponseMetaData retrieveFileFromStorage(List<ExternalObjectDirectoryEntity> eodEntities) throws FileNotDownloadedException {
        if (CollectionUtils.isEmpty(eodEntities)) {
            log.error("Supplied list of EodEntities is empty");
            throw new FileNotDownloadedException("Supplied list of EodEntities is empty");
        }

        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        List<ExternalObjectDirectoryEntity> storedEodEntities = eodEntities.stream().filter(eodEntity -> eodEntity.getStatus().equals(storedStatus)).toList();
        if (CollectionUtils.isEmpty(storedEodEntities)) {
            String errorMessage = "Supplied list of EodEntities does not have any that are stored";
            log.error(errorMessage);
            throw new FileNotDownloadedException(errorMessage);
        }
        try {
            return getDataFromStorage(storedEodEntities);
        } catch (FileNotDownloadedException fnde) {
            log.error("Could not retrieve file from any storage for eodId: {}", eodEntities.getFirst().getId());
            throw fnde;
        }
    }

    private DownloadResponseMetaData retrieveFileFromStorage(DatastoreContainerType datastoreType, ExternalObjectDirectoryEntity eodEntity,
                                                             BlobContainerDownloadable container) throws FileNotDownloadedException {
        try {
            if (datastoreType == ARM) {
                return armApiService.downloadArmData(eodEntity.getExternalRecordId(), eodEntity.getExternalFileId());
            } else {
                return container.downloadBlobFromContainer(datastoreType, eodEntity);
            }
        } catch (UncheckedIOException | BlobStorageException e) {
            throw new FileNotDownloadedException(eodEntity.getExternalLocation(), datastoreType.name(), "Error downloading blob", e);
        }
    }

    /**
     * Loop through each storage type in order to see if it has a matched EodEntity, and if it does, try to download the file from there, if it fails,
     * move to the next one, if they all fail then throw a FileNotDownloadedException.
     */
    private DownloadResponseMetaData getDataFromStorage(List<ExternalObjectDirectoryEntity> storedEodEntities) throws FileNotDownloadedException {
        List<DatastoreContainerType> storageOrder = storageOrderHelper.getStorageOrder();
        StringBuilder logBuilder = new StringBuilder(134)
            .append("Starting to search for files with ")
            .append(storedEodEntities.size())
            .append(" eodEntities\n");

        ExternalObjectDirectoryEntity eodEntityToDelete = null;
        for (DatastoreContainerType datastoreContainerType : storageOrder) {
            logBuilder.append("checking container ")
                .append(datastoreContainerType.name())
                .append('\n');
            ExternalObjectDirectoryEntity eodEntity = findCorrespondingEodEntityForStorageLocation(storedEodEntities, datastoreContainerType);
            if (eodEntity == null) {
                logBuilder.append("matching eodEntity not found for ")
                    .append(datastoreContainerType.name())
                    .append('\n');
                continue;
            }
            if (datastoreContainerType.equals(DatastoreContainerType.UNSTRUCTURED)) {
                eodEntityToDelete = eodEntity;
            }
            Optional<BlobContainerDownloadable> container = getSupportedContainer(datastoreContainerType);
            if (container.isEmpty()) {
                logBuilder.append("Supporting Container ").append(datastoreContainerType.name()).append(" not found\n");
                continue;
            }
            log.info("Downloading blob id {} from container {}", eodEntity.getExternalLocation(), datastoreContainerType.name());

            try {
                DownloadResponseMetaData downloadResponseMetaData = retrieveFileFromStorage(datastoreContainerType, eodEntity, container.get());
                downloadResponseMetaData.setEodEntity(eodEntity);
                downloadResponseMetaData.setContainerTypeUsedToDownload(datastoreContainerType);
                processUnstructuredData(datastoreContainerType, downloadResponseMetaData, eodEntity, eodEntityToDelete);
                return downloadResponseMetaData;
            } catch (FileNotDownloadedException | IOException e) {
                String logMessage = MessageFormat.format("Could not download file for eodEntity ''{0,number,#}''", eodEntity.getId());
                logBuilder.append(logMessage)
                    .append('\n');
                log.error(logMessage, e);
            }
        }
        throw new FileNotDownloadedException(logBuilder.toString());
    }

    @SuppressWarnings("PMD.CloseResource")//TODO - ensure resource is closed after use to prevent memory leaks
    void processUnstructuredData(
        DatastoreContainerType datastoreContainerType,
        DownloadResponseMetaData downloadResponseMetaData,
        ExternalObjectDirectoryEntity eodEntityToUpload,
        ExternalObjectDirectoryEntity eodEntityToDelete) throws IOException {

        if (!ARM.equals(datastoreContainerType)) {
            return;
        }

        File targetFile;

        boolean deleteFileOnCompletion = true;
        if (downloadResponseMetaData instanceof FileBasedDownloadResponseMetaData fileBasedDownloadResponseMetaData) {
            targetFile = fileBasedDownloadResponseMetaData.getFileToBeDownloadedTo();
            deleteFileOnCompletion = false;
        } else {
            String tempBlobPath = dataManagementConfiguration.getTempBlobWorkspace() + "/" + UUID.randomUUID().toString();
            targetFile = new File(tempBlobPath);
            FileUtils.copyInputStreamToFile(downloadResponseMetaData.getResource().getInputStream(), targetFile);
        }
        createCopyInUnstructuredDatastore(eodEntityToUpload, eodEntityToDelete, targetFile, deleteFileOnCompletion);
    }

    /**
     * Creates a copy in the unstructured data store for quicker retrieval next time.
     */
    void createCopyInUnstructuredDatastore(
        ExternalObjectDirectoryEntity eodEntityToUpload,
        ExternalObjectDirectoryEntity eodEntityToDelete,
        File targetFile,
        boolean deleteFileOnCompletion) {

        try (InputStream inputStream = Files.newInputStream(targetFile.toPath())) {
            unstructuredDataHelper.createUnstructuredDataFromEod(
                eodEntityToDelete,
                eodEntityToUpload,
                inputStream
            );

            if (deleteFileOnCompletion) {
                try {
                    Files.delete(targetFile.toPath());
                } catch (IOException e) {
                    log.error("Unable to delete temporary file {}", targetFile.getPath(), e);
                }
            }
        } catch (Exception e) {
            log.warn("unable to store a copy of EOD {} in the unstructured Datastore.", eodEntityToUpload.getId(), e);
        }
    }

    private ExternalObjectDirectoryEntity findCorrespondingEodEntityForStorageLocation(List<ExternalObjectDirectoryEntity> storedEodEntities,
                                                                                       DatastoreContainerType datastoreContainerType) {
        Optional<ExternalLocationTypeEnum> externalLocationTypeEnumOpt = datastoreContainerType.getExternalLocationTypeEnum();
        if (externalLocationTypeEnumOpt.isEmpty()) {
            return null;
        }
        Integer locationTypeId = externalLocationTypeEnumOpt.get().getId();
        return storedEodEntities.stream().filter(storedEntity -> storedEntity.getExternalLocationType().getId().equals(locationTypeId)).findAny().orElse(null);
    }

    private Optional<BlobContainerDownloadable> getSupportedContainer(DatastoreContainerType typeToFind) {
        return supportedDownloadableContainers.stream().filter(type -> type.getContainerName(typeToFind).isPresent())
            .findAny();
    }

    private void createOrUpdateRetrievalQueue(MediaEntity mediaEntity,
                                              TranscriptionDocumentEntity transcriptionDocumentEntity,
                                              String parentObjectId,
                                              String contentObjectId,
                                              String clipId) {

        var doesObjectRetrievalQueueExist = objectRetrievalQueueRepository.findMatchingObjectRetrievalQueueItem(mediaEntity,
                                                                                                                transcriptionDocumentEntity,
                                                                                                                parentObjectId,
                                                                                                                contentObjectId,
                                                                                                                clipId);

        if (doesObjectRetrievalQueueExist.isPresent()) {
            log.info("Object retrieval queue items already exists. No action taken");
            return;
        }

        createExternalRetrievalQueueEntity(mediaEntity,
                                           transcriptionDocumentEntity,
                                           parentObjectId,
                                           contentObjectId,
                                           clipId);
    }

    private void createExternalRetrievalQueueEntity(MediaEntity media,
                                                    TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                    String parentObjectId,
                                                    String contentObjectId,
                                                    String clipId) {

        ObjectRetrievalQueueEntity objectRetrievalQueueEntity = new ObjectRetrievalQueueEntity();

        if (nonNull(media)) {
            objectRetrievalQueueEntity.setMedia(media);
            objectRetrievalQueueEntity.setCreatedById(media.getCreatedById());
            objectRetrievalQueueEntity.setLastModifiedById(media.getLastModifiedById());
        }
        if (nonNull(transcriptionDocumentEntity)) {
            objectRetrievalQueueEntity.setTranscriptionDocument(transcriptionDocumentEntity);
            objectRetrievalQueueEntity.setCreatedBy(transcriptionDocumentEntity.getUploadedBy());
            objectRetrievalQueueEntity.setLastModifiedById(transcriptionDocumentEntity.getLastModifiedById());
        }

        objectRetrievalQueueEntity.setParentObjectId(parentObjectId);
        objectRetrievalQueueEntity.setContentObjectId(contentObjectId);
        objectRetrievalQueueEntity.setClipId(clipId);

        objectRetrievalQueueRepository.saveAndFlush(objectRetrievalQueueEntity);
    }

}