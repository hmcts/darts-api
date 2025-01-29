package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rpt_id")
    private RetentionPolicyTypeEntity retentionPolicyType;

    @Column(name = "case_total_sentence")
    private String caseTotalSentence;

    @Column(name = "retention_event_ts")
    private OffsetDateTime retentionEventDateTime;

    @Column(name = "case_retention_fixed")
    private String caseRetentionFixed;

    @Column(name = "retention_applies_from_ts")
    private OffsetDateTime retentionAppliesFromDateTime;

    @Column(name = "end_of_sentence_date_ts")
    private OffsetDateTime endOfSentenceDateTime;

    @Column(name = "manual_retention_override")
    private Integer manualRetentionOverride;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilDateTime;

    @Column(name = "audio_folder_object_id", updatable = false, insertable = false)
    private String audioFolderObjectId;

    @Column(name = "c_closed_pre_live")
    private Integer caseClosedPreLive;

    @Column(name = "c_case_closed_date_pre_live")
    private OffsetDateTime caseClosedDatePreLive;

    @Column(name = "case_created_ts")
    private OffsetDateTime caseCreatedAt;

    @Column(name = "case_last_modified_ts")
    private OffsetDateTime caseLastModifiedAt;

    @Column(name = "audio_last_modified_ts")
    private OffsetDateTime audioLastModifiedAt;

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedAt;

}
