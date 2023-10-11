package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

@Entity
@Table(name = "transcription_urgency")
@Getter
@Setter
public class TranscriptionUrgencyEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "tru_id")
    private Integer id;

    @Column(name = "description")
    private String description;

}
