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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "security_group")
@Getter
@Setter
public class SecurityGroupEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "grp_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grp_gen")
    @SequenceGenerator(name = "grp_gen", sequenceName = "grp_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false, foreignKey = @ForeignKey(name = "security_group_role_fk"))
    private SecurityRoleEntity securityRoleEntity;

    @Column(name = "dm_group_s_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "group_name", nullable = false)
    @EqualsAndHashCode.Include
    private String groupName;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "description")
    private String description;

    @Column(name = "group_global_unique_id")
    private String groupGlobalUniqueId;

    @Column(name = "global_access")
    private Boolean globalAccess;

    @Column(name = "display_state")
    private Boolean displayState;

    @Column(name = "use_interpreter")
    private Boolean useInterpreter;

    @ManyToMany
    @JoinTable(name = "security_group_courthouse_ae",
          joinColumns = {@JoinColumn(name = "grp_id")},
          inverseJoinColumns = {@JoinColumn(name = "cth_id")})
    private Set<CourthouseEntity> courthouseEntities = new LinkedHashSet<>();

    @ManyToMany(mappedBy = UserAccountEntity_.SECURITY_GROUP_ENTITIES)
    private Set<UserAccountEntity> users = new LinkedHashSet<>();

    @Column(name = "display_name")
    private String displayName;

}
