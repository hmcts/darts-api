package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "security_role")
@Getter
@Setter
public class SecurityRoleEntity {

    @Id
    @Column(name = "rol_id")
    private Integer id;

    @Column(name = "role_name")
    private String roleName;

    @ManyToMany
    @JoinTable(name = "security_role_permission_ae",
        joinColumns = {@JoinColumn(name = "rol_id")},
        inverseJoinColumns = {@JoinColumn(name = "per_id")})
    private List<SecurityPermissionEntity> securityPermissionEntities;

}
