package uk.gov.hmcts.darts.dailylist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = DailyListEntity.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyListEntity {

    public static final String ID = "moj_dal_id";
    public static final String COURTHOUSE_ID = "moj_crt_id";
    public static final String DAILY_LIST_OBJECT_ID = "r_daily_list_object_id";
    public static final String UNIQUE_ID = "c_unique_id";
    public static final String JOB_STATUS = "c_job_status";
    public static final String PUBLISHED_TIME = "c_timestamp";
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
    public static final String VERSION_LABEL = "i_version_label";
    public static final String TABLE_NAME = "moj_daily_list";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_dal_gen")
    @SequenceGenerator(name = "moj_dal_gen", sequenceName = "moj_dal_seq", allocationSize = 1)
    private Integer id;

    @Column(name = COURTHOUSE_ID)
    private Integer crtId;

    @Column(name = DAILY_LIST_OBJECT_ID)
    private String dailyListObjectId;

    @Column(name = UNIQUE_ID)
    private String uniqueID;

    @Column(name = JOB_STATUS)
    private String jobStatus;

    @Column(name = PUBLISHED_TIME)
    private OffsetDateTime publishedTime;

    @Column(name = DAILY_LIST_ID)
    private Integer dailylistId;

    @Column(name = START_DATE)
    private OffsetDateTime startDate;

    @Column(name = END_DATE)
    private OffsetDateTime endDate;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime lastUpdatedDateTime;

    @Column(name = DAILY_LIST_ID_STRING, length = 100)
    private String dailyListIdString;

    @Column(name = DAILY_LIST_SOURCE, length = 3)
    private String dailyListSource;

    @Column(name = LEGACY_COURTHOUSE_OBJECT_ID, length = 16)
    private String legacyCourthouseObjectId;

    @Column(name = LEGACY_VERSION_LABEL, length = 32)
    private String legacyVersionLabel;

    @Column(name = SUPERSEDED)
    private Boolean superseded;

    @Version
    @Column(name = VERSION_LABEL)
    private Short version;


}

