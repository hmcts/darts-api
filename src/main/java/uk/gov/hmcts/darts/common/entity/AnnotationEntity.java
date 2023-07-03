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
@Table(name = "moj_annotation")
@Data
@EqualsAndHashCode(callSuper = false)
public class AnnotationEntity extends VersionedEntity {

    @Id
    @Column(name = "moj_ann_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_ann_gen")
    @SequenceGenerator(name = "moj_ann_gen", sequenceName = "moj_ann_seq", allocationSize = 1)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "moj_cas_id")
    private CaseEntity courtCase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_crt_id")
    private CourthouseEntity courthouse;

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

    @Column(name = "c_courtroom")
    private String courtroom;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

}
