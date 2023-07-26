package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "security_permission")
@Getter
@Setter
public class SecurityPermissionEntity {

    @Id
    @Column(name = "per_id")
    private Integer id;

    @Column(name = "permission_name")
    private String permissionName;

}
