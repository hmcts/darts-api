package uk.gov.hmcts.darts.annotation.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.component.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.component.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.component.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.util.UUID;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationServiceImpl implements AnnotationService {

    private final AnnotationMapper annotationMapper;
    private final AnnotationDocumentBuilder annotationDocumentBuilder;
    private final ExternalObjectDirectoryBuilder externalObjectDirectoryBuilder;
    private final DataManagementApi dataManagementApi;
    private final FileContentChecksum fileContentChecksum;
    private final AnnotationPersistenceService annotationPersistenceService;
    private final Validator<Annotation> annotationValidator;

    @Override
    public Integer process(MultipartFile document, Annotation annotation) {
        annotationValidator.validate(annotation);

        UUID externalLocation;
        BinaryData binaryData;

        try {
            binaryData = BinaryData.fromStream(document.getInputStream());
            externalLocation = dataManagementApi.saveBlobDataToInboundContainer(binaryData);
        } catch (RuntimeException | IOException e) {
            log.error("Failed to upload annotation document {}", document.getOriginalFilename(), e);
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }

        var annotationEntity = annotationMapper.mapFrom(annotation);
        var annotationDocumentEntity = annotationDocumentBuilder.buildFrom(
            document,
            annotationEntity,
            fileContentChecksum.calculate(binaryData.toBytes()));
        var externalObjectDirectoryEntity = externalObjectDirectoryBuilder.buildFrom(annotationDocumentEntity, externalLocation);

        try {
            annotationPersistenceService.persistAnnotation(externalObjectDirectoryEntity, annotation.getHearingId());
        } catch (RuntimeException exception) {
            attemptToDeleteDocument(externalLocation);
        }

        return annotationEntity.getId();
    }

    private void attemptToDeleteDocument(UUID externalLocation) {
        try {
            dataManagementApi.deleteBlobDataFromInboundContainer(externalLocation);
        } catch (AzureDeleteBlobException e) {
            log.error("Failed to delete orphaned annotation document {}", externalLocation, e);
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }
    }
}
