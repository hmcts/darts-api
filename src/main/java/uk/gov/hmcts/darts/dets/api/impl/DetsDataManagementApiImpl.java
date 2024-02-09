package uk.gov.hmcts.darts.dets.api.impl;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.ResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.dets.api.DetsDataManagementApi;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.dets.service.DetsApiService;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class DetsDataManagementApiImpl implements DetsDataManagementApi {

    private final DetsApiService service;

    private final DetsDataManagementConfiguration detsManagementConfiguration;

    @Override
    public boolean downloadBlobFromContainer(DatastoreContainerType container, ExternalObjectDirectoryEntity blobId, ResponseMetaData request) {
        Optional<String> containerName = getContainerName(container);
        if (containerName.isPresent()) {
            service.downloadData(blobId.getExternalLocation(), request);
        }

        return request.isSuccessfullyDownloaded();
    }

    public Optional<String> getContainerName(DatastoreContainerType datastoreContainerType) {
        switch (datastoreContainerType) {
            case DETS -> {
                return Optional.of(detsManagementConfiguration.getContainerName());
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}