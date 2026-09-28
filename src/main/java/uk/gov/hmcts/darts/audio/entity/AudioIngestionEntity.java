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
import org.hibernate.envers.AuditTable;
import uk.gov.hmcts.darts.audio.enums.AudioIngestionStatus;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;

import java.time.OffsetDateTime;

@Entity
@Table(name = AudioIngestionEntity.TABLE_NAME)
@Getter
@Setter
@AuditTable("audio_ingestion")
public class AudioIngestionEntity extends CreatedModifiedBaseEntity implements HasIntegerId {

    public static final String ID_COLUMN_NAME = "id";
    public static final String SOURCE_FULL_FILE_NAME = "source_full_filename";
    public static final String SOURCE_LAST_MODIFIED_TS = "source_last_modified_ts";
    public static final String STATUS = "status";
    public static final String CLAIM_OWNER = "claim_owner";
    public static final String CLAIMED_AT = "claimed_at";
    public static final String ERROR = "error";
    public static final String CREATED_TS = "created_ts";
    public static final String TABLE_NAME = "audio_ingestion";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = ID_COLUMN_NAME)
    private Integer id;

    @Column(name = SOURCE_FULL_FILE_NAME, nullable = false, unique = true)
    private String sourceFullFilename;

    @Column(name = SOURCE_LAST_MODIFIED_TS, nullable = false)
    private OffsetDateTime sourceLastModifiedTs;

    @Enumerated(EnumType.STRING)
    @Column(name = STATUS, nullable = false)
    private AudioIngestionStatus status;

    @Column(name = CLAIM_OWNER)
    private String claimOwner;

    @Column(name = CLAIMED_AT)
    private OffsetDateTime claimedAt;

    @Column(name = ERROR)
    private String error;
}