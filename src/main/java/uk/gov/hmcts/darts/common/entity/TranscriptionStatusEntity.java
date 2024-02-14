package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcription_status")
@Getter
@NoArgsConstructor
@Setter
public class TranscriptionStatusEntity {

    public TranscriptionStatusEntity(Integer id) {
        this.setId(id);
    }

    @Id
    @Column(name = "trs_id", nullable = false)
    private Integer id;

    @Column(name = "status_type", nullable = false)
    private String statusType;

    @Column(name = "display_name", nullable = false)
    private String displayName;
}
