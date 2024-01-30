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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transcription_document")
@Getter
@Setter
public class TranscriptionDocumentEntity {

    @Id
    @Column(name = "trd_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trd_gen")
    @SequenceGenerator(name = "trd_gen", sequenceName = "trd_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tra_id", nullable = false, foreignKey = @ForeignKey(name = "transcription_document_transcription_fk"))
    private TranscriptionEntity transcription;

    @Column(name = "clip_id")
    private String clipId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private UserAccountEntity uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_ts", nullable = false)
    private OffsetDateTime uploadedDateTime;

    @OneToMany(mappedBy = ExternalObjectDirectoryEntity_.TRANSCRIPTION_DOCUMENT_ENTITY)
    private List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = new ArrayList<>();

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "content_object_id")
    private String contentObjectId;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;


}
