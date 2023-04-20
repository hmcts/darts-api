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
@Table(name = "moj_hearing")
@Data
public class Hearing {

    @Id
    @Column(name = "moj_hea_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "moj_cas_id")
    private Case theCase;

    @Column(name = "c_judge", length = 2000)
    private String judge;

    @Column(name = "c_defendant", length = 2000)
    private String defendant;

    @Column(name = "c_prosecutor", length = 2000)
    private String prosecutor;

    @Column(name = "c_defence", length = 2000)
    private String defence;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_hearing_date")
    private Date hearingDate;

    @Column(name = "c_judge_hearing_date", length = 2000)
    private String judgeHearingDate;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
