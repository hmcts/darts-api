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

@Entity
@Table(name = "event_linked_case")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class EventLinkedCaseEntity {

    @Id
    @Column(name = "elc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elc_gen")
    @SequenceGenerator(name = "elc_gen", sequenceName = "elc_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "eve_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @Column(name = "courthouse_name")
    private String courthouseName;

    @Column(name = "case_number")
    private String caseNumber;

}
