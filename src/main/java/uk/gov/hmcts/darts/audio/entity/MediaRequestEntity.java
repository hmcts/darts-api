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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JpaAuditing;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = MediaRequestEntity.TABLE_NAME)
@Getter
@Setter
public class MediaRequestEntity implements JpaAuditing {

    public static final String REQUEST_ID = "mer_id";
    public static final String HEARING_ID = "hea_id";
    public static final String REQUESTOR = "requestor";
    public static final String REQUEST_STATUS = "request_status";
    public static final String REQUEST_TYPE = "request_type";
    public static final String REQ_PROC_ATTEMPTS = "req_proc_attempts";
    public static final String START_TIME = "start_ts";
    public static final String END_TIME = "end_ts";
    public static final String OUTPUT_FORMAT = "output_format";
    public static final String OUTPUT_FILENAME = "output_filename";
    public static final String LAST_ACCESSED_TS = "last_accessed_ts";
    public static final String EXPIRY_TS = "expiry_ts";
    public static final String CREATED_TS = "created_ts";
    public static final String CREATED_BY = "created_by";
    public static final String LAST_MODIFIED_TS = "last_modified_ts";
    public static final String LAST_MODIFIED_BY = "last_modified_by";
    public static final String TABLE_NAME = "media_request";

    @Id
    @Column(name = REQUEST_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_request_gen")
    @SequenceGenerator(name = "media_request_gen", sequenceName = "mer_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = HEARING_ID, nullable = false)
    private HearingEntity hearing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = REQUESTOR, nullable = false)
    private UserAccountEntity requestor;

    @Column(name = REQUEST_STATUS, nullable = false)
    @Enumerated(EnumType.STRING)
    private AudioRequestStatus status;

    @Column(name = REQUEST_TYPE, nullable = false)
    @Enumerated(EnumType.STRING)
    private AudioRequestType requestType;

    @Column(name = REQ_PROC_ATTEMPTS)
    private Integer attempts;

    @Column(name = START_TIME, nullable = false)
    private OffsetDateTime startTime;

    @Column(name = END_TIME, nullable = false)
    private OffsetDateTime endTime;

    @Column(name = OUTPUT_FORMAT)
    @Enumerated(EnumType.STRING)
    private AudioRequestOutputFormat outputFormat;

    @Column(name = OUTPUT_FILENAME)
    private String outputFilename;

    @Column(name = LAST_ACCESSED_TS)
    private OffsetDateTime lastAccessedDateTime;

    @Column(name = EXPIRY_TS)
    private OffsetDateTime expiryTime;

    @CreationTimestamp
    @Column(name = CREATED_TS, nullable = false)
    private OffsetDateTime createdTimestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = CREATED_BY, nullable = false)
    private UserAccountEntity createdBy;

    @UpdateTimestamp
    @Column(name = LAST_MODIFIED_TS, nullable = false)
    private OffsetDateTime modifiedTimestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = LAST_MODIFIED_BY, nullable = false)
    private UserAccountEntity modifiedBy;

}

