package uk.gov.hmcts.darts.dets.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.dets.api.DetsDataManagementApi;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.dets.service.DetsApiService;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetsDataManagementApiImpl implements DetsDataManagementApi {

    private final DetsApiService service;

    private final DetsDataManagementConfiguration detsManagementConfiguration;

    @Override
    public DownloadResponseMetaData downloadBlobFromContainer(DatastoreContainerType container,
                                                              ExternalObjectDirectoryEntity blobId) throws FileNotDownloadedException {
        Optional<String> containerName = getContainerName(container);
        if (containerName.isPresent()) {
            return service.downloadData(blobId.getExternalLocation());
        }
        throw new FileNotDownloadedException("Container for " + container.name() + " not found in DETS.");
    }

    public Optional<String> getContainerName(DatastoreContainerType datastoreContainerType) {
        if (Objects.requireNonNull(datastoreContainerType) == DatastoreContainerType.DETS) {
            return Optional.of(detsManagementConfiguration.getContainerName());
        }
        return Optional.empty();
    }

    @Override
    public StorageConfiguration getConfiguration() {
        return detsManagementConfiguration;
    }
}