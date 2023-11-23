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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.UUID;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "external_object_directory")
@Getter
@Setter
public class ExternalObjectDirectoryEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "eod_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eod_gen")
    @SequenceGenerator(name = "eod_gen", sequenceName = "eod_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})
    @JoinColumn(name = "med_id", foreignKey = @ForeignKey(name = "eod_media_fk"))
    private MediaEntity media;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})
    @JoinColumn(name = "trd_id", foreignKey = @ForeignKey(name = "eod_transcription_document_fk"))
    private TranscriptionDocumentEntity transcriptionDocumentEntity;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {PERSIST, MERGE})
    @JoinColumn(name = "ado_id", foreignKey = @ForeignKey(name = "eod_annotation_document_fk"))
    private AnnotationDocumentEntity annotationDocumentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ods_id", foreignKey = @ForeignKey(name = "eod_object_directory_status_fk"), nullable = false)
    private ObjectDirectoryStatusEntity status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elt_id", foreignKey = @ForeignKey(name = "eod_external_location_type_fk"), nullable = false)
    private ExternalLocationTypeEntity externalLocationType;

    @NaturalId(mutable = true)
    @Column(name = "external_location", unique = true, nullable = false)
    private UUID externalLocation;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "transfer_attempts")
    private Integer transferAttempts;

}
