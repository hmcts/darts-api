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
@Table(name = "media_type")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MediaTypeEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "met_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "met_gen")
    @SequenceGenerator(name = "met_gen", sequenceName = "med_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "media_type")
    private String legacyObjectId;
}
