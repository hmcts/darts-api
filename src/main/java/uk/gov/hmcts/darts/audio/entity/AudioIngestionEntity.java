package uk.gov.hmcts.darts.audio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.audio.enums.AudioIngestionStatus;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

import java.time.OffsetDateTime;

@Entity
@Table(name = AudioIngestionEntity.TABLE_NAME)
@Getter
@Setter
public class AudioIngestionEntity implements HasIntegerId {

    public static final String TABLE_NAME = "audio_ingestion";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "source_full_filename", nullable = false)
    private String sourceFullFilename;

    @Column(name = "source_last_modified_ts", nullable = false)
    private OffsetDateTime sourceLastModifiedTs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AudioIngestionStatus status;

    @Column(name = "claim_owner")
    private String claimOwner;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "error")
    private String error;

    @Column(name = "created_ts", nullable = false)
    private OffsetDateTime createdTs;
}