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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;


@Entity
@Table(name = "report")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ReportEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "rep_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rep_gen")
    @SequenceGenerator(name = "rep_gen", sequenceName = "rep_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "report_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "name")
    private String name;

    @Column(name = "subject")
    private String subject;

    @Column(name = "report_text")
    private String text;

    @Column(name = "query")
    private String query;

    @Column(name = "recipients")
    private String recipients;

}
