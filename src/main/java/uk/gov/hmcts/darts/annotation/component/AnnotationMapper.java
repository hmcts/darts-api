package uk.gov.hmcts.darts.annotation.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

@Component
@RequiredArgsConstructor
public class AnnotationMapper {

    private final AuthorisationApi authorisationApi;
    private final CurrentTimeHelper currentTimeHelper;

    public AnnotationEntity mapFrom(Annotation annotation) {
        var annotationEntity = new AnnotationEntity();
        annotationEntity.setText(annotation.getComment());
        annotationEntity.setDeleted(false);
        annotationEntity.setTimestamp(currentTimeHelper.currentOffsetDateTime());
        annotationEntity.setCurrentOwner(authorisationApi.getCurrentUser());
        annotationEntity.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime());
        annotationEntity.setLastModifiedBy(authorisationApi.getCurrentUser());

        return annotationEntity;
    }
}
