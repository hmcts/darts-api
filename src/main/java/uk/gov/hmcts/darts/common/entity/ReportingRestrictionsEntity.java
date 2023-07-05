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

@Entity
@Table(name = "moj_reporting_restrictions")
@Getter
@Setter
public class ReportingRestrictionsEntity {

    @Id
    @Column(name = "moj_rer_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_rer_gen")
    @SequenceGenerator(name = "moj_rer_gen", sequenceName = "moj_rer_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "rer_description")
    private Integer description;

}
