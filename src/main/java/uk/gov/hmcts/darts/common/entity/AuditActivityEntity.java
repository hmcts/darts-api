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
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = "audit_activity")
@Getter
@Setter
public class AuditActivityEntity extends CreatedModifiedBaseEntity {
    @Id
    @Column(name = "aua_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aua_gen")
    @SequenceGenerator(name = "aua_gen", sequenceName = "aua_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "activity_name")
    private String name;

    @Column(name = "activity_description")
    private String description;
}
