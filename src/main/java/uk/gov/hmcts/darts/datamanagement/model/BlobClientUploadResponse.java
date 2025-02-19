package uk.gov.hmcts.darts.datamanagement.model;

import java.util.Map;

public interface BlobClientUploadResponse {

    String getBlobName();

    Long getBlobSize();

    Map<String, String> addMetadata(Map<String, String> additionalMetadata);

}
