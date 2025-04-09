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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = "transcription_linked_case")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TranscriptionLinkedCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tlc_gen")
    @SequenceGenerator(name = "tlc_gen", sequenceName = "tlc_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    @Column(name = "tlc_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tra_id", nullable = false)
    private TranscriptionEntity transcription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @Column(name = "courthouse_name")
    private String courthouseName;

    @Column(name = "case_number")
    private String caseNumber;

}
