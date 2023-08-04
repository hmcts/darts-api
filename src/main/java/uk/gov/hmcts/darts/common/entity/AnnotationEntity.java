package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "annotation")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AnnotationEntity extends VersionedEntity {

    @Id
    @Column(name = "ann_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ann_gen")
    @SequenceGenerator(name = "ann_gen", sequenceName = "ann_seq", allocationSize = 1)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "cas_id")
    private CourtCaseEntity courtCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ctr_id")
    private CourtroomEntity courtroom;

    @Column(name = "annotation_text")
    private String text;

    @Column(name = "annotation_ts")
    private OffsetDateTime timestamp;

    @Column(name = "annotation_object_id")
    private String legacyObjectId;

    @Column(name = "version_label")
    private String legacyVersionLabel;

    @Column(name = "superseded")
    private Boolean superseded;
}
