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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.util.DataUtil;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "security_role")
@Getter
@Setter
public class SecurityRoleEntity {

    @Id
    @Column(name = "rol_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rol_gen")
    @SequenceGenerator(name = "rol_gen", sequenceName = "rol_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "role_name", nullable = false)
    @EqualsAndHashCode.Include
    private String roleName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "display_state")
    private Boolean displayState;

    @ManyToMany
    @JoinTable(name = "security_role_permission_ae",
        joinColumns = {@JoinColumn(name = "rol_id")},
        inverseJoinColumns = {@JoinColumn(name = "per_id")})
    private Set<SecurityPermissionEntity> securityPermissionEntities = new LinkedHashSet<>();

}
