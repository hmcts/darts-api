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
import uk.gov.hmcts.darts.common.entity.base.ModifiedBaseEntity;

import java.time.OffsetDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ohr_id")
    private ObjectHiddenReasonEntity objectHiddenReason;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private UserAccountEntity uploadedBy;

    @CreationTimestamp
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

    @Column(name = "marked_for_manual_deletion", nullable = false)
    private boolean markedForManualDeletion;

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTs;
}
