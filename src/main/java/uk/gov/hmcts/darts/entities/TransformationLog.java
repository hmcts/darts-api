package uk.gov.hmcts.darts.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "moj_transformation_log")
@Data
public class TransformationLog {

    @Id
    @Column(name = "moj_trl_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "moj_cas_id")
    private Case theCase;

    @Column(name = "r_transformation_log_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_case_id", length = 32)
    private String caseId;

    @Column(name = "c_courthouse", length = 64)
    private String courthouse;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_requested_date")
    private Date requestedDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_received_date")
    private Date receivedDate;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
