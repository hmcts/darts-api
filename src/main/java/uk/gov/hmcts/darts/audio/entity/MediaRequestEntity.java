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

import java.time.OffsetDateTime;

@Entity
@Table(name = MediaRequestEntity.TABLE_NAME)
@Getter
@Setter
public class MediaRequestEntity {

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
    public static final String LAST_ACCESSED_DATE_TIME = "last_accessed_ts";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_MODIFIED_TS = "last_modified_ts";
    public static final String TABLE_NAME = "media_request";

    @Id
    @Column(name = REQUEST_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_request_gen")
    @SequenceGenerator(name = "media_request_gen", sequenceName = "mer_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = HEARING_ID)
    private HearingEntity hearing;

    @Column(name = REQUESTOR)
    private Integer requestor;

    @Column(name = REQUEST_STATUS)
    @Enumerated(EnumType.STRING)
    private AudioRequestStatus status;

    @Column(name = REQUEST_TYPE)
    @Enumerated(EnumType.STRING)
    private AudioRequestType requestType;

    @Column(name = REQ_PROC_ATTEMPTS)
    private Integer attempts;

    @Column(name = START_TIME)
    private OffsetDateTime startTime;

    @Column(name = END_TIME)
    private OffsetDateTime endTime;

    @Column(name = OUTPUT_FORMAT)
    @Enumerated(EnumType.STRING)
    private AudioRequestOutputFormat outputFormat;

    @Column(name = OUTPUT_FILENAME)
    private String outputFilename;

    @Column(name = LAST_ACCESSED_DATE_TIME)
    private OffsetDateTime lastAccessedDateTime;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_MODIFIED_TS)
    private OffsetDateTime lastUpdated;

}

