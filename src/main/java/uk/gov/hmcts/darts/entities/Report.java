package uk.gov.hmcts.darts.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "moj_report")
@Data
public class Report {

    @Id
    @Column(name = "moj_rep_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "r_report_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_name", length = 32)
    private String name;

    @Column(name = "c_subject", length = 256)
    private String subject;

    @Column(name = "c_text", length = 1024)
    private String text;

    @Column(name = "c_query", length = 2048)
    private String query;

    @Column(name = "c_recipients", length = 1024)
    private String recipients;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
