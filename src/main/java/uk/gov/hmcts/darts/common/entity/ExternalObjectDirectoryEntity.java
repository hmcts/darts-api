package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_object_directory")
@Getter
@Setter
public class ExternalObjectDirectoryEntity extends CreatedModifiedBaseEntity implements ObjectDirectory {

    @Id
    @Column(name = "eod_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eod_gen")
    @SequenceGenerator(name = "eod_gen", sequenceName = "eod_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "med_id", foreignKey = @ForeignKey(name = "eod_media_fk"))
    private MediaEntity media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trd_id", foreignKey = @ForeignKey(name = "eod_transcription_document_fk"))
    private TranscriptionDocumentEntity transcriptionDocumentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ado_id", foreignKey = @ForeignKey(name = "eod_annotation_document_fk"))
    private AnnotationDocumentEntity annotationDocumentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cad_id", foreignKey = @ForeignKey(name = "eod_case_document_fk"))
    private CaseDocumentEntity caseDocument;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ors_id", foreignKey = @ForeignKey(name = "eod_object_record_status_fk"), nullable = false)
    private ObjectRecordStatusEntity status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elt_id", foreignKey = @ForeignKey(name = "eod_external_location_type_fk"), nullable = false)
    private ExternalLocationTypeEntity externalLocationType;

    @NaturalId(mutable = true)
    @Column(name = "external_location", unique = true)
    private UUID externalLocation;

    @Column(name = "external_file_id")
    private String externalFileId;

    @Column(name = "external_record_id")
    private String externalRecordId;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "transfer_attempts")
    private Integer transferAttempts;

    @Column(name = "verification_attempts")
    private Integer verificationAttempts;

    @Column(name = "data_ingestion_ts")
    private OffsetDateTime dataIngestionTs;

    @Column(name = "manifest_file")
    private String manifestFile;

    @Column(name = "event_date_ts")
    private OffsetDateTime eventDateTs;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "is_response_cleaned")
    private boolean responseCleaned;

    @Column(name = "osr_uuid")
    private Long osrUuid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "osr_uuid", updatable = false, insertable = false)
    private ObjectStateRecordEntity objectStateRecordEntity;

    @Column(name = "update_retention")
    private boolean updateRetention;

    @Column(name = "input_upload_processed_ts")
    private OffsetDateTime inputUploadProcessedTs;

    @Column(name = "force_response_cleanup")
    private Boolean forceResponseCleanup;

    @Column(name = "is_dets", nullable = false)
    private Boolean isDets = false;

    @Override
    public int getStatusId() {
        return getStatus().getId();
    }

    @Override
    public UUID getLocation() {
        return externalLocation;
    }

    public boolean isForLocationType(ExternalLocationTypeEnum type) {
        return getExternalLocationType().getId().equals(type.getId());
    }
}
