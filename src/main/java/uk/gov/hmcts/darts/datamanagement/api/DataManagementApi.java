package uk.gov.hmcts.darts.datamanagement.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.Optional;
import java.util.UUID;

public interface DataManagementApi {

    BinaryData getBlobDataFromUnstructuredContainer(UUID blobId);

    BinaryData getBlobDataFromOutboundContainer(UUID blobId);

    UUID saveBlobDataToOutboundContainer(BinaryData binaryData);

    void deleteBlobDataFromOutboundContainer(UUID blobId);

    Optional<UUID> getMediaLocation(MediaEntity media);

}
