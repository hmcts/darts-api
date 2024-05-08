package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

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
    private OffsetDateTime retentionEventTs;

    @Column(name = "case_retention_fixed")
    private String caseRetentionFixed;

    @Column(name = "retention_applies_from_ts")
    private OffsetDateTime retentionAppliesFromTimestamp;

    @Column(name = "end_of_sentence_date_ts")
    private OffsetDateTime endOfSentenceTimestamp;

    @Column(name = "manual_retention_override")
    private Integer manualRetentionOverride;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTs;

    @Column(name = "is_standard_policy")
    private Boolean isStandardPolicy;

    @Column(name = "is_permanent_policy")
    private Boolean isPermanentPolicy;

    @Column(name = "audio_folder_object_id", updatable = false, insertable = false)
    private String audioFolderObjectId;

    @Column(name = "checked_ts")
    private OffsetDateTime checkedTs;

    @Column(name = "corrected_ts")
    private OffsetDateTime correctedTs;

    @Column(name = "confidence_level")
    private Integer confidenceLevel;

    @Column(name = "confidence_reason")
    private String confidenceReason;

    @Column(name = "c_closed_pre_live")
    private Integer caseClosedPreLive;

    @Column(name = "c_case_closed_date_pre_live")
    private OffsetDateTime caseClosedDatePreLive;

}
