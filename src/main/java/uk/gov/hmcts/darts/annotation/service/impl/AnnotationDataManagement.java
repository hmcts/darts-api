package uk.gov.hmcts.darts.annotation.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.UUID;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationDataManagement {

    private final DataManagementApi dataManagementApi;

    public ExternalBlobLocations upload(BinaryData binaryData, String filename) {
        UUID inboundLocation = null;
        UUID unstructuredLocation;
        try {
            inboundLocation = dataManagementApi.saveBlobDataToInboundContainer(binaryData);
            unstructuredLocation = dataManagementApi.saveBlobDataToUnstructuredContainer(binaryData);
        } catch (RuntimeException e) {
            if (inboundLocation != null) {
                log.error("Failed to upload annotation document {} to unstructured container", filename, e);
                attemptToDeleteDocument(inboundLocation);
            } else {
                log.error("Failed to upload annotation document {} to inbound container", filename, e);
            }
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }

        return new ExternalBlobLocations(inboundLocation, unstructuredLocation);
    }


    public InputStreamResource download(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        try {
            return new InputStreamResource(
                dataManagementApi.getBlobDataFromInboundContainer(externalObjectDirectoryEntity.getExternalLocation()).toStream());
        } catch (RuntimeException e) {
            log.error("Failed to download annotation document {}", externalObjectDirectoryEntity.getId(), e);
            throw new DartsApiException(INTERNAL_SERVER_ERROR, e);
        }
    }

    public void attemptToDeleteDocument(UUID externalLocation) {
        try {
            dataManagementApi.deleteBlobDataFromInboundContainer(externalLocation);
        } catch (AzureDeleteBlobException e) {
            log.error("Failed to delete orphaned annotation document {}", externalLocation, e);
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }
    }

    public record ExternalBlobLocations(UUID inboundLocation, UUID unstructuredLocation) {}
}
