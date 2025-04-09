package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = "security_permission")
@Getter
@Setter
public class SecurityPermissionEntity {

    @Id
    @Column(name = "per_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "per_gen")
    @SequenceGenerator(name = "per_gen", sequenceName = "per_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "permission_name", nullable = false)
    @EqualsAndHashCode.Include
    private String permissionName;

}
