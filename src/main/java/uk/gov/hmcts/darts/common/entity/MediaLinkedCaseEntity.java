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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_linked_case")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MediaLinkedCaseEntity {

    @Id
    @Column(name = "mlc_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mlc_gen")
    @SequenceGenerator(name = "mlc_gen", sequenceName = "mlc_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "med_id")
    private MediaEntity media;

    @ManyToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @Column(name = "courthouse_name")
    private String courthouseName;

    @Column(name = "case_number")
    private String caseNumber;

    public MediaLinkedCaseEntity(MediaEntity mediaEntity, CourtCaseEntity courtCase) {
        this.media = mediaEntity;
        this.courtCase = courtCase;
    }
}
