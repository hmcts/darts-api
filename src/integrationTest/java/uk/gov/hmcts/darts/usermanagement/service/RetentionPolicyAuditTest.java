package uk.gov.hmcts.darts.usermanagement.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.retention.service.RetentionPolicyTypeService;
import uk.gov.hmcts.darts.retentions.model.AdminPatchRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.RetentionPolicyTestData.minimalRetentionPolicy;

class RetentionPolicyAuditTest extends IntegrationBase {

    @Autowired
    private RetentionPolicyTypeService retentionPolicyTypeService;

    @Autowired
    private GivenBuilder given;

    @Test
    void auditsWhenRetentionPolicyTypesAreCreated() {
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var retentionPolicy = retentionPolicyTypeService.createOrReviseRetentionPolicyType(
            adminPostRetentionRequestWithDefaults(),
            false
        );
        transactionalUtil.executeInTransaction(() -> {
            var createRetentionPolicyActivity = findAuditActivity("Create Retention Policy", dartsDatabase.findAudits());
            assertThat(createRetentionPolicyActivity.getUser().getId()).isEqualTo(userAccountEntity.getId());

            var retentionPolicyRevisions = dartsDatabase.findRetentionPolicyRevisionsFor(retentionPolicy.getId());
            assertThat(retentionPolicyRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);
        });
    }

    @Test
    void auditsWhenRetentionPolicyTypesAreCreatedWithRevision() {
        // Given
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var priorPolicyEntity = minimalRetentionPolicy();
        priorPolicyEntity.setPolicyStart(OffsetDateTime.now().minusWeeks(1));
        dartsDatabase.save(priorPolicyEntity);

        // When
        var adminPostRetentionRevision = adminPostRetentionRequestWithDefaults();
        var retentionPolicy = retentionPolicyTypeService.createOrReviseRetentionPolicyType(
            adminPostRetentionRevision,
            true
        );

        transactionalUtil.executeInTransaction(() -> {
            var audits = dartsDatabase.findAudits();
            assertThat(audits).extracting("auditActivity.name")
                .containsExactlyInAnyOrder("Create Retention Policy", "Revise Retention Policy");
            assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId(), userAccountEntity.getId());

            var retentionPolicyRevisions = dartsDatabase.findRetentionPolicyRevisionsFor(retentionPolicy.getId());
            assertThat(retentionPolicyRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(INSERT);

            var priorPolicyRevisions = dartsDatabase.findRetentionPolicyRevisionsFor(priorPolicyEntity.getId());
            assertThat(priorPolicyRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    @Test
    void auditsWhenRetentionPolicyTypesArePatched() {
        // Given
        var userAccountEntity = given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var retentionPolicyType = minimalRetentionPolicy();
        retentionPolicyType.setPolicyStart(OffsetDateTime.now().plusWeeks(1));
        dartsDatabase.save(retentionPolicyType);

        // When
        var adminPatchRetentionPolicy = new AdminPatchRetentionRequest().name("some-new-patched-name");
        retentionPolicyTypeService.editRetentionPolicyType(
            retentionPolicyType.getId(),
            adminPatchRetentionPolicy);

        transactionalUtil.executeInTransaction(() -> {
            var audits = dartsDatabase.findAudits();
            assertThat(audits).extracting("auditActivity.name").containsExactly("Edit Retention Policy");
            assertThat(audits).extracting("user.id").containsExactly(userAccountEntity.getId());

            var retentionPolicyRevisions = dartsDatabase.findRetentionPolicyRevisionsFor(retentionPolicyType.getId());
            assertThat(retentionPolicyRevisions.getLatestRevision().getMetadata().getRevisionType()).isEqualTo(UPDATE);
        });
    }

    private static AdminPostRetentionRequest adminPostRetentionRequestWithDefaults() {
        return new AdminPostRetentionRequest()
            .name("some-retention-policy-name")
            .fixedPolicyKey("some-fixed-policy-key")
            .description("some-description")
            .displayName("some-display-name")
            .policyStartAt(OffsetDateTime.now().plusWeeks(1))
            .duration("1Y2M3D");
    }

    private AuditEntity findAuditActivity(String activity, List<AuditEntity> audits) {
        return audits.stream()
            .filter(audit -> activity.equals(audit.getAuditActivity().getName()))
            .findFirst().orElseThrow();
    }

}