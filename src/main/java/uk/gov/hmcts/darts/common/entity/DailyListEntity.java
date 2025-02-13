package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = DailyListEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DailyListEntity extends CreatedModifiedBaseEntity {

    public static final String ID = "dal_id";
    public static final String LISTING_COURTHOUSE = "listing_courthouse";
    public static final String DAILY_LIST_OBJECT_ID = "daily_list_object_id";
    public static final String UNIQUE_ID = "unique_id";
    public static final String JOB_STATUS = "job_status";
    public static final String TIMESTAMP = "published_ts";
    public static final String START_DATE = "start_dt";
    public static final String END_DATE = "end_dt";
    public static final String DAILY_LIST_ID_STRING = "daily_list_id_s";
    public static final String DAILY_LIST_SOURCE = "daily_list_source";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String TABLE_NAME = "daily_list";
    public static final String DAILY_LIST_CONTENT_JSON = "daily_list_content_json";
    public static final String DAILY_LIST_CONTENT_XML = "daily_list_content_xml";
    public static final String MESSAGE_ID = "message_id";
    public static final String COURTHOUSE_CODE = "courthouse_code";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dal_gen")
    @SequenceGenerator(name = "dal_gen", sequenceName = "dal_seq", allocationSize = 1)
    private Integer id;

    @Column(name = LISTING_COURTHOUSE)
    private String listingCourthouse;

    @Column(name = COURTHOUSE_CODE)
    private Integer courthouseCode;

    @Column(name = DAILY_LIST_OBJECT_ID)
    private String legacyObjectId;

    @Column(name = UNIQUE_ID)
    private String uniqueId;

    @Column(name = JOB_STATUS)
    @Enumerated(EnumType.STRING)
    private JobStatusType status;

    @Column(name = TIMESTAMP)
    private OffsetDateTime publishedTimestamp;

    @Column(name = START_DATE)
    private LocalDate startDate;

    @Column(name = END_DATE)
    private LocalDate endDate;

    @Column(name = DAILY_LIST_ID_STRING)
    private String legacyIdString;

    @Column(name = DAILY_LIST_SOURCE)
    private String source;

    @Column(name = DAILY_LIST_CONTENT_JSON)
    private String content;

    @Column(name = DAILY_LIST_CONTENT_XML)
    private String xmlContent;

    @Column(name = MESSAGE_ID)
    private String messageId;

    @Column(name = "content_object_id", length = 16)
    private String contentObjectId;

    @Column(name = "clip_id")
    private String clipId;

    @Column(name = "external_location")
    private UUID externalLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elt_id")
    private ExternalLocationTypeEntity externalLocationTypeEntity;

    @Column(name = "subcontent_object_id", length = 16)
    private String subcontentObjectId;

    @Column(name = "subcontent_position")
    private Integer subcontentPosition;

    @Column(name = "data_ticket")
    private Integer dataTicket;

    @Column(name = "storage_id", length = 16)
    private String storageId;

}
