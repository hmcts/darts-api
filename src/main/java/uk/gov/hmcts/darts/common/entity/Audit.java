package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit")
@Data
public class Audit {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_gen")
    @SequenceGenerator(name = "audit_gen", sequenceName = "audit_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "case_id")
    private Integer caseId;

    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "moj_usr_id")
    private Integer userId;

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdAt;

    @Column(name = "application_server")
    private String applicationServer;

    @Column(name = "additional_data")
    private String additionalData;
}
