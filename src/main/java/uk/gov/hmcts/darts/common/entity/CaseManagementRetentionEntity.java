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
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = CaseManagementRetentionEntity.TABLE_NAME)
@Getter
@Setter
public class CaseManagementRetentionEntity {
    public static final String ID = "cmr_id";
    public static final String TABLE_NAME = "case_management_retention";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cmr_gen")
    @SequenceGenerator(name = "cmr_gen", sequenceName = "cmr_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cas_id", nullable = false)
    private CourtCaseEntity courtCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rpt_id", nullable = false)
    private RetentionPolicyTypeEntity retentionPolicyTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eve_id", nullable = false)
    private EventEntity eventEntity;

    @Column(name = "total_sentence")
    private String totalSentence;
}
