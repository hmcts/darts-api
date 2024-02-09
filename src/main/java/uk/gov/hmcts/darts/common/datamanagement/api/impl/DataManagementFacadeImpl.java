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

import java.io.InputStream;
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

    // The order to process the download data containers
    private static final List<DatastoreContainerType> CONTAINER_PROCESSING_ORDER = new ArrayList<>();

    {
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.UNSTRUCTURED);
        CONTAINER_PROCESSING_ORDER.add(DatastoreContainerType.DETS);
    }

    @Override
    public void getDataFromUnstructuredArmAndDetsBlobs(Collection<DownloadableExternalObjectDirectory> dataToDownloadList,
                                                       Function<DownloadableExternalObjectDirectory, Boolean> processed) {

        getDataFromUnstructuredArmAndDetsBlobs(dataToDownloadList, true, processed);
    }

    @Override
    public void getDataFromUnstructuredArmAndDetsBlobs(Collection<DownloadableExternalObjectDirectory> dataToDownloadList,
                                                       boolean isFetchfromDets,
                                                       Function<DownloadableExternalObjectDirectory, Boolean> processed) {
        dataToDownloadList.forEach(dataToDownload -> {
            CONTAINER_PROCESSING_ORDER.forEach(type -> {
                Optional<BlobContainerDownloadable> container = getSupportedContainer(type);
                if (container.isPresent()) {
                    Optional<String> containerName = container.get().getContainerName(type);
                    if (containerName.isPresent() && processObjectDirectoryForContainerType(dataToDownload, type, isFetchfromDets)) {
                        log.info("Downloading blob id {} from container {}", dataToDownload.getDirectory().getExternalLocationType(), type.name());

                        if (!dataToDownload.getResponse().isSuccessfullyDownloaded()) {
                            boolean success = container.get().downloadBlobFromContainer(type,
                                                                                        dataToDownload.getDirectory().getExternalLocation(),
                                                                                        dataToDownload.getResponse());
                            if (!success) {
                                log.info("Failed to download blob id {} from container {}, " +
                                                 "continuing to process...", dataToDownload.getDirectory().getExternalLocationType(), type.name());
                            }

                            // if we have been asked to stop processing then fail fast and do not process the rest of the downloads
                            if (!processResponse(dataToDownload, success, processed, type)) {
                                return;
                            }
                        }
                    }
                }

                // if all download blob implementations fail use arm
                processArmFallback(dataToDownload, processed);
            });
        });
    }

    private void processArmFallback(DownloadableExternalObjectDirectory dataToDownload, Function<DownloadableExternalObjectDirectory, Boolean> processed) {
        // fall back to arm download if we have not
        if (!dataToDownload.getResponse().isSuccessfullyDownloaded()) {
            if (dataToDownload.getDirectory().isForLocationType(ExternalLocationTypeEnum.UNSTRUCTURED)) {

                try {
                    log.info("Attempting download using ARM to download blob id {} from container {}, " +
                                     "continuing to process...", dataToDownload.getDirectory().getExternalLocationType(), DatastoreContainerType.ARM);

                    InputStream stream = armDataManagementApi.downloadArmData(dataToDownload.getDirectory().getExternalFileId(),

                                                                              dataToDownload.getDirectory().getExternalRecordId());
                    dataToDownload.getResponse().markInputStream(stream);

                    log.info("Successful download using ARM to download blob id {} from container {}, " +
                                     "continuing to process...", dataToDownload.getDirectory().getExternalLocationType(), DatastoreContainerType.ARM);

                    processResponse(dataToDownload, true, processed, DatastoreContainerType.ARM);
                } catch (Exception e) {
                    log.error("Download failed from arm fallback. Giving up", e);
                    processResponse(dataToDownload, false, processed, DatastoreContainerType.ARM);
                }
            }
        }
    }

    private boolean processObjectDirectoryForContainerType(
            DownloadableExternalObjectDirectory externalObjectDirectory, DatastoreContainerType type,
            boolean fetchFromDets) {
        return type != DatastoreContainerType.DETS || fetchFromDets
                && externalObjectDirectory.getDirectory().isForLocationType(ExternalLocationTypeEnum.TEMPSTORE);
    }


    private Optional<BlobContainerDownloadable> getSupportedContainer(DatastoreContainerType typeToFind) {
        return supportedDownloadableContainers.stream().filter(type -> type.getContainerName(typeToFind).isPresent())
                .findFirst();
    }

    private boolean processResponse(DownloadableExternalObjectDirectory download,
                         boolean downloadSuccess, Function<DownloadableExternalObjectDirectory, Boolean> processed,
                         DatastoreContainerType processContainer) {
        download.getResponse().markProcessed();
        if (downloadSuccess) {
            download.getResponse().markSuccess(processContainer);
        }
        return processed.apply(download);
    }
}