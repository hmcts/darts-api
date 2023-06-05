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
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "moj_transcription")
@Data
public class Transcription {

    @Id
    @Column(name = "moj_tra_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_tra_gen")
    @SequenceGenerator(name = "moj_tra_gen", sequenceName = "moj_tra_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "moj_cas_id", nullable = false)
    private Case theCase;

    @Column(name = "moj_crt_id", nullable = false)
    private Integer courthouseId;

    @Column(name = "moj_trt_id", nullable = false)
    private Integer transcriptionTypeId;

    @Column(name = "moj_urg_id")
    private Integer transcriptionUrgencyId;

    @Column(name = "moj_hea_id")
    private Integer hearingId;

    @Column(name = "r_transcription_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_company")
    private String company;

    @Column(name = "c_requestor")
    private String requestor;

    @Column(name = "c_current_state")
    private String currentState;

    @Column(name = "i_current_state_ts")
    private OffsetDateTime currentStateTimestamp;

    @Column(name = "c_hearing_date")
    private OffsetDateTime hearingDate;

    @Column(name = "c_start")
    private OffsetDateTime start;

    @Column(name = "c_end")
    private OffsetDateTime end;

    @Column(name = "c_courtroom")
    private String courtroom;

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

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version")
    private Short version;

    @OneToMany(mappedBy = "theTranscription")
    private Set<TranscriptionComment> theTranscriptionComments = new HashSet<>();
}
