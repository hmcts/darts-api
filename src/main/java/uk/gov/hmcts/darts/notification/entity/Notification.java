package uk.gov.hmcts.darts.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.sql.Timestamp;

@Entity
@Table(name = Notification.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Audited
public class Notification {

    public static final String ID = "id";
    public static final String EVENT_ID = "event_id";
    public static final String CASE_ID = "case_id";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String STATUS = "status";
    public static final String ATTEMPTS = "attempts";
    public static final String TEMPLATE_VALUES = "template_values";
    public static final String CREATED_DATE_TIME = "created_date_time";
    public static final String LAST_UPDATED_DATE_TIME = "last_updated_date_time";
    public static final String TABLE_NAME = "notification";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @Column(name = EVENT_ID)
    private String eventId;

    @Column(name = CASE_ID)
    private String caseId;

    @Column(name = EMAIL_ADDRESS)
    private String emailAddress;

    @Column(name = STATUS)
    private String status;

    @Column(name = ATTEMPTS)
    @ColumnDefault("0")
    private int attempts;

    @Column(name = TEMPLATE_VALUES)
    private String templateValues;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_UPDATED_DATE_TIME)
    private Timestamp lastUpdatedDateTime;

}
