package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

@Entity
@Table(name = "audit")
@Getter
@Setter
public class AuditEntity extends CreatedModifiedBaseEntity {
    @Id
    @Column(name = "aud_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_gen")
    @SequenceGenerator(name = "audit_gen", sequenceName = "audit_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "cas_id")
    private Integer caseId;

    @Column(name = "aua_id")
    private Integer eventId;

    @Column(name = "usr_id")
    private Integer userId;

    @Column(name = "application_server")
    private String applicationServer;

    @Column(name = "additional_data")
    private String additionalData;
}
