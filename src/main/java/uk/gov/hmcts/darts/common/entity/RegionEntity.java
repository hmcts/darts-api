package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.util.DataUtil;

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
    @SequenceGenerator(name = "reg_gen", sequenceName = "reg_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "region_name")
    private String regionName;

    @ManyToMany(mappedBy = CourthouseEntity_.REGIONS)
    private Set<CourthouseEntity> courthouses = new LinkedHashSet<>();

}
