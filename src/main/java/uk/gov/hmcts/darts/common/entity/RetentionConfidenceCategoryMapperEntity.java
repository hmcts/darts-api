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
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

@Entity
@Table(name = "retention_confidence_category_mapper")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class RetentionConfidenceCategoryMapperEntity extends MandatoryCreatedModifiedBaseEntity {

    @Id
    @Column(name = "rcc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rcc_gen")
    @SequenceGenerator(name = "rcc_gen", sequenceName = "rcc_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "ret_conf_score")
    private RetentionConfidenceScoreEnum confidenceScore;

    @Column(name = "ret_conf_reason")
    private RetentionConfidenceReasonEnum confidenceReason;

    @Column(name = "confidence_category")
    private RetentionConfidenceCategoryEnum confidenceCategory;

    @Column(name = "description")
    private String description;

}
