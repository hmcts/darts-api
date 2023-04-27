package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "moj_cached_media")
@Data
public class CachedMedia {

    @Id
    @Column(name = "moj_med_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "moj_cas_id")
    private Case theCase;

    @Column(name = "r_cached_media_object_id", length = 16)
    private String legacyObjectId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "c_last_accessed")
    private Date lastAccessed;

    @Column(name = "c_log_id", length = 16)
    private String logId;

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

    @Column(name = "r_case_object_id", length = 32)
    private String legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
