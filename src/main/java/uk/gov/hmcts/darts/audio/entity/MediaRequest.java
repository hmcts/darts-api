package uk.gov.hmcts.darts.audio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestType;

import java.time.OffsetDateTime;

@Entity
@Table(name = MediaRequest.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaRequest {

    public static final String REQUEST_ID = "moj_mer_id";
    public static final String HEARING_ID = "moj_hea_id";
    public static final String REQUESTOR = "requestor";
    public static final String REQUEST_STATUS = "request_status";
    public static final String REQUEST_TYPE = "request_type";
    public static final String REQ_PROC_ATTEMPTS = "req_proc_attempts";
    public static final String START_TIME = "start_ts";
    public static final String END_TIME = "end_ts";
    public static final String OUTBOUND_LOCATION = "outbound_location";
    public static final String OUTPUT_FORMAT = "output_format";
    public static final String OUTPUT_FILENAME = "output_filename";
    public static final String LAST_ACCESSED_DATE_TIME = "last_accessed_ts";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_UPDATED_DATE_TIME = "last_updated_ts";
    public static final String TABLE_NAME = "moj_media_request";

    @Id
    @Column(name = REQUEST_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_request_gen")
    @SequenceGenerator(name = "media_request_gen", sequenceName = "moj_mer_seq", allocationSize = 1)
    private Integer requestId;

    @Column(name = HEARING_ID)
    private Integer hearingId;

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

    @Column(name = OUTBOUND_LOCATION)
    private String outboundLocation;

    @Column(name = OUTPUT_FORMAT)
    private AudioRequestOutputFormat outputFormat;

    @Column(name = OUTPUT_FILENAME)
    private String outputFilename;

    @Column(name = LAST_ACCESSED_DATE_TIME)
    private OffsetDateTime lastAccessedDateTime;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime lastUpdatedDateTime;

}

