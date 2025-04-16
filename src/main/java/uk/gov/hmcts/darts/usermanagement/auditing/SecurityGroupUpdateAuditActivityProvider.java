package uk.gov.hmcts.darts.usermanagement.auditing;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditActivityProvider;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupPatch;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_COURTHOUSE_GROUP;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_GROUP;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_USERS_GROUP;

public final class SecurityGroupUpdateAuditActivityProvider implements AuditActivityProvider {

    private final Set<AuditActivity> auditActivities = EnumSet.noneOf(AuditActivity.class);

    public static SecurityGroupUpdateAuditActivityProvider auditActivitiesFor(SecurityGroupEntity entity, SecurityGroupPatch patch) {
        return new SecurityGroupUpdateAuditActivityProvider(entity, patch);
    }

    @Override
    public Set<AuditActivity> getAuditActivities() {
        return auditActivities;
    }

    private SecurityGroupUpdateAuditActivityProvider(SecurityGroupEntity entity, SecurityGroupPatch patch) {
        if (basicDetailsAreUpdated(entity, patch)) {
            auditActivities.add(UPDATE_GROUP);
        }
        if (userInGroupAreUpdated(entity, patch)) {
            auditActivities.add(UPDATE_USERS_GROUP);
        }
        if (courthousesInGroupAreUpdated(entity, patch)) {
            auditActivities.add(UPDATE_COURTHOUSE_GROUP);
        }
    }

    private boolean courthousesInGroupAreUpdated(SecurityGroupEntity entity, SecurityGroupPatch patch) {
        Set<Integer> patchValues = patch.getCourthouseIds() == null ? new HashSet<>() : new HashSet<>(patch.getCourthouseIds());
        var prePatchValues = entity.getCourthouseEntities().stream().map(CourthouseEntity::getId).collect(toSet());

        return notNullAndDifferent(prePatchValues, patchValues);
    }

    private boolean userInGroupAreUpdated(SecurityGroupEntity prePatched, SecurityGroupPatch patch) {
        Set<Integer> patchValues = patch.getUserIds() == null ? new HashSet<>() : new HashSet<>(patch.getUserIds());
        var prePatchValues = Optional.ofNullable(prePatched.getUsers())
            .map(users -> users.stream()
                .map(UserAccountEntity::getId)
                .collect(toSet()))
            .orElse(Collections.emptySet());

        return notNullAndDifferent(prePatchValues, patchValues);
    }

    private boolean basicDetailsAreUpdated(SecurityGroupEntity prePatched, SecurityGroupPatch patch) {
        boolean isNameUpdated = notNullAndDifferent(patch.getName(), prePatched.getGroupName());
        boolean isDisplayNameUpdated = notNullAndDifferent(patch.getDisplayName(), prePatched.getDisplayName());
        boolean isDescriptionUpdated = notNullAndDifferent(patch.getDescription(), prePatched.getDescription());

        return isNameUpdated || isDisplayNameUpdated || isDescriptionUpdated;
    }
}
