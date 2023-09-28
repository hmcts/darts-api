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
@Table(name = "transcription_status")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TranscriptionStatusEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "trs_id", nullable = false)
    private Integer id;

    @Column(name = "status_type", nullable = false)
    private String statusType;

}
