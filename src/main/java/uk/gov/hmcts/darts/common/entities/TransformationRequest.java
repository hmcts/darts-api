package uk.gov.hmcts.darts.common.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "moj_transformation_request")
@Data
public class TransformationRequest {

    @Id
    @Column(name = "moj_trr_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_cas_id")
    private Case theCase;

    @Column(name = "r_transformation_request_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_type", length = 12)
    private String type;

    @Column(name = "c_output_format", length = 12)
    private String outputFormat;

    @Column(name = "c_audio_folder_id", length = 16)
    private String audioFolderId;

    @Column(name = "c_output_file", length = 100)
    private String outputFile;

    @Column(name = "c_requestor", length = 32)
    private String requestor;

    @Column(name = "c_court_log_id", length = 16)
    private String courtLogId;

    @Column(name = "c_priority")
    private Integer priority;

    @Column(name = "c_channel")
    private Integer channel;

    @Column(name = "c_total_channels")
    private Integer totalChannels;

    @Column(name = "c_reference_id", length = 32)
    private String referenceId;

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

}
