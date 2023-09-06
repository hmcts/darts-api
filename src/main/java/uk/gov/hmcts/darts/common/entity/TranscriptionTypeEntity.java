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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

@Entity
@Table(name = "transcription_type")
@Getter
@Setter
public class TranscriptionTypeEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "trt_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trt_gen")
    @SequenceGenerator(name = "trt_gen", sequenceName = "trt_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "description")
    private String description;
}
