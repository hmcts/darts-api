package uk.gov.hmcts.darts.annotation.builders;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
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
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        annotationEntity.setCurrentOwner(currentUser);
        annotationEntity.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime());
        annotationEntity.setLastModifiedBy(currentUser);
        annotationEntity.setCreatedBy(currentUser);

        return annotationEntity;
    }
}
