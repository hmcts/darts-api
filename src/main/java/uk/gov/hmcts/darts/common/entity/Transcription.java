package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "moj_transcription")
@Data
public class Transcription {

    @Id
    @Column(name = "moj_tra_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "moj_cas_id")
    private Case theCase;

    @Column(name = "r_transcription_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_company", length = 64)
    private String company;

    @Column(name = "c_type")
    private Integer type;

    @Column(name = "c_notification_type", length = 64)
    private String notificationType;

    @Column(name = "c_urgent")
    private Integer urgent;

    @Column(name = "c_requestor", length = 32)
    private String requestor;

    @Column(name = "c_current_state", length = 32)
    private String currentState;

    @Column(name = "c_urgency", length = 32)
    private String urgency;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_hearing_date")
    private Date hearingDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_start")
    private Date start;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_end")
    private Date end;

    @Column(name = "c_courthouse", length = 64)
    private String courthouse;

    @Column(name = "c_courtroom", length = 64)
    private String courtroom;

    @Column(name = "c_reporting_restrictions")
    private Integer reportingRestrictions;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

    @OneToMany(mappedBy = "theTranscription")
    private Set<TranscriptionComment> theTranscriptionComments = new HashSet<>();
}
