package uk.gov.hmcts.darts.annotation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.annotation.controller.dto.AnnotationResponseDto;
import uk.gov.hmcts.darts.annotation.service.AnnotationDownloadService;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.List;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationDownloadServiceImpl implements AnnotationDownloadService {

    private final AnnotationDataManagement annotationDataManagement;
    private final ExternalObjectDirectoryRepository eodRepository;
    private final Validator<Integer> userAuthorisedToDownloadAnnotationValidator;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;


    @Override
    public AnnotationResponseDto downloadAnnotationDoc(Integer annotationId, Integer annotationDocumentId) {

        userAuthorisedToDownloadAnnotationValidator.validate(annotationId);

        final ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(
            ObjectRecordStatusEnum.STORED.getId());

        final List<ExternalObjectDirectoryEntity> eodDir = eodRepository.findByAnnotationIdAndAnnotationDocumentId(
            annotationId, annotationDocumentId, storedStatus);

        if (eodDir.isEmpty()) {
            throw new DartsApiException(INVALID_ANNOTATIONID_OR_ANNOTATION_DOCUMENTID);
        }

        final ExternalObjectDirectoryEntity externalObjectDirectoryEntity = eodDir.get(0);

        final InputStreamResource blobStream = annotationDataManagement.download(externalObjectDirectoryEntity);

        return AnnotationResponseDto.builder()
            .resource(blobStream)
            .fileName(externalObjectDirectoryEntity.getAnnotationDocumentEntity().getFileName())
            .externalLocation(externalObjectDirectoryEntity.getExternalLocation())
            .annotationDocumentId(annotationDocumentId).build();
    }
}
