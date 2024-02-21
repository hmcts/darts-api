package uk.gov.hmcts.darts.annotation.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.component.AnnotationDocumentBuilder;
import uk.gov.hmcts.darts.annotation.component.AnnotationMapper;
import uk.gov.hmcts.darts.annotation.component.ExternalObjectDirectoryBuilder;
import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDto;
import uk.gov.hmcts.darts.annotation.persistence.AnnotationPersistenceService;
import uk.gov.hmcts.darts.annotation.service.AnnotationService;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.FAILED_TO_UPLOAD_ANNOTATION_DOCUMENT;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID;

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
    private final ExternalObjectDirectoryRepository eodRepository;
    private final Validator<Annotation> annotationUploadValidator;
    private final Validator<Integer> userAuthorisedToDeleteAnnotationValidator;
    private final Validator<Integer> userAuthorisedToDownloadAnnotationValidator;
    private final Validator<Integer> annotationExistsValidator;

    @Override
    public Integer process(MultipartFile document, Annotation annotation) {
        annotationUploadValidator.validate(annotation);

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

    @Override
    public AnnotationResponseDto downloadAnnotationDoc(Integer annotationId, Integer annotationDocumentId) {

        userAuthorisedToDownloadAnnotationValidator.validate(annotationId);

        final Optional<ExternalObjectDirectoryEntity> eodDir = eodRepository.findByAnnotationIdAndAnnotationDocumentId(annotationId, annotationDocumentId);
        final InputStreamResource blobStream;

        final ExternalObjectDirectoryEntity externalObjectDirectoryEntity;

        if (eodDir.isEmpty()) {
                throw new DartsApiException(INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID);
        }

        externalObjectDirectoryEntity = eodDir.get();

        try {

            blobStream = new InputStreamResource(
                dataManagementApi.getBlobDataFromInboundContainer(externalObjectDirectoryEntity.getExternalLocation()).toStream());

        } catch (RuntimeException e) {
            log.error("Failed to download annotation document {}", externalObjectDirectoryEntity.getId(), e);
            throw new DartsApiException(FAILED_TO_DOWNLOAD_ANNOTATION_DOCUMENT, e);
        }

        return AnnotationResponseDto.builder()
                .resource(blobStream)
                .fileName(externalObjectDirectoryEntity.getAnnotationDocumentEntity().getFileName())
                .externalLocation(externalObjectDirectoryEntity.getExternalLocation())
                .annotationDocumentId(annotationDocumentId).build();

    }


    @Override
    public void delete(Integer annotationId) {
        annotationExistsValidator.validate(annotationId);
        userAuthorisedToDeleteAnnotationValidator.validate(annotationId);
        annotationPersistenceService.markForDeletion(annotationId);
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
