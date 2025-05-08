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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = NotificationEntity.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationEntity extends CreatedModifiedBaseEntity {

    public static final String NOT_ID = "not_id";
    public static final String EVENT_ID = "notification_event";
    public static final String CASE_ID = "cas_id";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String NOTIFICATION_STATUS = "notification_status";
    public static final String SEND_ATTEMPTS = "send_attempts";
    public static final String TEMPLATE_VALUES = "template_values";
    public static final String TABLE_NAME = "notification";

    @Id
    @Column(name = NOT_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "not_gen")
    @SequenceGenerator(name = "not_gen", sequenceName = "not_seq", allocationSize = 1)
    private Long id;

    @Column(name = EVENT_ID)
    private String eventId;

    @JoinColumn(name = CASE_ID)
    @ManyToOne(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})
    private CourtCaseEntity courtCase;

    @Column(name = EMAIL_ADDRESS)
    private String emailAddress;

    @Column(name = NOTIFICATION_STATUS)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = SEND_ATTEMPTS)
    private Integer attempts;

    @Column(name = TEMPLATE_VALUES)
    private String templateValues;

}
