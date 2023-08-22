package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_account")
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class UserAccountEntity implements JpaAuditing {

    @Id
    @Column(name = "usr_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usr_gen")
    @SequenceGenerator(name = "usr_gen", sequenceName = "usr_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "dm_user_s_object_id", length = 16)
    private String dmObjectId;

    @Column(name = "user_name")
    private String username;

    @Column(name = "user_email_address")
    private String emailAddress;

    @Column(name = "description")
    private String userDescription;

    @Column(name = "user_state")
    private Integer state;

    @Column(name = "last_login_ts")
    private OffsetDateTime lastLoginTime;

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccountEntity createdBy;

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime modifiedTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    private UserAccountEntity modifiedBy;

    @ManyToMany
    @JoinTable(name = "security_group_user_account_ae",
        joinColumns = {@JoinColumn(name = "usr_id")},
        inverseJoinColumns = {@JoinColumn(name = "grp_id")})
    private List<SecurityGroupEntity> securityGroupEntities = new ArrayList<>();

}
