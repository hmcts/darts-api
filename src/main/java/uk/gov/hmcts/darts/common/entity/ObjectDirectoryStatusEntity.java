package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "object_directory_status")
@Data
public class ObjectDirectoryStatusEntity {

    @Id
    @Column(name = "ods_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ods_gen")
    @SequenceGenerator(name = "ods_gen", sequenceName = "ods_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "ods_description")
    private String description;

}
