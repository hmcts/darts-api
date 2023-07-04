package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "media")
@Data
@EqualsAndHashCode(callSuper = false)
public class MediaEntity extends VersionedEntity {

    @Id
    @Column(name = "med_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "med_gen")
    @SequenceGenerator(name = "med_gen", sequenceName = "med_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ctr_id", nullable = false)
    private CourtroomEntity courtroom;

    @Column(name = "r_media_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_channel")
    private Integer channel;

    @Column(name = "c_total_channels")
    private Integer totalChannels;

    @Column(name = "c_reference_id")
    private String referenceId;

    @Column(name = "c_start")
    private OffsetDateTime start;

    @Column(name = "c_end")
    private OffsetDateTime end;

    @Column(name = "c_case_id")
    private List<String> legacyCaseId;

    @Column(name = "r_case_object_id")
    private List<String> legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

}
