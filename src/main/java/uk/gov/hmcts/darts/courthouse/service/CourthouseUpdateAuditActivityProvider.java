package uk.gov.hmcts.darts.courthouse.service;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditActivityProvider;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_COURTHOUSE;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.UPDATE_COURTHOUSE_GROUP;

public final class CourthouseUpdateAuditActivityProvider implements AuditActivityProvider {


    private final Set<AuditActivity> updates = EnumSet.noneOf(AuditActivity.class);

    public static CourthouseUpdateAuditActivityProvider auditActivitiesFor(CourthousePatch patch, CourthouseEntity prePatched) {
        return new CourthouseUpdateAuditActivityProvider(prePatched, patch);
    }

    private CourthouseUpdateAuditActivityProvider(CourthouseEntity courthouseEntity, CourthousePatch courthousePatch) {
        if (isBasicDetailsUpdated(courthousePatch, courthouseEntity)) {
            updates.add(UPDATE_COURTHOUSE);
        }
        if (securityGroupsUpdated(courthousePatch, courthouseEntity)) {
            updates.add(UPDATE_COURTHOUSE_GROUP);
        }
    }

    @Override
    public Set<AuditActivity> getAuditActivities() {
        return updates;
    }

    private boolean isBasicDetailsUpdated(CourthousePatch patch, CourthouseEntity prePatched) {
        boolean isNameUpdated = notNullAndDifferent(patch.getCourthouseName(), prePatched.getCourthouseName());
        boolean isDisplayNameUpdated = notNullAndDifferent(patch.getDisplayName(), prePatched.getDisplayName());
        boolean isRegionUpdated = isRegionUpdated(patch, prePatched);

        return isNameUpdated || isDisplayNameUpdated || isRegionUpdated;
    }

    private static boolean isRegionUpdated(CourthousePatch patch, CourthouseEntity prePatched) {
        return nonNull(patch.getRegionId())
            && (prePatched.getRegion() == null || !patch.getRegionId().equals(prePatched.getRegion().getId()));
    }

    private static boolean securityGroupsUpdated(CourthousePatch courthousePatch, CourthouseEntity prePatchedEntity) {
        if (courthousePatch.getSecurityGroupIds() == null) {
            return false;
        }
        var prePatchedGroups = prePatchedEntity.getSecurityGroups().stream()
            .map(SecurityGroupEntity::getId)
            .toList();
        return !prePatchedGroups.equals(courthousePatch.getSecurityGroupIds());
    }
}
