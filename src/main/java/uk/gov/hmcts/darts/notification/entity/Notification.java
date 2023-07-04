package uk.gov.hmcts.darts.notification.entity;

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
@Table(name = Notification.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    public static final String ID = "not_id";
    public static final String EVENT_TYPE = "notification_event";
    public static final String CASE_ID = "cas_id";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String STATUS = "notification_status";
    public static final String ATTEMPTS = "send_attempts";
    public static final String TEMPLATE_VALUES = "template_values";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_UPDATED_DATE_TIME = "last_updated_ts";
    public static final String TABLE_NAME = "notification";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_gen")
    @SequenceGenerator(name = "notification_gen", sequenceName = "not_seq", allocationSize = 1)
    private Integer id;

    @Column(name = EVENT_TYPE)
    private String eventType;

    @Column(name = CASE_ID)
    private String caseId;

    @Column(name = EMAIL_ADDRESS)
    private String emailAddress;

    @Column(name = STATUS)
    private String status;

    @Column(name = ATTEMPTS)
    private int attempts;

    @Column(name = TEMPLATE_VALUES)
    private String templateValues;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private OffsetDateTime lastUpdatedDateTime;

}
