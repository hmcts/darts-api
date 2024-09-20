package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestAnnotationEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class AnnotationTestData  implements Persistable<TestAnnotationEntity.TestAnnotationEntityRetrieve,
    AnnotationEntity, TestAnnotationEntity.TestAnnotationEntityBuilder> {

    AnnotationTestData() {
    }

    public AnnotationEntity minimalAnnotationEntity() {
        return someMinimal();
    }

    @Override
    public AnnotationEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestAnnotationEntity.TestAnnotationEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Override
    public TestAnnotationEntity.TestAnnotationEntityRetrieve someMinimalBuilderHolder() {
        TestAnnotationEntity.TestAnnotationEntityRetrieve retrieve = new
            TestAnnotationEntity.TestAnnotationEntityRetrieve();
        UserAccountEntity userAccount = minimalUserAccount();
        retrieve.getBuilder().currentOwner(userAccount).lastModifiedBy(userAccount)
            .createdBy(userAccount).createdTimestamp(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now())
            .hearingList(new ArrayList<>());
        return retrieve;
    }
}