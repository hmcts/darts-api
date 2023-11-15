package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = CaseRetentionEntity.TABLE_NAME)
@Getter
@Setter
public class CaseRetentionEntity extends CreatedBaseEntity {


    public static final String ID = "car_id";
    public static final String TABLE_NAME = "case_retention";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_gen")
    @SequenceGenerator(name = "car_gen", sequenceName = "car_seq", allocationSize = 1)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rpt_id", nullable = false)
    private RetentionPolicyTypeEntity retentionPolicyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmr_id")
    private CaseManagementRetentionEntity caseManagementRetention;

    @Column(name = "total_sentence", nullable = false)
    private String totalSentence;

    @Column(name = "retain_until_ts", nullable = false)
    private OffsetDateTime retainUntil;

    @Column(name = "retain_until_applied_on_ts", nullable = false)
    private OffsetDateTime retainUntilAppliedOn;

    @Column(name = "current_state", nullable = false)
    private String currentState;

    @Column(name = "comments")
    private String comments;

    @Column(name = "retention_object_id")
    private String retentionObjectId;

    @Column(name = "submitted_ts", nullable = false)
    private OffsetDateTime submitted;

}
