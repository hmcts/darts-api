package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transcription_urgency")
@Getter
@Setter
public class TranscriptionUrgencyEntity {

    @Id
    @Column(name = "tru_id")
    private Integer id;

    @Column(name = "description")
    private String description;

    @Column(name = "display_state")
    private Boolean displayState;

}
