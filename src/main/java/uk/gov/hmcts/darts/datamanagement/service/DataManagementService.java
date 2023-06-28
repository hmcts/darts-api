package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.util.BinaryData;

import java.util.UUID;

public interface DataManagementService {
    BinaryData getAudioBlobData(String containerName, UUID uniqueBlobName);

    UUID saveAudioBlobData(String containerName, BinaryData audioData);
}
