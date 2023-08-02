package uk.gov.hmcts.darts.datamanagement.api;

import com.azure.core.util.BinaryData;

import java.util.UUID;

public interface DataManagementApi {

    BinaryData getBlobDataFromUnstructuredContainer(UUID blobId);

    UUID saveBlobDataToOutboundContainer(BinaryData binaryData);

}
