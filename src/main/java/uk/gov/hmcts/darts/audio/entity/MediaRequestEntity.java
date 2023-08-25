package uk.gov.hmcts.darts.audio.entity;

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
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = MediaRequestEntity.TABLE_NAME)
@Getter
@Setter
public class MediaRequestEntity extends CreatedModifiedBaseEntity {

    public static final String ID_COLUMN_NAME = "mer_id";
    public static final String HEARING_ID_COLUMN_NAME = "hea_id";
    public static final String REQUESTOR_COLUMN_NAME = "requestor";
    public static final String REQUEST_STATUS_COLUMN_NAME = "request_status";
    public static final String REQUEST_TYPE_COLUMN_NAME = "request_type";
    public static final String REQ_PROC_ATTEMPTS_COLUMN_NAME = "req_proc_attempts";
    public static final String START_TIME_COLUMN_NAME = "start_ts";
    public static final String END_TIME_COLUMN_NAME = "end_ts";
    public static final String OUTPUT_FORMAT_COLUMN_NAME = "output_format";
    public static final String OUTPUT_FILENAME_COLUMN_NAME = "output_filename";
    public static final String LAST_ACCESSED_TS_COLUMN_NAME = "last_accessed_ts";
    public static final String EXPIRY_TS_COLUMN_NAME = "expiry_ts";
    public static final String CREATED_TS_COLUMN_NAME = "created_ts";
    public static final String CREATED_BY_COLUMN_NAME = "created_by";
    public static final String LAST_MODIFIED_TS_COLUMN_NAME = "last_modified_ts";
    public static final String LAST_MODIFIED_BY_COLUMN_NAME = "last_modified_by";
    public static final String TABLE_NAME = "media_request";

    @Id
    @Column(name = ID_COLUMN_NAME)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_request_gen")
    @SequenceGenerator(name = "media_request_gen", sequenceName = "mer_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = HEARING_ID_COLUMN_NAME, nullable = false)
    private HearingEntity hearing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = REQUESTOR_COLUMN_NAME, nullable = false)
    private UserAccountEntity requestor;

    @Column(name = REQUEST_STATUS_COLUMN_NAME, nullable = false)
    @Enumerated(EnumType.STRING)
    private AudioRequestStatus status;

    @Column(name = REQUEST_TYPE_COLUMN_NAME, nullable = false)
    @Enumerated(EnumType.STRING)
    private AudioRequestType requestType;

    @Column(name = REQ_PROC_ATTEMPTS_COLUMN_NAME)
    private Integer attempts;

    @Column(name = START_TIME_COLUMN_NAME, nullable = false)
    private OffsetDateTime startTime;

    @Column(name = END_TIME_COLUMN_NAME, nullable = false)
    private OffsetDateTime endTime;

    @Column(name = OUTPUT_FORMAT_COLUMN_NAME)
    @Enumerated(EnumType.STRING)
    private AudioRequestOutputFormat outputFormat;

    @Column(name = OUTPUT_FILENAME_COLUMN_NAME)
    private String outputFilename;

    @Column(name = LAST_ACCESSED_TS_COLUMN_NAME)
    private OffsetDateTime lastAccessedDateTime;

    @Column(name = EXPIRY_TS_COLUMN_NAME)
    private OffsetDateTime expiryTime;

}

