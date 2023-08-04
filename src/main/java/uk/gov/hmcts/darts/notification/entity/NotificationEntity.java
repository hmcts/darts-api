package uk.gov.hmcts.darts.notification.entity;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;

import java.time.OffsetDateTime;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = NotificationEntity.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEntity {

    public static final String ID = "not_id";
    public static final String EVENT_ID = "notification_event";
    public static final String CASE_ID = "cas_id";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String STATUS = "notification_status";
    public static final String ATTEMPTS = "send_attempts";
    public static final String TEMPLATE_VALUES = "template_values";
    public static final String CREATED_DATE_TIME = "created_ts";
    public static final String LAST_MODIFIED_TS = "last_modified_ts";
    public static final String TABLE_NAME = "notification";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "not_gen")
    @SequenceGenerator(name = "not_gen", sequenceName = "not_seq", allocationSize = 1)
    private Integer id;

    @Column(name = EVENT_ID)
    private String eventId;

    @JoinColumn(name = CASE_ID)
    @ManyToOne(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})
    private CourtCaseEntity courtCase;

    @Column(name = EMAIL_ADDRESS)
    private String emailAddress;

    @Column(name = STATUS)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = ATTEMPTS)
    private int attempts;

    @Column(name = TEMPLATE_VALUES)
    private String templateValues;

    @CreationTimestamp
    @Column(name = CREATED_DATE_TIME)
    private OffsetDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = LAST_MODIFIED_TS)
    private OffsetDateTime lastUpdated;

}
