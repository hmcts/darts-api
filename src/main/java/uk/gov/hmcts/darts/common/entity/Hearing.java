package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "moj_hearing")
@Data
public class Hearing {

    @Id
    @Column(name = "moj_hea_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_crt_id")
    private Courthouse theCourthouse;

    @Column(name = "c_judge")
    private List<String> judge;

    @Column(name = "c_defendant")
    private List<String> defendant;

    @Column(name = "c_prosecutor")
    private List<String> prosecutor;

    @Column(name = "c_defence")
    private List<String> defence;

    @Column(name = "c_hearing_date")
    private OffsetDateTime hearingDate;

    @Column(name = "c_judge_hearing_date")
    private String judgeHearingDate;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
