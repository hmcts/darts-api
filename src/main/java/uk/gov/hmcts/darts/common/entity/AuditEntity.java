package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aud_gen")
    @SequenceGenerator(name = "aud_gen", sequenceName = "aud_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne
    @JoinColumn(name = "aua_id", nullable = false)
    private AuditActivityEntity auditActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_id", nullable = false)
    private UserAccountEntity user;

    @Column(name = "additional_data")
    private String additionalData;
}
