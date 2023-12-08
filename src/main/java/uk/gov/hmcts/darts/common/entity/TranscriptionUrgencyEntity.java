package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "transcription_urgency")
@Data
public class TranscriptionUrgencyEntity {

    @Id
    @Column(name = "tru_id")
    private Integer id;

    @Column(name = "description")
    private String description;

    @Column(name = "display_state")
    private Boolean displayState;

    @Column(name = "priority_order", nullable = false)
    private int priorityOrder;

}
