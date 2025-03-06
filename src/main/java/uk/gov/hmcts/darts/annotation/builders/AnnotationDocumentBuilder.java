package uk.gov.hmcts.darts.annotation.builders;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;

@Component
@RequiredArgsConstructor
public class AnnotationDocumentBuilder {

    private final UserIdentity userIdentity;

    public AnnotationDocumentEntity buildFrom(MultipartFile document, String checksum) {
        final var annotationDocumentEntity = new AnnotationDocumentEntity();
        annotationDocumentEntity.setFileName(document.getOriginalFilename());
        annotationDocumentEntity.setFileType(document.getContentType());
        annotationDocumentEntity.setFileSize((int) document.getSize());
        annotationDocumentEntity.setChecksum(checksum);
        annotationDocumentEntity.setUploadedBy(userIdentity.getUserAccount());
        annotationDocumentEntity.setLastModifiedBy(userIdentity.getUserAccount());

        return annotationDocumentEntity;
    }

}
