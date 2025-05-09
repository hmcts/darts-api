package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.springframework.data.annotation.Immutable;
import uk.gov.hmcts.darts.common.entity.compositeid.UserRoleCourthouseId;

@Entity
@Table(name = "user_roles_courthouses")
@Getter
@Immutable
@IdClass(UserRoleCourthouseId.class)
public class UserRolesCourthousesEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cth_id")
    private CourthouseEntity courthouse;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_id")
    private UserAccountEntity userAccount;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private SecurityRoleEntity securityRole;

}
