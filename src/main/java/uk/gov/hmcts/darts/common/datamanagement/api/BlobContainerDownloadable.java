package uk.gov.hmcts.darts.common.datamanagement.api;

import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.util.Optional;

public interface BlobContainerDownloadable {
    /**
     * downloads a blob id from a container.
     */
    DownloadResponseMetaData downloadBlobFromContainer(DatastoreContainerType container,
                                                       ExternalObjectDirectoryEntity blobId) throws FileNotDownloadedException;

    /**
     * gets the supported container types.
     *
     * @return The option name, empty if not supported
     */
    Optional<String> getContainerName(DatastoreContainerType datastoreContainerType);

    /**
     * gets the configuration for the implementation.
     *
     * @return the temp storage location
     */
    StorageConfiguration getConfiguration();
}