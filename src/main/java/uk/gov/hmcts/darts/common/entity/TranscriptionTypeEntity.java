package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transcription_type")
@Getter
@Setter
public class TranscriptionTypeEntity {

    @Id
    @Column(name = "trt_id")
    private Integer id;

    @Column(name = "description")
    private String description;

    @Column(name = "display_state")
    private Boolean displayState;

}
