package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moj_transformation_log")
@Data
public class TransformationLog {

    @Id
    @Column(name = "moj_trl_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_trl_gen")
    @SequenceGenerator(name = "moj_trl_gen", sequenceName = "moj_trl_seq", allocationSize = 1)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "moj_cas_id")
    private Case theCase;

    @Column(name = "moj_crt_id", nullable = false)
    private Integer courthouseId;

    @Column(name = "r_transformation_log_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_case_id")
    private String caseId;

    @Column(name = "c_requested_date")
    private OffsetDateTime requestedDate;

    @Column(name = "c_received_date")
    private OffsetDateTime receivedDate;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version")
    private Short version;

}
