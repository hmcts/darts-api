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
@Table(name = "object_record_status")
@Data
public class ObjectRecordStatusEntity {

    @Id
    @Column(name = "ors_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ors_gen")
    @SequenceGenerator(name = "ors_gen", sequenceName = "ors_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "ors_description")
    private String description;

}
