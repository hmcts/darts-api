package uk.gov.hmcts.darts.audio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.notification.entity.Notification;

import java.sql.Timestamp;

@Entity
@Table(name = Notification.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioRequest {

    public static final String REQUEST_ID = "request_id";
    public static final String CASE_ID = "case_id";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String START_TIME = "start_time";
    public static final String END_TIME = "end_time";
    public static final String REQUEST_TYPE = "request_type";
    public static final String STATUS = "status";
    public static final String ATTEMPTS = "attempts";
    public static final String CREATED_DATE_TIME = "created_date_time";
    public static final String LAST_UPDATED_DATE_TIME = "last_updated_date_time";
    public static final String TABLE_NAME = "audio_request";

    @Id
    @Column(name = REQUEST_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @Column(name = CASE_ID)
    private String caseId;

    @Column(name = EMAIL_ADDRESS)
    private String emailAddress;

    @Column(name = START_TIME)
    private Timestamp startTime;

    @Column(name = END_TIME)
    private Timestamp endTime;

    @Column(name = REQUEST_TYPE)
    private String requestType;

    @Column(name = STATUS)
    private String status;

    @Column(name = ATTEMPTS)
    private int attempts;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private Timestamp lastUpdatedDateTime;

}

