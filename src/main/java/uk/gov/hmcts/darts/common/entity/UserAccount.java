package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_account")
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class UserAccount {

    @Id
    @Column(name = "usr_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usr_gen")
    @SequenceGenerator(name = "usr_gen", sequenceName = "usr_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "dm_user_s_object_id", length = 16)
    private String dmObjectId;

    @Column(name = "user_name")
    private String username;

    @Column(name = "user_os_name")
    private String osName;

    @Column(name = "user_address")
    private String address;

    @Column(name = "user_privileges")
    private String privileges;

    @Column(name = "user_db_name")
    private String dbName;

    @Column(name = "description")
    private String userDescription;

    @Column(name = "user_state")
    private Integer state;

    @Column(name = "modify_ts")
    private OffsetDateTime modifyTime;

    @Column(name = "workflow_disabled", length = 32)
    private Integer workflowDisabled;

    @Column(name = "user_source")
    private String source;

    @Column(name = "user_ldap_cn")
    private String ldapCn;

    @Column(name = "user_global_unique_id")
    private String globalUniqueId;

    @Column(name = "user_login_name")
    private String loginName;

    @Column(name = "user_login_domain")
    private String loginDomain;

    @Column(name = "last_login_utc_time")
    private OffsetDateTime lastLoginUtcTime;
}
