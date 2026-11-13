package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcription_hub_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionHubRequestEntity {

    @Id
    @Column(name = "tra_id")
    private Long transcriptionId;

}
