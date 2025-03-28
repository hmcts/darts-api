package uk.gov.hmcts.darts.usermanagement.auditing;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditActivityProvider;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DEACTIVATE_USER;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.REACTIVATE_USER;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_USER;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_USERS_GROUP;

public class UserAccountUpdateAuditActivityProvider implements AuditActivityProvider {
    private final Set<AuditActivity> auditActivities = new HashSet<>();

    public static UserAccountUpdateAuditActivityProvider auditActivitiesFor(UserAccountEntity entity, UserPatch patch) {
        return new UserAccountUpdateAuditActivityProvider(entity, patch);
    }

    @Override
    public Set<AuditActivity> getAuditActivities() {
        return auditActivities;
    }

    private UserAccountUpdateAuditActivityProvider(UserAccountEntity entity, UserPatch patch) {
        if (isUserDeactivated(entity, patch)) {
            auditActivities.add(DEACTIVATE_USER);
        }
        if (isUserReactivated(entity, patch)) {
            auditActivities.add(REACTIVATE_USER);
        }
        if (basicDetailsAreUpdated(entity, patch)) {
            auditActivities.add(UPDATE_USER);
        }
        if (userInGroupAreUpdated(entity, patch)) {
            auditActivities.add(UPDATE_USERS_GROUP);
        }
    }

    private boolean userInGroupAreUpdated(UserAccountEntity prePatched, UserPatch patch) {
        Set<Integer> patchValues = patch.getSecurityGroupIds() == null ? new HashSet<>() : new HashSet<>(patch.getSecurityGroupIds());
        var prePatchValues = prePatched.getSecurityGroupEntities().stream().map(SecurityGroupEntity::getId).collect(toSet());
        return notNullAndDifferent(
            prePatchValues,
            patchValues);
    }

    private boolean basicDetailsAreUpdated(UserAccountEntity prePatched, UserPatch patch) {
        boolean isNameUpdated = notNullAndDifferent(patch.getFullName(), prePatched.getUserFullName());
        boolean isDisplayNameUpdated = notNullAndDifferent(patch.getDescription(), prePatched.getUserDescription());
        boolean isEmailAddressUpdated = notNullAndDifferent(patch.getEmailAddress(), prePatched.getEmailAddress());

        return isNameUpdated || isDisplayNameUpdated || isEmailAddressUpdated;
    }

    private boolean isUserReactivated(UserAccountEntity prePatched, UserPatch patch) {
        return !prePatched.isActive() && notNullAndDifferent(patch.getActive(), prePatched.isActive());
    }

    private boolean isUserDeactivated(UserAccountEntity prePatched, UserPatch patch) {
        return prePatched.isActive() && notNullAndDifferent(patch.getActive(), prePatched.isActive());
    }

}
