package uk.gov.hmcts.darts.datamanagement.model;

import java.util.Map;
import java.util.UUID;

public interface BlobClientUploadResponse {

    UUID getBlobName();

    Long getBlobSize();

    Map<String, String> addMetadata(Map<String, String> additionalMetadata);

}
