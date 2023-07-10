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
@Table(name = "reporting_restrictions")
@Getter
@Setter
public class ReportingRestrictionsEntity {

    @Id
    @Column(name = "rer_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rer_gen")
    @SequenceGenerator(name = "rer_gen", sequenceName = "rer_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "rer_description")
    private Integer description;

}
