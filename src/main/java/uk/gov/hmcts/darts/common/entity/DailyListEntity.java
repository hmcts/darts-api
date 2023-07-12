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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = DailyListEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DailyListEntity extends VersionedEntity {

    public static final String ID = "dal_id";
    public static final String COURTHOUSE_ID = "cth_id";
    public static final String DAILY_LIST_OBJECT_ID = "r_daily_list_object_id";
    public static final String UNIQUE_ID = "unique_id";
    public static final String JOB_STATUS = "job_status";
    public static final String TIMESTAMP = "published_ts";
    public static final String DAILY_LIST_ID = "daily_list_id";
    public static final String START_DATE = "start_dt";
    public static final String END_DATE = "end_dt";
    public static final String DAILY_LIST_ID_STRING = "daily_list_id_s";
    public static final String DAILY_LIST_SOURCE = "daily_list_source";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_UPDATED_DATE_TIME = "last_modified_ts";
    public static final String LEGACY_VERSION_LABEL = "version_label";
    public static final String SUPERSEDED = "superseded";
    public static final String TABLE_NAME = "daily_list";
    public static final String DAILY_LIST_CONTENT = "daily_list_content";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dal_gen")
    @SequenceGenerator(name = "dal_gen", sequenceName = "dal_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = COURTHOUSE_ID)
    private CourthouseEntity courthouse;

    @Column(name = DAILY_LIST_OBJECT_ID)
    private String legacyObjectId;

    @Column(name = UNIQUE_ID)
    private String uniqueId;

    @Column(name = JOB_STATUS)
    private String status;

    @Column(name = TIMESTAMP)
    private OffsetDateTime publishedTimestamp;

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

    @Column(name = LEGACY_VERSION_LABEL)
    private String legacyVersionLabel;

    @Column(name = SUPERSEDED)
    private Boolean superseded;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDate;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime modifiedDateTime;

}
