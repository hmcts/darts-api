package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.util.BinaryData;

import java.io.InputStream;
import java.util.UUID;

public interface DataManagementService {
    BinaryData getBlobData(String containerName, UUID blobId);

    UUID saveBlobData(String containerName, BinaryData binaryData);

    UUID saveBlobData(String containerName, InputStream inputStream);

    void deleteBlobData(String containerName, UUID blobId);
}
