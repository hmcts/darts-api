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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@Audited
@AuditTable("user_account_aud")
public class UserAccountEntity extends CreatedModifiedBaseEntity
    implements HasIntegerId {

    @Id
    @Column(name = "usr_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usr_gen")
    @SequenceGenerator(name = "usr_gen", sequenceName = "usr_seq", allocationSize = 1)
    private Integer id;

    @NotAudited
    @Column(name = "dm_user_s_object_id", length = 16)
    private String dmObjectId;

    @NotAudited
    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_full_name", nullable = false)
    private String userFullName;

    @Column(name = "user_email_address")
    private String emailAddress;

    @Column(name = "description")
    private String userDescription;

    @Getter(AccessLevel.NONE)
    @Column(name = "is_active")
    private Boolean active;

    @NotAudited
    @Column(name = "last_login_ts")
    private OffsetDateTime lastLoginTime;

    @NotAudited
    @Column(name = "account_guid")
    private String accountGuid;

    @NotAudited
    @Column(name = "is_system_user", nullable = false)
    private Boolean isSystemUser;

    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "security_group_user_account_ae_aud")
    @ManyToMany
    @JoinTable(name = "security_group_user_account_ae",
        joinColumns = {@JoinColumn(name = "usr_id")},
        inverseJoinColumns = {@JoinColumn(name = "grp_id")})
    private Set<SecurityGroupEntity> securityGroupEntities = new LinkedHashSet<>();

    @NotAudited
    @Column(name = "user_os_name")
    private String userOsName;

    @NotAudited
    @Column(name = "user_ldap_dn")
    private String userLdapDomainName;

    @NotAudited
    @Column(name = "user_global_unique_id")
    private String userGlobalUniqueId;

    @NotAudited
    @Column(name = "user_login_name")
    private String userLoginName;

    @NotAudited
    @Column(name = "user_login_domain")
    private String userLoginDomain;

    @NotAudited
    @Column(name = "user_state")
    private Short userState;



    public Boolean isActive() {
        return active;
    }

    public boolean isInGroup(List<SecurityGroupEnum> securityGroupEnum) {
        return this.getSecurityGroupEntities().stream()
            .anyMatch(group -> securityGroupEnum.stream()
                .anyMatch(enumGroup -> enumGroup.name().equalsIgnoreCase(group.getGroupName())));
    }

}
