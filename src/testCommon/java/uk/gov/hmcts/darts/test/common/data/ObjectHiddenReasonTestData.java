package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ObjectHiddenReasonTestData {

    public static ObjectHiddenReasonEntity publicInterestImmunity() {
        var reason = new ObjectHiddenReasonEntity();
        reason.setId(1);
        reason.setDisplayName("Public interest immunity");
        reason.setDisplayOrder(1);
        reason.setMarkedForDeletion(true);
        return reason;
    }

    public static ObjectHiddenReasonEntity classified() {
        var reason = new ObjectHiddenReasonEntity();
        reason.setId(2);
        reason.setDisplayName("Classified above official");
        reason.setDisplayOrder(1);
        reason.setMarkedForDeletion(true);
        return reason;
    }

    public static ObjectHiddenReasonEntity otherDelete() {
        var reason = new ObjectHiddenReasonEntity();
        reason.setId(3);
        reason.setDisplayName("Other interest immunity");
        reason.setDisplayOrder(1);
        reason.setMarkedForDeletion(true);
        return reason;
    }

    public static ObjectHiddenReasonEntity otherHide() {
        var reason = new ObjectHiddenReasonEntity();
        reason.setId(4);
        reason.setDisplayName("Other hide");
        reason.setDisplayOrder(1);
        reason.setMarkedForDeletion(true);
        return reason;
    }


}
