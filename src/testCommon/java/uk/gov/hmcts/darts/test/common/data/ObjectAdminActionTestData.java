package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import static java.time.OffsetDateTime.now;
import static uk.gov.hmcts.darts.test.common.data.ObjectHiddenReasonTestData.publicInterestImmunity;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public class ObjectAdminActionTestData {

    private ObjectAdminActionTestData() {

    }

    public static ObjectAdminActionEntity minimalObjectAdminAction() {
        var action = new ObjectAdminActionEntity();
        action.setObjectHiddenReason(publicInterestImmunity());
        return action;
    }

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