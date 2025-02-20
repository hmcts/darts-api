package uk.gov.hmcts.darts.datamanagement.model;

import com.azure.storage.blob.BlobClient;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper/facade around BlobClient to expose properties useful to upload callers.
 */
public class BlobClientUploadResponseImpl implements BlobClientUploadResponse {

    private final BlobClient blobClient;

    private Long blobSize;

    public BlobClientUploadResponseImpl(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    @Override
    public String getBlobName() {
        return blobClient.getBlobName();
    }

    @Override
    public Long getBlobSize() {
        // This triggers a network call via blobClient, so we only fetch it if we need to
        if (blobSize == null) {
            blobSize = blobClient.getProperties().getBlobSize();
        }
        return blobSize;
    }

    /**
     * Retrospectively adds additional metadata to the uploaded blob.
     *
     * @param additionalMetadata Metadata to add to the blob
     * @return A map containing all metadata applied to the blob.
     */
    @Override
    public Map<String, String> addMetadata(Map<String, String> additionalMetadata) {
        var metadata = new HashMap<>(blobClient.getProperties().getMetadata());
        metadata.putAll(additionalMetadata);

        // Triggers a network call via blobClient
        blobClient.setMetadata(metadata);

        return metadata;
    }

}
