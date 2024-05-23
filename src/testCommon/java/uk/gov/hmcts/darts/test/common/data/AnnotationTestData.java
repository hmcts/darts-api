package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationEntity;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class AnnotationTestData {

    public static AnnotationEntity minimalAnnotationEntity() {
        var annotation = new AnnotationEntity();
        annotation.setCurrentOwner(minimalUserAccount());
        return annotation;
    }
}
