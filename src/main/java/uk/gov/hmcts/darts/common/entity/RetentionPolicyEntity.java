package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;


@Entity
@Table(name = "retention_policy")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class RetentionPolicyEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "rtp_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rtp_gen")
    @SequenceGenerator(name = "rtp_gen", sequenceName = "rtp_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "policy_name")
    private String name;

    @Column(name = "retention_period")
    private int retentionPeriod;

}
