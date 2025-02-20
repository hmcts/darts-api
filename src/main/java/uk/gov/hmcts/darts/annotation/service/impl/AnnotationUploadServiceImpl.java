package uk.gov.hmcts.darts.annotation.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.builders.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.builders.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.builders.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationUploadService;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;

import java.io.IOException;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationUploadServiceImpl implements AnnotationUploadService {

    private final AnnotationMapper annotationMapper;
    private final AnnotationDocumentBuilder annotationDocumentBuilder;
    private final ExternalObjectDirectoryBuilder externalObjectDirectoryBuilder;
    private final FileContentChecksum fileContentChecksum;
    private final AnnotationPersistenceService annotationPersistenceService;
    private final Validator<Annotation> hearingExistsValidator;
    private final Validator<MultipartFile> fileTypeValidator;
    private final AnnotationDataManagement annotationDataManagement;

    @Override
    public Integer upload(MultipartFile multipartFile, Annotation annotation) {
        hearingExistsValidator.validate(annotation);
        fileTypeValidator.validate(multipartFile);

        String checksum;
        try (var inputStream = multipartFile.getInputStream()) {
            checksum = fileContentChecksum.calculate(inputStream);
        } catch (IOException e) {
            log.error("Failed to calculate checksum for annotation document {}", multipartFile.getOriginalFilename(), e);
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }

        var binaryData = extractBinaryFrom(multipartFile);
        var containerLocations = annotationDataManagement.upload(binaryData, multipartFile.getOriginalFilename());

        var annotationEntity = annotationMapper.mapFrom(annotation);

        var annotationDocumentEntity = annotationDocumentBuilder.buildFrom(multipartFile, checksum);

        var inboundExternalObjectDirectory = externalObjectDirectoryBuilder.buildFrom(
            annotationDocumentEntity, containerLocations.get(INBOUND), INBOUND);
        var unstructuredExternalObjectDirectory = externalObjectDirectoryBuilder.buildFrom(
            annotationDocumentEntity, containerLocations.get(UNSTRUCTURED), UNSTRUCTURED);

        try {
            annotationPersistenceService.persistAnnotation(
                inboundExternalObjectDirectory,
                unstructuredExternalObjectDirectory,
                annotation.getHearingId(),
                annotationEntity,
                annotationDocumentEntity);

        } catch (RuntimeException exception) {
            log.error("Unable to persist annotation ", exception);
            annotationDataManagement.attemptToDeleteDocuments(containerLocations);
        }

        return annotationEntity.getId();
    }

    private static BinaryData extractBinaryFrom(MultipartFile document) {
        BinaryData binaryData;
        try {
            binaryData = BinaryData.fromStream(document.getInputStream());
        } catch (IOException e) {
            log.error("Failed to upload annotation document {}", document.getOriginalFilename(), e);
            throw new DartsApiException(FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT, e);
        }
        return binaryData;
    }
}
