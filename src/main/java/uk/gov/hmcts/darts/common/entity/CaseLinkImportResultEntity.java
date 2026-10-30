package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

@Entity
@Table(name = "case_link_import_result")
@Getter
@Setter
public class CaseLinkImportResultEntity extends CreatedModifiedBaseEntity {

    private static final String TEXT_TYPE = "text";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clr_gen")
    @SequenceGenerator(name = "clr_gen", sequenceName = "clr_seq", allocationSize = 1)
    @Column(name = "clr_id")
    private Long id;

    @Column(name = "cli_id", nullable = false)
    private Long importId;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "case_1_case_number", columnDefinition = TEXT_TYPE)
    private String case1CaseNumber;

    @Column(name = "case_1_courthouse", columnDefinition = TEXT_TYPE)
    private String case1Courthouse;

    @Column(name = "case_2_case_number", columnDefinition = TEXT_TYPE)
    private String case2CaseNumber;

    @Column(name = "case_2_courthouse", columnDefinition = TEXT_TYPE)
    private String case2Courthouse;

    @Column(name = "column_count", nullable = false)
    private int columnCount;

    @Column(name = "case_1_id")
    private Integer case1Id;

    @Column(name = "case_2_id")
    private Integer case2Id;

    @Column(name = "case_1_created", nullable = false)
    private boolean case1Created;

    @Column(name = "case_2_created", nullable = false)
    private boolean case2Created;

    @Column(name = "case_1_deleted", nullable = false)
    private boolean case1Deleted;

    @Column(name = "case_2_deleted", nullable = false)
    private boolean case2Deleted;

    @Column(name = "case_1_anonymised", nullable = false)
    private boolean case1Anonymised;

    @Column(name = "case_2_anonymised", nullable = false)
    private boolean case2Anonymised;

    @Column(name = "clc_id")
    private Integer linkedCaseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crs_id", nullable = false)
    private CaseLinkImportResultStatusEntity status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = TEXT_TYPE)
    private String errorMessage;

    @Column(name = "warning_codes", columnDefinition = TEXT_TYPE)
    private String warningCodes;
}
