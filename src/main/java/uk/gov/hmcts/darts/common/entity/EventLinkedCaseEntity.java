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

    @ManyToOne
    @JoinColumn(name = "eve_id")
    private EventEntity event;

    @ManyToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @Column(name = "courthouse_name")
    @Deprecated(forRemoval = true)
    /**
     * Use {@link CourtCaseEntity#getCourthouseName()} instead
     */
    private String courthouseName;

    @Column(name = "case_number")
    @Deprecated(forRemoval = true)
    /**
     * Use {@link CourtCaseEntity#getCaseNumber()} instead
     */
    private String caseNumber;

    @EqualsAndHashCode.Include(replaces = "event")
    public Integer getEventId() {
        return event != null ? event.getId() : null;
    }

}
