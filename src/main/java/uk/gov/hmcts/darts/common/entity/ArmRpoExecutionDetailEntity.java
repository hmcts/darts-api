package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

@Entity
@Table(name = "arm_rpo_execution_detail")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ArmRpoExecutionDetailEntity extends MandatoryCreatedModifiedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ard_seq")
    @SequenceGenerator(name = "ard_seq", sequenceName = "ard_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    @Column(name = "ard_id", nullable = false)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "are_id")
    private ArmRpoStateEntity armRpoState;

    @ManyToOne
    @JoinColumn(name = "aru_id")
    private ArmRpoStatusEntity armRpoStatus;

    @Column(name = "matter_id")
    private String matterId;

    @Column(name = "index_id")
    private String indexId;

    @Column(name = "entitlement_id")
    private String entitlementId;

    @Column(name = "storage_account_id")
    private String storageAccountId;

    @Column(name = "search_id")
    private String searchId;

    @Column(name = "production_id")
    private String productionId;

    @Column(name = "sorting_field")
    private String sortingField;

    @Column(name = "search_item_count")
    private Integer searchItemCount;

    @Column(name = "polling_created_ts")
    private OffsetDateTime pollingCreatedAt;

    @Column(name = "production_name")
    private String productionName;

}
