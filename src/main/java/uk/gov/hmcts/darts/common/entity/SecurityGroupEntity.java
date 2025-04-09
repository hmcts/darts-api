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
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "security_group")
@Getter
@Setter
@Audited
@AuditTable("security_group_aud")
public class SecurityGroupEntity extends MandatoryCreatedModifiedBaseEntity {

    @Id
    @Column(name = "grp_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "grp_gen")
    @SequenceGenerator(name = "grp_gen", sequenceName = "grp_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false, foreignKey = @ForeignKey(name = "security_group_role_fk"))
    private SecurityRoleEntity securityRoleEntity;

    @NotAudited
    @Column(name = "dm_group_s_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "group_name", nullable = false)
    @EqualsAndHashCode.Include
    private String groupName;

    @NotAudited
    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "description")
    private String description;

    @NotAudited
    @Column(name = "group_global_unique_id")
    private String groupGlobalUniqueId;

    @NotAudited
    @Column(name = "global_access")
    private Boolean globalAccess;

    @NotAudited
    @Column(name = "display_state")
    private Boolean displayState;

    @NotAudited
    @Column(name = "use_interpreter")
    private Boolean useInterpreter;

    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "security_group_courthouse_ae_aud")
    @ManyToMany
    @JoinTable(name = "security_group_courthouse_ae",
        joinColumns = {@JoinColumn(name = "grp_id")},
        inverseJoinColumns = {@JoinColumn(name = "cth_id")})
    private Set<CourthouseEntity> courthouseEntities = new LinkedHashSet<>();

    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "security_group_user_account_ae_aud")
    @ManyToMany(mappedBy = UserAccountEntity_.SECURITY_GROUP_ENTITIES)
    private Set<UserAccountEntity> users = new LinkedHashSet<>();

    @Column(name = "display_name")
    private String displayName;

    @NotAudited
    @Column(name = "group_display_name")
    private String legacyGroupDisplayName;

}