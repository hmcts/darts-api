package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Entity
@Table(name = "annotation")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationEntity extends VersionedEntity {

    @Id
    @Column(name = "ann_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ann_gen")
    @SequenceGenerator(name = "ann_gen", sequenceName = "ann_seq", allocationSize = 1)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "cas_id")
    private CaseEntity courtCase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ctr_id")
    private CourtroomEntity courtroom;

    @Column(name = "r_annotation_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_text")
    private String text;

    @Column(name = "c_time_stamp")
    private OffsetDateTime timestamp;

    @Column(name = "c_start")
    private OffsetDateTime start;

    @Column(name = "c_end")
    private OffsetDateTime end;

    @Column(name = "c_case_id")
    private String legacyCaseId;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

}
