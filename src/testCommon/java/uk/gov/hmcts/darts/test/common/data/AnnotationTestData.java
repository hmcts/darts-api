package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomAnnotationEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class AnnotationTestData  implements Persistable<CustomAnnotationEntity.CustomAnnotationEntityRetrieve> {

    AnnotationTestData() {
    }

    public AnnotationEntity minimalAnnotationEntity() {
        return someMinimal().getBuilder().build().getEntity();
    }

    @Override
    public CustomAnnotationEntity.CustomAnnotationEntityRetrieve someMinimal() {
        CustomAnnotationEntity.CustomAnnotationEntityRetrieve retrieve = new
            CustomAnnotationEntity.CustomAnnotationEntityRetrieve();
        UserAccountEntity userAccount = minimalUserAccount();
        retrieve.getBuilder().currentOwner(userAccount).lastModifiedBy(userAccount)
            .createdBy(userAccount).createdTimestamp(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now())
            .hearingList(new ArrayList<>());
        return retrieve;
    }

    @Override
    public CustomAnnotationEntity.CustomAnnotationEntityRetrieve someMaximal() {
        return someMinimal();
    }
}