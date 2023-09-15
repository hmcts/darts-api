package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "security_group")
@Getter
@Setter
public class SecurityGroupEntity {

    @Id
    @Column(name = "grp_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grp_gen")
    @SequenceGenerator(name = "grp_gen", sequenceName = "grp_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false, foreignKey = @ForeignKey(name = "security_group_role_fk"))
    private SecurityRoleEntity securityRoleId;

    @Column(name = "r_dm_group_s_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "group_name", nullable = false)
    @EqualsAndHashCode.Include
    private String groupName;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "description")
    private String description;

    @Column(name = "r_modify_date")
    private OffsetDateTime legacyModifyDate;

    @Column(name = "group_class")
    private String groupClass;

    @Column(name = "group_global_unique_id")
    private String groupGlobalUniqueId;

    @Column(name = "group_display_name")
    private String groupDisplayName;

    @ManyToMany
    @JoinTable(name = "security_group_courthouse_ae",
        joinColumns = {@JoinColumn(name = "grp_id")},
        inverseJoinColumns = {@JoinColumn(name = "cth_id")})
    private Set<CourthouseEntity> courthouseEntities = new LinkedHashSet<>();

}
