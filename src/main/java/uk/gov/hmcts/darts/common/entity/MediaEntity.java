package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moj_media")
@Data
public class MediaEntity {

    @Id
    @Column(name = "moj_med_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_med_gen")
    @SequenceGenerator(name = "moj_med_gen", sequenceName = "moj_med_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "moj_crt_id")
    private Integer courthouseId;

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

    @Column(name = "c_courtroom")
    private String courtroom;

    @Column(name = "c_case_id")
    private String caseId;

    @Column(name = "r_case_object_id")
    private String caseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version")
    private Short version;

}
