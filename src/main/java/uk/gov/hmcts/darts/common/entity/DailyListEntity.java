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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = DailyListEntity.TABLE_NAME)
@Data
public class DailyListEntity {

    public static final String ID = "moj_dal_id";
    public static final String COURTHOUSE_ID = "moj_cth_id";
    public static final String DAILY_LIST_OBJECT_ID = "r_daily_list_object_id";
    public static final String UNIQUE_ID = "c_unique_id";
    public static final String JOB_STATUS = "c_job_status";
    public static final String TIMESTAMP = "c_timestamp";
    public static final String DAILY_LIST_ID = "c_daily_list_id";
    public static final String START_DATE = "c_start_date";
    public static final String END_DATE = "c_end_date";
    public static final String DAILY_LIST_ID_STRING = "c_daily_list_id_s";
    public static final String DAILY_LIST_SOURCE = "c_daily_list_source";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_UPDATED_DATE_TIME = "last_modified_ts";
    public static final String LEGACY_COURTHOUSE_OBJECT_ID = "r_courthouse_object_id";
    public static final String LEGACY_VERSION_LABEL = "r_version_label";
    public static final String SUPERSEDED = "i_superseded";
    public static final String VERSION_LABEL = "i_version";
    public static final String TABLE_NAME = "moj_daily_list";
    public static final String DAILY_LIST_CONTENT = "daily_list_content";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_dal_gen")
    @SequenceGenerator(name = "moj_dal_gen", sequenceName = "moj_dal_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = COURTHOUSE_ID)
    private Courthouse courthouse;

    @Column(name = DAILY_LIST_OBJECT_ID)
    private String legacyObjectId;

    @Column(name = UNIQUE_ID)
    private String uniqueId;

    @Column(name = JOB_STATUS)
    private String status;

    @Column(name = TIMESTAMP)
    private OffsetDateTime timestamp;

    @Column(name = DAILY_LIST_ID)
    private Integer dailyListId;

    @Column(name = START_DATE)
    private LocalDate startDate;

    @Column(name = END_DATE)
    private LocalDate endDate;

    @Column(name = DAILY_LIST_ID_STRING)
    private String legacyIdString;

    @Column(name = DAILY_LIST_SOURCE)
    private String source;

    @Column(name = DAILY_LIST_CONTENT)
    private String content;

    @Column(name = LEGACY_COURTHOUSE_OBJECT_ID)
    private String legacyCourthouseObjectId;

    @Column(name = LEGACY_VERSION_LABEL)
    private String legacyVersionLabel;

    @Column(name = SUPERSEDED)
    private Boolean superseded;

    @Version
    @Column(name = VERSION_LABEL)
    private Short version;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDate;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime modifiedDateTime;

}
