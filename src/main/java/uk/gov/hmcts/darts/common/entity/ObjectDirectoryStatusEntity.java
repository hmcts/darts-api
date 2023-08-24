package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

@Entity
@Table(name = "object_directory_status")
@Data
@EqualsAndHashCode(callSuper = true)
public class ObjectDirectoryStatusEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "ods_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ods_gen")
    @SequenceGenerator(name = "ods_gen", sequenceName = "ods_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "ods_description")
    private String description;

}
