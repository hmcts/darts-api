package uk.gov.hmcts.darts.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "moj_case")
@NoArgsConstructor
@Getter
@Setter
@SuppressWarnings({"PMD.ShortClassName"})
public class Case {

    private static final String MAPPED_BY_THE_CASE = "theCase";

    @Id
    @Column(name = "moj_cas_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_crt_id")
    private Courthouse theCourthouse;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "c_type", length = 32)
    private String type;

    @Column(name = "c_case_id", length = 32)
    private String caseId;

    @Column(name = "c_courthouse", length = 64)
    private String courthouse;

    @Column(name = "c_courtroom", length = 64)
    private String courtroom;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_scheduled_start")
    private Date scheduledStart;

    @Column(name = "c_upload_priority")
    private Integer uploadPriority;

    @Column(name = "c_reporting_restrictions", length = 128)
    private String reportingRestrictions;

    @Column(name = "c_closed")
    private Integer closed;

    @Column(name = "c_interpreter_used")
    private Short interpreterUsed;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_case_closed_date")
    private Date caseClosedDate;

    @Column(name = "r_courthouse_object_id", length = 16)
    private String legacyCourthouseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private Set<TransformationRequest> theTransformationRequests = new HashSet<>();

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private Set<TransformationLog> theTransformationLogs = new HashSet<>();

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private Set<CachedMedia> theCachedMedias = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "moj_case_media_ae")
    private Set<Media> theMedias = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "moj_case_event_ae")
    private Set<Event> theEvents = new HashSet<>();

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private Set<Transcription> theTranscriptions = new HashSet<>();

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private Set<Hearing> theHearings = new HashSet<>();

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private Set<Annotation> theAnnotations = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(caseId, courthouse, courtroom, type, uploadPriority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Case other = (Case) obj;
        return Objects.equals(caseId, other.caseId) && Objects.equals(courthouse, other.courthouse)
            && Objects.equals(courtroom, other.courtroom) && Objects.equals(type, other.type)
            && Objects.equals(uploadPriority, other.uploadPriority);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(250);
        builder.append("Case [id=").append(id)
            .append(", legacyCaseObjectId=").append(legacyCaseObjectId)
            .append(", type=").append(type)
            .append(", caseId=").append(caseId)
            .append(", courthouse=").append(courthouse)
            .append(", courtroom=").append(courtroom)
            .append(", scheduledStart=").append(scheduledStart)
            .append(", uploadPriority=").append(uploadPriority)
            .append(", reportingRestrictions=").append(reportingRestrictions)
            .append(", closed=").append(closed)
            .append(", interpreterUsed=").append(interpreterUsed)
            .append(", caseClosedDate=").append(caseClosedDate)
            .append(", legacyCourthouseObjectId=").append(legacyCourthouseObjectId)
            .append(", legacyVersionLabel=").append(legacyVersionLabel)
            .append(", superseded=").append(superseded)
            .append(", version=").append(version)
            .append(']');
        return builder.toString();
    }
}
