package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

@Entity
@Table(name = "transcription_type")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TranscriptionTypeEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "trt_id")
    private Integer id;

    @Column(name = "description")
    private String description;

}
