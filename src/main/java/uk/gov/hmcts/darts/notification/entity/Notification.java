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

import java.sql.Timestamp;

@Entity
@Table(name = "notification")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer id;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "case_id")
    private String caseId;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "status")
    private String status;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "template_values")
    private String templateValues;

    @Column(name = "created_datetime")
    private Timestamp createdDatetime;

    @Column(name = "last_updated_datetime")
    private Timestamp lastUpdatedDatetime;


}
