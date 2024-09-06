package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class AnnotationTestData {

    public static AnnotationEntity minimalAnnotationEntity() {
        var annotation = new AnnotationEntity();
        UserAccountEntity userAccount = minimalUserAccount();
        annotation.setCurrentOwner(userAccount);
        annotation.setLastModifiedBy(userAccount);
        annotation.setCreatedBy(userAccount);
        return annotation;
    }
}
