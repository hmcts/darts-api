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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = "annotation")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AnnotationEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "ann_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ann_gen")
    @SequenceGenerator(name = "ann_gen", sequenceName = "ann_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "annotation_text")
    private String text;

    @Column(name = "annotation_ts")
    private OffsetDateTime timestamp;

    @Column(name = "annotation_object_id")
    private String legacyObjectId;

    @Column(name = "version_label")
    private String legacyVersionLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_owner", nullable = false)
    private UserAccountEntity currentOwner;

    @Column(name = "is_deleted")
    private boolean deleted;
}
