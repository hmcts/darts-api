package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "transcription")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TranscriptionEntity extends VersionedEntity {

    @Id
    @Column(name = "tra_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tra_gen")
    @SequenceGenerator(name = "tra_gen", sequenceName = "tra_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cas_id", nullable = false)
    private CourtCaseEntity courtCase;

    @Column(name = "ctr_id", nullable = false)
    private Integer courtroomId;

    @Column(name = "trt_id", nullable = false)
    private Integer transcriptionTypeId;

    @Column(name = "urg_id")
    private Integer transcriptionUrgencyId;

    @Column(name = "hea_id")
    private Integer hearingId;

    @Column(name = "transcription_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "company")
    private String company;

    @Column(name = "requestor")
    private String requestor;

    @Column(name = "current_state")
    private String currentState;

    @Column(name = "current_state_ts")
    private OffsetDateTime currentStateTimestamp;

    @Column(name = "hearing_date")
    private OffsetDateTime hearingDate;

    @Column(name = "start_ts")
    private OffsetDateTime start;

    @Column(name = "end_ts")
    private OffsetDateTime end;

    @Column(name = "created_ts")
    private OffsetDateTime createdTimestamp;

    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedTimestamp;

    @Column(name = "last_modified_by")
    private Integer lastModifiedByUserId;

    @Column(name = "requested_by")
    private Integer requestedByUserId;

    @Column(name = "approved_by")
    private Integer approvedByUserId;

    @Column(name = "approved_on_ts")
    private OffsetDateTime approvedOnTimestamp;

    @Column(name = "transcribed_by")
    private Integer transcribedByUserId;

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "superseded")
    private Boolean superseded;

    @OneToMany(mappedBy = "transcription")
    private List<TranscriptionCommentEntity> transcriptionComments;

}
