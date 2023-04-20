package uk.gov.hmcts.darts.entities;

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
@Table(name = "moj_daily_list")
@Data
public class DailyList {

    @Id
    @Column(name = "moj_dal_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "moj_crt_id")
    private Courthouse theCourthouse;

    @Column(name = "r_daily_list_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_unique_id", length = 200)
    private String uniqueId;

    @Column(name = "c_crown_court_name", length = 200)
    private String crownCourtName;

    @Column(name = "c_job_status", length = 20)
    private String jobStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "c_timestamp")
    private Date timestamp;

    @Column(name = "c_crown_court_code", length = 100)
    private String crownCourtCode;

    @Column(name = "c_daily_list_id")
    private Integer dailyListId;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_start_date")
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_end_date")
    private Date endDate;

    @Column(name = "c_daily_list_id_s", length = 100)
    private String dailyListIdString;

    @Column(name = "c_daily_list_source", length = 3)
    private String dailyListSource;

    @Column(name = "r_courthouse_object_id", length = 16)
    private String legacyCourthouseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
