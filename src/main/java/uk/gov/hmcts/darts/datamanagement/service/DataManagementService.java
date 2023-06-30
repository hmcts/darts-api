package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.util.BinaryData;

import java.util.UUID;

public interface DataManagementService {
    BinaryData getBlobData(String containerName, UUID uniqueBlobName);

    UUID saveBlobData(String containerName, BinaryData audioData);
}
