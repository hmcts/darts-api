package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestObjectAdminActionEntity;

import static java.time.OffsetDateTime.now;
import static uk.gov.hmcts.darts.test.common.data.ObjectHiddenReasonTestData.publicInterestImmunity;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class ObjectAdminActionTestData
    implements Persistable<TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilderRetrieve, ObjectAdminActionEntity,
    TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilder> {

    @Override
    public ObjectAdminActionEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilderRetrieve someMinimalBuilderHolder() {
        var builderRetrieve = new TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilderRetrieve();

        builderRetrieve.getBuilder()
            .media(PersistableFactory.getMediaTestData().someMinimal())
            .markedForManualDeletion(false);

        return builderRetrieve;
    }

    @Override
    public TestObjectAdminActionEntity.TestObjectAdminActionEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Deprecated
    public static ObjectAdminActionEntity minimalObjectAdminAction() {
        var action = new ObjectAdminActionEntity();
        action.setObjectHiddenReason(publicInterestImmunity());
        return action;
    }

    @Deprecated
    public static ObjectAdminActionEntity objectAdminActionWithDefaults() {
        var action = minimalObjectAdminAction();
        action.setComments("some comment");
        action.setTicketReference("Ticket-123");
        action.setHiddenBy(minimalUserAccount());
        action.setHiddenDateTime(now());
        action.setMarkedForManualDelDateTime(now());
        action.setMarkedForManualDelBy(minimalUserAccount());

        return action;
    }

}