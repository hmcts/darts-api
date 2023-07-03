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
@Table(name = "moj_object_directory_status")
@Data
public class ObjectDirectoryStatusEntity {

    @Id
    @Column(name = "moj_ods_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_ods_gen")
    @SequenceGenerator(name = "moj_ods_gen", sequenceName = "moj_ods_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "ods_description")
    private String description;

}
