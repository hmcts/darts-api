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
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

@Entity
@Table(name = RetentionPolicyTypeEntity.TABLE_NAME)
@Getter
@Setter
@Audited
@AuditTable("retention_policy_type_aud")
public class RetentionPolicyTypeEntity extends CreatedModifiedBaseEntity {
    public static final String ID = "rpt_id";
    public static final String TABLE_NAME = "retention_policy_type";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rpt_gen")
    @SequenceGenerator(name = "rpt_gen", sequenceName = "rpt_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "fixed_policy_key", nullable = false)
    private String fixedPolicyKey;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "duration", nullable = false)
    private String duration;

    @Column(name = "policy_start_ts", nullable = false)
    private OffsetDateTime policyStart;

    @Column(name = "policy_end_ts")
    private OffsetDateTime policyEnd;

    @Column(name = "description", nullable = false)
    private String description;

    @NotAudited
    @Column(name = "retention_policy_object_id")
    private String retentionPolicyObjectId;
}
