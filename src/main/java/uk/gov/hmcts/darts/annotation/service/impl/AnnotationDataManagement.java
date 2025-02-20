package uk.gov.hmcts.darts.annotation.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationDataManagement {

    private final DataManagementApi dataManagementApi;
    private final DataManagementFacade dataManagementFacade;

    public Map<ExternalLocationTypeEnum, String> upload(BinaryData binaryData, String filename) {
        String inboundLocation = null;
        String unstructuredLocation;
        try {
            inboundLocation = dataManagementApi.saveBlobDataToInboundContainer(binaryData);
            unstructuredLocation = dataManagementApi.saveBlobDataToUnstructuredContainer(binaryData);
        } catch (RuntimeException e) {
            if (inboundLocation != null) {
                log.error("Failed to upload annotation document {} to unstructured container", filename, e);
                attemptToDeleteDocument(INBOUND, inboundLocation);
            } else {
                log.error("Failed to upload annotation document {} to inbound container", filename, e);
            }
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }

        return Map.of(
            INBOUND, inboundLocation,
            UNSTRUCTURED, unstructuredLocation);
    }

    @SuppressWarnings({"PMD.CloseResource"})
    public Resource download(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities) {
        try {
            DownloadResponseMetaData downloadResponseMetaData = dataManagementFacade.retrieveFileFromStorage(externalObjectDirectoryEntities);

            return downloadResponseMetaData.getResource();
        } catch (IOException | FileNotDownloadedException e) {
            log.error("Failed to download annotation document {}",
                      externalObjectDirectoryEntities.get(0).getAnnotationDocumentEntity().getId(), e);
            throw new DartsApiException(FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT, e);
        }
    }

    public void attemptToDeleteDocuments(Map<ExternalLocationTypeEnum, String> documentLocations) {
        documentLocations.forEach(this::attemptToDeleteDocument);
    }

    private void attemptToDeleteDocument(ExternalLocationTypeEnum type, String location) {
        try {
            switch (type) {
                case INBOUND:
                    dataManagementApi.deleteBlobDataFromInboundContainer(location);
                    break;
                case UNSTRUCTURED:
                    dataManagementApi.deleteBlobDataFromUnstructuredContainer(location);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + type);
            }
        } catch (AzureDeleteBlobException e) {
            log.error("Failed to delete orphaned annotation document {}", location, e);
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }
    }
}