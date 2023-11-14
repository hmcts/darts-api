package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = CaseRetentionEntity.TABLE_NAME)
@SuppressWarnings({"PMD.ShortClassName"})
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

    @ManyToOne
    @JoinColumn(name = "rpt_id")
    private RetentionPolicyTypeEntity retentionPolicyType;

    @ManyToOne
    @JoinColumn(name = "cmr_id")
    private CaseManagementRetentionEntity caseManagementRetention;

    @Column(name = "total_sentence")
    private String totalSentence;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntil;

    @Column(name = "retain_until_applied_on_ts")
    private OffsetDateTime retainUntilAppliedOn;

    @Column(name = "current_state")
    private String currentState;

    @Column(name = "comments")
    private String comments;

    @Column(name = "retention_object_id")
    private String retentionObjectId;

    @Column(name = "submitted_ts")
    private OffsetDateTime submitted;

}
