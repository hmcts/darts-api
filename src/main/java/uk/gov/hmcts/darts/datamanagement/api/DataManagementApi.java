package uk.gov.hmcts.darts.datamanagement.api;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.common.exception.AzureException;

import java.util.UUID;

public interface DataManagementApi {

    BinaryData getBlobDataFromUnstructuredContainer(UUID blobId);

    BinaryData getBlobDataFromOutboundContainer(UUID blobId);

    UUID saveBlobDataToOutboundContainer(BinaryData binaryData);

    void deleteBlobDataFromOutboundContainer(UUID blobId) throws AzureException;

    void deleteBlobDataFromInboundContainer(UUID blobId) throws AzureException;

    void deleteBlobDataFromUnstructuredContainer(UUID blobId) throws AzureException;

    UUID saveBlobDataToInboundContainer(BinaryData binaryData);

}
