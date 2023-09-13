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

@Entity
@Table(name = "transcription_status")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TranscriptionStatusEntity {

    @Id
    @Column(name = "trs_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trs_gen")
    @SequenceGenerator(name = "trs_gen", sequenceName = "trs_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "status_type", nullable = false)
    private String statusType;
}
