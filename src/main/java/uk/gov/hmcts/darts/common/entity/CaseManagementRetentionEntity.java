package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = CaseManagementRetentionEntity.TABLE_NAME)
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class CaseManagementRetentionEntity {
    public static final String ID = "cmr_id";
    public static final String TABLE_NAME = "case_management_retention";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cmr_gen")
    @SequenceGenerator(name = "cmr_gen", sequenceName = "cmr_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne
    @JoinColumn(name = "rpt_id")
    private RetentionPolicyTypeEntity retentionPolicyTypeEntity;

    @ManyToOne
    @JoinColumn(name = "eve_id")
    private EventEntity eventEntity;

    @Column(name = "total_sentence")
    private String totalSentence;
}
