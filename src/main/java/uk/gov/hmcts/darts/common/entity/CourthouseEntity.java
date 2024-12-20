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
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "courthouse")
@Getter
@Setter
@Audited
@AuditTable("courthouse_aud")
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

    @NotAudited
    @OneToMany(mappedBy = "courthouse")
    private List<CourtroomEntity> courtrooms;

    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "security_group_courthouse_ae_aud")
    @ManyToMany
    @JoinTable(name = "security_group_courthouse_ae",
        joinColumns = {@JoinColumn(name = "cth_id")},
        inverseJoinColumns = {@JoinColumn(name = "grp_id")})
    private Set<SecurityGroupEntity> securityGroups = new LinkedHashSet<>();

    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "courthouse_region_ae_aud")
    @ManyToMany
    @JoinTable(name = "courthouse_region_ae",
        joinColumns = {@JoinColumn(name = "cth_id")},
        inverseJoinColumns = {@JoinColumn(name = "reg_id")})
    private Set<RegionEntity> regions = new LinkedHashSet<>();

    @Column(name = "display_name")
    private String displayName;

    @NotAudited
    @Column(name = "courthouse_object_id", length = 16)
    private String courthouseObjectId;

    @NotAudited
    @Column(name = "folder_path")
    private String folderPath;

    public RegionEntity getRegion() {
        throwIfStateBad();

        if (CollectionUtils.isEmpty(regions)) {
            return null;
        }

        return regions.stream().findFirst().get();
    }

    public void setRegion(RegionEntity region) {
        throwIfStateBad();

        regions = new LinkedHashSet<>();
        if (region != null) {
            regions = new LinkedHashSet<>();
            regions.add(region);
            region.getCourthouses().add(this);
        }
    }

    private void throwIfStateBad() {
        if (regions != null && regions.size() > 1) {
            throw new IllegalStateException();
        }
    }

    public void setCourthouseName(String courthouseName) {
        this.courthouseName = courthouseName.toUpperCase(Locale.ROOT);
    }
}
