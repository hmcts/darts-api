package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = "rps_retainer")
@Getter
@Setter
public class RpsRetainerEntity extends CreatedModifiedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rpr_gen")
    @SequenceGenerator(name = "rpr_gen", sequenceName = "rpr_seq", allocationSize = 1)
    @Column(name = "rpr_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rpt_id", nullable = false)
    private RetentionPolicyTypeEntity retentionPolicyType;

    @Column(name = "rps_retainer_object_id", nullable = false)
    private String retainerObjectId;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "dm_retainer_root_id")
    private String dmRetainerRootId;

    @Column(name = "dm_retention_rule_type")
    private Integer dmRetentionRuleType;

    @Column(name = "dm_retention_date")
    private OffsetDateTime dmRetentionAt;

    @Column(name = "dmc_current_phase_id")
    private String dmcCurrentPhaseId;

    @Column(name = "dmc_entry_date")
    private OffsetDateTime dmcEntryAt;

    @Column(name = "dmc_parent_ancestor_id")
    private String dmcParentAncestorId;

    @Column(name = "dmc_phase_name")
    private String dmcPhaseName;

    @Column(name = "dmc_qualification_date")
    private OffsetDateTime dmcQualificationAt;

    @Column(name = "dmc_retention_base_date")
    private OffsetDateTime dmcRetentionBaseAt;

    @Column(name = "dmc_retention_policy_id")
    private String dmcRetentionPolicyId;

    @Column(name = "dmc_ultimate_ancestor_id")
    private String dmcUltimateAncestorId;

    @Column(name = "dmc_vdm_retention_rule")
    private Integer dmcVdmRetentionRule;

    @Column(name = "dmc_is_superseded")
    private Integer dmcIsSuperseded;

    @Column(name = "dmc_superseded_date")
    private OffsetDateTime dmcSupersededAt;

    @Column(name = "dmc_superseded_phase_id")
    private String dmcSupersededPhaseId;

    @Column(name = "dmc_snapshot_retention_rule")
    private Integer dmcSnapshotRetentionRule;

    @Column(name = "dmc_approval_required")
    private Integer dmcApprovalRequired;

    @Column(name = "dmc_approval_status")
    private String dmcApprovalStatus;

    @Column(name = "dmc_approved_date")
    private OffsetDateTime dmcApprovedAt;

    @Column(name = "dmc_projected_disposition_date")
    private OffsetDateTime dmcProjectedDispositionAt;

    @Column(name = "dmc_is_qualification_suspended")
    private Integer dmcIsQualificationSuspended;

    @Column(name = "dmc_suspension_lift_date")
    private OffsetDateTime dmcSuspensionLiftAt;

    @Column(name = "dmc_base_date_override")
    private OffsetDateTime dmcBaseDateOverrideAt;

    @Column(name = "dms_object_name")
    private String dmsObjectName;

    @Column(name = "dms_i_chronicle_id")
    private String dmsIChronicleId;

    @Column(name = "dms_r_policy_id")
    private String dmsRPolicyId;

    @Column(name = "dms_r_resume_state")
    private Integer dmsRResumeState;

    @Column(name = "dms_r_current_state")
    private Integer dmsRCurrentState;

}