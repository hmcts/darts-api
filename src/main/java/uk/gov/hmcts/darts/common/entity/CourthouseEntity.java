package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courthouse")
@Getter
@Setter
public class CourthouseEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "cth_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cth_gen")
    @SequenceGenerator(name = "cth_gen", sequenceName = "cth_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "courthouse_code")
    @EqualsAndHashCode.Include
    private Integer code;

    @Column(name = "courthouse_name", unique = true)
    @EqualsAndHashCode.Include
    private String courthouseName;

    @OneToMany(mappedBy = "courthouse")
    private List<CourtroomEntity> courtrooms;

    @ManyToMany
    @JoinTable(name = "security_group_courthouse_ae",
        joinColumns = {@JoinColumn(name = "cth_id")},
        inverseJoinColumns = {@JoinColumn(name = "grp_id")})
    private Set<SecurityGroupEntity> securityGroups = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "courthouse_region_ae",
        joinColumns = {@JoinColumn(name = "cth_id")},
        inverseJoinColumns = {@JoinColumn(name = "reg_id")})
    private Set<RegionEntity> regions = new LinkedHashSet<>();

    public Set<RegionEntity> getRegions() throws IllegalStateException {
        try {
            if (regions.isEmpty()) {
                regions = null;
            }

            if (regions != null && regions.size() > 1) {
                throw new IllegalStateException();
            }

        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }
        return regions;
    }

    public void setRegions(Set<RegionEntity> regions) throws IllegalStateException {
        try {
            this.regions = regions;

            if (regions == null) {
                regions = new LinkedHashSet<>();
            }

            if (regions.size() > 1) {
                throw new IllegalStateException();
            }

        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }
    }


    @Column(name = "display_name")
    private String displayName;

}
