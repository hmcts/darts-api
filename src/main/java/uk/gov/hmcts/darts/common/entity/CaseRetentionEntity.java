package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.CascadeType;
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
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

import java.time.OffsetDateTime;

@Entity
@Table(name = CaseRetentionEntity.TABLE_NAME)
@Getter
@Setter
public class CaseRetentionEntity extends CreatedModifiedBaseEntity
    implements HasIntegerId {


    public static final String ID = "car_id";
    public static final String TABLE_NAME = "case_retention";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_gen")
    @SequenceGenerator(name = "car_gen", sequenceName = "car_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinColumn(name = "rpt_id", nullable = false)
    private RetentionPolicyTypeEntity retentionPolicyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmr_id")
    private CaseManagementRetentionEntity caseManagementRetention;

    @Column(name = "total_sentence")
    private String totalSentence;

    @Column(name = "retain_until_ts", nullable = false)
    private OffsetDateTime retainUntil;

    @Column(name = "retain_until_applied_on_ts", nullable = false)
    private OffsetDateTime retainUntilAppliedOn;

    @Column(name = "current_state", nullable = false)
    private String currentState;

    @Column(name = "comments")
    private String comments;

    @Column(name = "confidence_category")
    private RetentionConfidenceCategoryEnum confidenceCategory;

    @Column(name = "retention_object_id")
    private String retentionObjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private UserAccountEntity submittedBy;
}