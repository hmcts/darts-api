package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = CaseOverflowEntity.TABLE_NAME)
@Getter
@Setter
public class CaseOverflowEntity {

    public static final String ID = "cas_id";
    public static final String TABLE_NAME = "case_overflow";

    @Id
    @Column(name = ID)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @Column(name = "case_total_sentence")
    private String caseTotalSentence;

    @Column(name = "retention_event_ts")
    private LocalDateTime retentionEventTs;

    @Column(name = "case_retention_fixed")
    private String caseRetentionFixed;

    @Column(name = "retention_applies_from_ts")
    private LocalDateTime retentionAppliesFromTimestamp;

    @Column(name = "end_of_sentence_date_ts")
    private LocalDateTime endOfSentenceTimestamp;

    @Column(name = "manual_retention_override")
    private Integer manualRetentionOverride;

    @Column(name = "retain_until_ts")
    private LocalDateTime retainUntilTs;

    @Column(name = "is_standard_policy")
    private Boolean isStandardPolicy;

    @Column(name = "is_permanent_policy")
    private Boolean isPermanentPolicy;

    @Column(name = "audio_folder_object_id", updatable = false, insertable = false)
    private String audioFolderObjectId;

}
