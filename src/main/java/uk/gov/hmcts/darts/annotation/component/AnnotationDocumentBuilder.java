package uk.gov.hmcts.darts.annotation.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;

@Component
@RequiredArgsConstructor
public class AnnotationDocumentBuilder {

    private final UserIdentity userIdentity;

    public AnnotationDocumentEntity buildFrom(MultipartFile document, AnnotationEntity annotationEntity, String checksum) {
        final var annotationDocumentEntity = new AnnotationDocumentEntity();
        annotationDocumentEntity.setAnnotation(annotationEntity);
        annotationDocumentEntity.setFileName(document.getOriginalFilename());
        annotationDocumentEntity.setFileType(document.getContentType());
        annotationDocumentEntity.setFileSize((int) document.getSize());
        annotationDocumentEntity.setChecksum(checksum);
        annotationDocumentEntity.setUploadedBy(userIdentity.getUserAccount());

        return annotationDocumentEntity;
    }

}
