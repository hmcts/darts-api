package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

@Entity
@Table(name = "case_linked_case")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CaseLinkedCaseEntity extends CreatedModifiedBaseEntity implements HasIntegerId {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clc_seq")
    @SequenceGenerator(name = "clc_seq", sequenceName = "clc_seq", allocationSize = 1)
    @Column(name = "clc_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_1_id")
    private CourtCaseEntity courtCase1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_2_id")
    private CourtCaseEntity courtCase2;

}
