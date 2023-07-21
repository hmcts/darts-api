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
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(name = "user_email_address")
    private String emailAddress;

    @Column(name = "description")
    private String userDescription;

    @Column(name = "user_state")
    private Integer state;

    @Column(name = "created_ts")
    @CreationTimestamp
    private OffsetDateTime created;

    @Column(name = "last_modified_ts")
    private OffsetDateTime lastUpdated;

    @Column(name = "last_login_ts")
    private OffsetDateTime lastLoginTime;

    @Column(name = "last_modified_by")
    private Integer lastModifiedBy;
}
