package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.time.OffsetDateTime;

@Entity
@Table(name = "case_retention_extra")
@Getter
@Setter
public class CaseRetentionExtraEntity {

    @Id
    @Column(name = "cas_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @Column(name = "current_rah_id")
    private Integer currentRahId;

    @Column(name = "current_rah_rpt_id")
    private Integer currentRahRptId;

    @Column(name = "current_rpr_id")
    private Integer currentRprId;

    @Column(name = "current_rpr_rpt_id")
    private Integer currentRprRptId;

    @Column(name = "retention_fixed_rpt_id")
    private Integer retentionFixedRptId;

    @Column(name = "case_total_sentence")
    private String caseTotalSentence;

    @Column(name = "case_retention_fixed")
    private String caseRetentionFixed;

    @Column(name = "end_of_sentence_date_ts")
    private OffsetDateTime endOfSentenceAt;

    @Column(name = "manual_retention_override")
    private Integer manualRetentionOverride;

    @Column(name = "actual_case_closed_flag")
    private Integer actualCaseClosedFlag;

    @Column(name = "actual_case_closed_ts")
    private OffsetDateTime actualCaseClosedAt;

    @Column(name = "actual_retain_until_ts")
    private OffsetDateTime actualRetainUntilAt;

    @Column(name = "actual_case_created_ts")
    private OffsetDateTime actualCaseCreatedAt;

    @Column(name = "submitted_by")
    private Integer submittedBy;

    @Column(name = "rps_retainer_object_id")
    private String rpsRetainerObjectId;

    @Column(name = "case_closed_eve_id")
    private Integer caseClosedEveId;

    @Column(name = "case_closed_event_ts")
    private OffsetDateTime caseClosedEventAt;

    @Column(name = "max_event_ts")
    private OffsetDateTime maxEventAt;

    @Column(name = "max_media_ts")
    private OffsetDateTime maxMediaAt;

    @Column(name = "closure_method_type")
    private String closureMethodType;

    @Column(name = "best_case_closed_ts")
    private OffsetDateTime bestCaseClosedAt;

    @Column(name = "best_case_closed_type")
    private String bestCaseClosedType;

    @Column(name = "best_retainer_retain_until_ts")
    private OffsetDateTime bestRetainerRetainUntilAt;

    @Column(name = "best_audit_retain_until_ts")
    private OffsetDateTime bestAuditRetainUntilAt;

    @Column(name = "retention_aged_policy_name")
    private String retentionAgedPolicyName;

    @Column(name = "case_closed_diff_in_days")
    private Integer caseClosedDiffInDays;

    @SuppressWarnings("checkstyle:MemberName")
    @Column(name = "r_retain_until_diff_in_days")
    private Integer rRetainUntilDiffInDays;

    @SuppressWarnings("checkstyle:MemberName")
    @Column(name = "a_retain_until_diff_in_days")
    private Integer aRetainUntilDiffInDays;

    @Column(name = "validation_error_1")
    private String validationError1;

    @Column(name = "validation_error_2")
    private String validationError2;

    @Column(name = "validation_error_3")
    private String validationError3;

    @Column(name = "validation_error_4")
    private String validationError4;

    @Column(name = "validation_error_5")
    private String validationError5;

    @Column(name = "ret_conf_score")
    private RetentionConfidenceScoreEnum retConfScore;

    @Column(name = "ret_conf_reason")
    @Enumerated(EnumType.STRING)
    private RetentionConfidenceReasonEnum retConfReason;

    @Column(name = "ret_conf_updated_ts")
    private OffsetDateTime retConfUpdatedAt;

    @Column(name = "validated_ts")
    private OffsetDateTime validatedAt;

    @CreationTimestamp
    @Column(name = "created_ts", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified_ts", nullable = false)
    private OffsetDateTime lastModifiedAt;

    @Column(name = "migrated_ts")
    private OffsetDateTime migratedAt;

}