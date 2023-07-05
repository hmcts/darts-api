package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "moj_report")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ReportEntity extends VersionedEntity {

    @Id
    @Column(name = "moj_rep_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_rep_gen")
    @SequenceGenerator(name = "moj_rep_gen", sequenceName = "moj_rep_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "r_report_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_name")
    private String name;

    @Column(name = "c_subject")
    private String subject;

    @Column(name = "c_text")
    private String text;

    @Column(name = "c_query")
    private String query;

    @Column(name = "c_recipients")
    private String recipients;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

}
