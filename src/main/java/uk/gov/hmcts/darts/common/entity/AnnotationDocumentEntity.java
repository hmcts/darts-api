package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import uk.gov.hmcts.darts.common.entity.base.ModifiedBaseEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "annotation_document")
public class AnnotationDocumentEntity extends ModifiedBaseEntity {

    @Id
    @Column(name = "ado_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ado_gen")
    @SequenceGenerator(name = "ado_gen", sequenceName = "ado_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ann_id", nullable = false)
    private AnnotationEntity annotation;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private UserAccountEntity uploadedBy;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "uploaded_ts")
    private OffsetDateTime uploadedDateTime;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "content_object_id")
    private String contentObjectId;

    @Column(name = "clip_id")
    private String clipId;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTs;

    public List<CourtCaseEntity> associatedCourtCases() {
        var cases = annotation.getHearingList().stream().map(HearingEntity::getCourtCase);
        return io.vavr.collection.List.ofAll(cases).distinctBy(CourtCaseEntity::getId).toJavaList();
    }
}
