package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = CaseRetentionEventEntity.TABLE_NAME)
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class CaseRetentionEventEntity extends CreatedModifiedBaseEntity {


    public static final String ID = "cre_id";
    public static final String TABLE_NAME = "case_retention_event";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cre_gen")
    @SequenceGenerator(name = "cre_gen", sequenceName = "cre_seq", allocationSize = 1)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "car_id")
    private CaseRetentionEntity caseRetention;

    @Column(name = "sentencing_type")
    private Integer sentencingType;

    @Column(name = "total_sentencing")
    private String totalSentencing;

    @Column(name = "last_processed_event_ts")
    private LocalDateTime lastProcessedEventTimestamp;

    @Column(name = "submitted_by")
    private Integer submittedBy;

    @Column(name = "user_comment")
    private String userComment;

}
