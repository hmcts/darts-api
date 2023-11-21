package uk.gov.hmcts.darts.datamanagement.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.util.UUID;

public interface DataManagementApi {

    BinaryData getBlobDataFromUnstructuredContainer(UUID blobId);

    BinaryData getBlobDataFromOutboundContainer(UUID blobId);

    UUID saveBlobDataToOutboundContainer(BinaryData binaryData);

    void deleteBlobDataFromOutboundContainer(UUID blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromInboundContainer(UUID blobId) throws AzureDeleteBlobException;

    void deleteBlobDataFromUnstructuredContainer(UUID blobId) throws AzureDeleteBlobException;

    UUID saveBlobDataToInboundContainer(BinaryData binaryData);

}
