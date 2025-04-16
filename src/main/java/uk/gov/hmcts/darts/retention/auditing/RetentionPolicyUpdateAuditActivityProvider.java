package uk.gov.hmcts.darts.retention.auditing;

import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditActivityProvider;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.retentions.model.AdminPatchRetentionRequest;

import java.util.EnumSet;
import java.util.Set;

import static uk.gov.hmcts.darts.audit.api.AuditActivity.EDIT_RETENTION_POLICY;

public final class RetentionPolicyUpdateAuditActivityProvider implements AuditActivityProvider {


    private final Set<AuditActivity> updates = EnumSet.noneOf(AuditActivity.class);

    public static RetentionPolicyUpdateAuditActivityProvider auditActivitiesFor(RetentionPolicyTypeEntity entity, AdminPatchRetentionRequest patch) {
        return new RetentionPolicyUpdateAuditActivityProvider(entity, patch);
    }

    private RetentionPolicyUpdateAuditActivityProvider(RetentionPolicyTypeEntity courthouseEntity, AdminPatchRetentionRequest courthousePatch) {
        if (isBasicDetailsUpdated(courthousePatch, courthouseEntity)) {
            updates.add(EDIT_RETENTION_POLICY);
        }
    }

    @Override
    public Set<AuditActivity> getAuditActivities() {
        return updates;
    }

    private boolean isBasicDetailsUpdated(AdminPatchRetentionRequest patch, RetentionPolicyTypeEntity prePatched) {
        boolean isNameUpdated = notNullAndDifferent(patch.getName(), prePatched.getPolicyName());
        boolean isDisplayNameUpdated = notNullAndDifferent(patch.getDisplayName(), prePatched.getDisplayName());
        boolean descriptionIsUpdated = notNullAndDifferent(patch.getDescription(), prePatched.getDescription());
        boolean fixedPolicyKeyIsUpdated = notNullAndDifferent(patch.getFixedPolicyKey(), prePatched.getFixedPolicyKey());
        boolean durationIsUpdated = notNullAndDifferent(patch.getDuration(), prePatched.getDuration());

        return isNameUpdated
            || isDisplayNameUpdated
            || descriptionIsUpdated
            || fixedPolicyKeyIsUpdated
            || durationIsUpdated;
    }
}
