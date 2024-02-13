package uk.gov.hmcts.darts.common.datamanagement.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class DataManagementFacadeImpl implements DataManagementFacade {

    private final List<BlobContainerDownloadable> supportedDownloadableContainers;

    private final DetsDataManagementConfiguration configuration;

    // The order to process the download data containers
    private static final List<DatastoreContainerType> CONTAINER_PROCESSING_ORDER = new ArrayList<>();

    {
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.UNSTRUCTURED);
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.DETS);
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.ARM);
    }

    @Override
    public void getDataFromUnstructuredArmAndDetsBlobs(DownloadableExternalObjectDirectories downloadableExternalObjectDirectories) {

        // process for all standard blob stores
        downloadableExternalObjectDirectories.getEntities().forEach(dataToDownload -> {
            CONTAINER_PROCESSING_ORDER.forEach(type -> {
                Optional<BlobContainerDownloadable> container = getSupportedContainer(type);
                if (container.isPresent() && !downloadableExternalObjectDirectories.getResponse().isProcessedByContainer()) {
                    Optional<String> containerName = container.get().getContainerName(type);
                    if (containerName.isPresent()
                            && dataToDownload.isForLocationType(getForDatastoreContainerType(type))
                            && processObjectDirectoryForContainerType(dataToDownload, type, configuration.isFetchFromDetsEnabled())) {
                            log.info("Downloading blob id {} from container {}", dataToDownload.getExternalLocationType(), type.name());

                            if (!downloadableExternalObjectDirectories.getResponse().isSuccessfullyDownloaded()) {
                                boolean success = false;
                                try {
                                    success = container.get().downloadBlobFromContainer(type,
                                                                                        dataToDownload, downloadableExternalObjectDirectories.getResponse());
                                } catch (UncheckedIOException e) {
                                    log.error("Error occurred working out whether to continue", e);
                                }

                                if (!success) {
                                    log.info("Failed to download blob id {} from container {}, " +
                                                     "continuing to process...", dataToDownload.getExternalLocationType(), type.name());
                                }

                                // if we have been asked to stop processing then fail fast and do not process the rest of the downloads
                                processResponse(downloadableExternalObjectDirectories, success,  type);
                            }
                    }
                }
            });
        });

        if (!downloadableExternalObjectDirectories.getResponse().isProcessedByContainer()) {

            log.info("Downloading was not attempted, Falling back...");

            // now fallback as we have not found any way of processing the download
            processFallback(downloadableExternalObjectDirectories);
        }
    }

    /**
     * TODO: Implement this method to do something as a fallback.
     */
    private void processFallback(DownloadableExternalObjectDirectories downloadableExternalObjectDirectories) {

        // The success will always be false if we get here
        boolean success = downloadableExternalObjectDirectories.getResponse().isSuccessfullyDownloaded();

        // This will always return true if we get here
        boolean processedByDataManagementApi = downloadableExternalObjectDirectories.getResponse().isProcessedByContainer();

        // mark the download as passed fail etc. Fail by default
        //processResponse(dataToDownload, success, processed, DatastoreContainerType.ARM);
    }

    private boolean processObjectDirectoryForContainerType(
            ExternalObjectDirectoryEntity externalObjectDirectory, DatastoreContainerType type,
            boolean fetchFromDets) {
        return type != DatastoreContainerType.DETS || fetchFromDets
                && externalObjectDirectory.isForLocationType(ExternalLocationTypeEnum.TEMPSTORE);
    }

    private ExternalLocationTypeEnum getForDatastoreContainerType(DatastoreContainerType type) {
        if (type == DatastoreContainerType.DETS) {
            return ExternalLocationTypeEnum.TEMPSTORE;
        } else if (type == DatastoreContainerType.ARM) {
            return ExternalLocationTypeEnum.ARM;
        } else if (type == DatastoreContainerType.UNSTRUCTURED) {
            return ExternalLocationTypeEnum.UNSTRUCTURED;
        }

        // no match found
        return null;
    }

    private Optional<BlobContainerDownloadable> getSupportedContainer(DatastoreContainerType typeToFind) {
        return supportedDownloadableContainers.stream().filter(type -> type.getContainerName(typeToFind).isPresent())
                .findFirst();
    }

    private void processResponse(DownloadableExternalObjectDirectories download,
                                 boolean downloadSuccess,
                                 DatastoreContainerType processContainer) {
        if (downloadSuccess) {
            download.getResponse().markSuccess(processContainer);
        } else {
            download.getResponse().markFailure(processContainer);
        }
    }
}