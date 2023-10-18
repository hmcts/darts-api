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


@Entity
@Table(name = "region")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class RegionEntity {

    @Id
    @Column(name = "reg_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reg_gen")
    @SequenceGenerator(name = "reg_gen", sequenceName = "reg_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "region_name")
    private String regionName;

}
