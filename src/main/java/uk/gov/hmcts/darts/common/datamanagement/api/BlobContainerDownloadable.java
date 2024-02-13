package uk.gov.hmcts.darts.common.datamanagement.api;

import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.Optional;

public interface BlobContainerDownloadable {
    /**
     * downloads a blob id from a container.
     * @param container The container name
     * @param blobId The blob id to download
     * @param response The response to download to
     * @return true or false signifying that the download was processed. It offers no indication as to download success
     */
    boolean downloadBlobFromContainer(DatastoreContainerType container, ExternalObjectDirectoryEntity blobId, DownloadResponseMetaData response);

    /**
     * gets the supported container types.
     * @return The option name, empty if not supported
     */
    Optional<String> getContainerName(DatastoreContainerType datastoreContainerType);
}