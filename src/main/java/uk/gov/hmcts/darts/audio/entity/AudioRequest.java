package uk.gov.hmcts.darts.audio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = AudioRequest.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioRequest {

    public static final String REQUEST_ID = "request_id";
    public static final String CASE_ID = "case_id";
    public static final String REQUESTER = "requester";
    public static final String START_TIME = "start_time";
    public static final String END_TIME = "end_time";
    public static final String REQUEST_TYPE = "request_type";
    public static final String STATUS = "status";
    public static final String ATTEMPTS = "attempts";
    public static final String OUTBOUND_LOCATION = "outbound_location";
    public static final String CREATED_DATE_TIME = "created_date_time";
    public static final String LAST_UPDATED_DATE_TIME = "last_updated_date_time";
    public static final String TABLE_NAME = "audio_request";

    @Id
    @Column(name = REQUEST_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audio_request_gen")
    @SequenceGenerator(name = "audio_request_gen", sequenceName = "audio_request_seq", allocationSize = 1)
    private Integer requestId;

    @Column(name = CASE_ID)
    private String caseId;

    @Column(name = REQUESTER)
    private String requester;

    @Column(name = START_TIME)
    private OffsetDateTime startTime;

    @Column(name = END_TIME)
    private OffsetDateTime endTime;

    @Column(name = REQUEST_TYPE)
    private String requestType;

    @Column(name = STATUS)
    private String status;

    @Column(name = ATTEMPTS)
    private int attempts;

    @Column(name = OUTBOUND_LOCATION)
    private String outboundLocation;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime lastUpdatedDateTime;

}

