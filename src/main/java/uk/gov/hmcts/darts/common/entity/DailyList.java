package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moj_daily_list")
@Data
public class DailyList {

    @Id
    @Column(name = "moj_dal_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_dal_gen")
    @SequenceGenerator(name = "moj_dal_gen", sequenceName = "moj_dal_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "moj_crt_id")
    private Courthouse theCourthouse;

    @Column(name = "r_daily_list_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_unique_id")
    private String uniqueId;

    @Column(name = "c_job_status")
    private String jobStatus;

    @Column(name = "c_timestamp")
    private OffsetDateTime timestamp;

    @Column(name = "c_daily_list_id")
    private Integer dailyListId;

    @Column(name = "c_start_date")
    private OffsetDateTime startDate;

    @Column(name = "c_end_date")
    private OffsetDateTime endDate;

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
    @Column(name = "i_version")
    private Short version;

}
