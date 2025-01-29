package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "retention_policy_type_heritage_mapping")
public class RetentionPolicyTypeHeritageMappingEntity {

    @Id
    @Column(name = "rhm_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rhm_gen")
    @SequenceGenerator(name = "rhm_gen", sequenceName = "rhm_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "heritage_policy_name", nullable = false)
    private String heritagePolicyName;

    @Column(name = "heritage_table", nullable = false)
    private String heritageTable;

    @Column(name = "modernised_rpt_id")
    private Integer modernisedRptId;

}
