package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;


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

    @ManyToMany
    @JoinTable(name = "courthouse_region_ae",
        joinColumns = {@JoinColumn(name = "cth_id")},
        inverseJoinColumns = {@JoinColumn(name = "reg_id")})
    private Set<CourthouseEntity> courthouseEntities = new LinkedHashSet<>();

}
