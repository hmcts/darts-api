package uk.gov.hmcts.darts.common.datamanagement.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.common.datamanagement.api.BlobContainerDownloadable;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectory;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
@Service
public class DataManagementFacadeImpl implements DataManagementFacade {

    private final List<BlobContainerDownloadable> supportedDownloadableContainers;

    private final ArmDataManagementApi armDataManagementApi;

    private final DetsDataManagementConfiguration configuration;

    // The order to process the download data containers
    private static final List<DatastoreContainerType> CONTAINER_PROCESSING_ORDER = new ArrayList<>();

    {
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.UNSTRUCTURED);
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.DETS);
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.ARM);
    }

    @Override
    public void getDataFromUnstructuredArmAndDetsBlobs(Collection<DownloadableExternalObjectDirectory> dataToDownloadList) {

        getDataFromUnstructuredArmAndDetsBlobs(dataToDownloadList, (downloadData) -> true);
    }

    @Override
    public void getDataFromUnstructuredArmAndDetsBlobs(Collection<DownloadableExternalObjectDirectory> dataToDownloadList,
                                                       Function<DownloadableExternalObjectDirectory, Boolean> processed) {

        // process for all standard blob stores
        dataToDownloadList.forEach(dataToDownload -> {
            CONTAINER_PROCESSING_ORDER.forEach(type -> {
                Optional<BlobContainerDownloadable> container = getSupportedContainer(type);
                if (container.isPresent()) {
                    Optional<String> containerName = container.get().getContainerName(type);
                    if (containerName.isPresent() &&
                            dataToDownload.getDirectory().isForLocationType(getForDatastoreContainerType(type))
                            && processObjectDirectoryForContainerType(dataToDownload, type, configuration.isFetchFromDets())) {
                            log.info("Downloading blob id {} from container {}", dataToDownload.getDirectory().getExternalLocationType(), type.name());

                            if (!dataToDownload.getResponse().isSuccessfullyDownloaded()) {
                                boolean success = false;
                                try {
                                    success = container.get().downloadBlobFromContainer(type,
                                                                                                           dataToDownload.getDirectory(),
                                                                                                           dataToDownload.getResponse());
                                }
                                catch (Exception e) {
                                    log.error("Error occured working out wether to continue", e);
                                };

                                if (!success) {
                                    log.info("Failed to download blob id {} from container {}, " +
                                                     "continuing to process...", dataToDownload.getDirectory().getExternalLocationType(), type.name());
                                }

                                // if we have been asked to stop processing then fail fast and do not process the rest of the downloads
                                if (!processResponse(dataToDownload, success, processed, type)) {
                                    log.info("Forcibly ending the download process by client");
                                    return;
                                }
                            }
                    }
                }

                // now fallback as we have not found any way of processing the download
                processFallback(dataToDownload, processed);
            });
        });
    }

    /**
     * TODO: Implement this method to do something as a fallback
     */
    private void processFallback(DownloadableExternalObjectDirectory dataToDownload,
                                 Function<DownloadableExternalObjectDirectory, Boolean> processed) {
        boolean success = false;

        // do something and return a success of failure


        // mark the download as passed fail etc. Fail by default
        //processResponse(dataToDownload, success, processed, DatastoreContainerType.ARM);
    }

    private boolean processObjectDirectoryForContainerType(
            DownloadableExternalObjectDirectory externalObjectDirectory, DatastoreContainerType type,
            boolean fetchFromDets) {
        return type != DatastoreContainerType.DETS || fetchFromDets
                && externalObjectDirectory.getDirectory().isForLocationType(ExternalLocationTypeEnum.TEMPSTORE);
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

    private boolean processResponse(DownloadableExternalObjectDirectory download,
                         boolean downloadSuccess, Function<DownloadableExternalObjectDirectory, Boolean> processed,
                         DatastoreContainerType processContainer) {
        download.getResponse().markProcessed(processContainer);
        if (downloadSuccess) {
            download.getResponse().markSuccess();
        }
        return processed.apply(download);
    }
}