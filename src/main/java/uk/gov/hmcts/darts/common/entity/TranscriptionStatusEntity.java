package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transcription_status")
@Getter
@Setter
public class TranscriptionStatusEntity {

    @Id
    @Column(name = "trs_id", nullable = false)
    private Integer id;

    @Column(name = "status_type", nullable = false)
    private String statusType;

    @Column(name = "display_name", nullable = false)
    private String displayName;
}
