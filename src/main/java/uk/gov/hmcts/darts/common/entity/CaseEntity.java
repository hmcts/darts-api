package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "moj_case")
@Data
@SuppressWarnings({"PMD.ShortClassName"})
public class CaseEntity {

    private static final String MAPPED_BY_THE_CASE = "theCase";

    @Id
    @Column(name = "moj_cas_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_cas_gen")
    @SequenceGenerator(name = "moj_cas_gen", sequenceName = "moj_cas_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "c_type")
    private String type;

    @Column(name = "c_case_id")
    private String caseNumber;

    @Column(name = "c_reporting_restrictions")
    private String reportingRestrictions;

    @Column(name = "c_closed")
    private Boolean closed;

    @Column(name = "c_interpreter_used")
    private Boolean interpreterUsed;

    @Column(name = "c_case_closed_ts")
    private OffsetDateTime caseClosedTimestamp;

    @Column(name = "c_defendant")
    private List<String> defendant = new ArrayList<>();

    @Column(name = "c_prosecutor")
    private List<String> prosecutor = new ArrayList<>();

    @Column(name = "c_defence")
    private List<String> defence = new ArrayList<>();

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTimestamp;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version")
    private Short version;

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private List<Transcription> theTranscriptions;

    @OneToMany(mappedBy = MAPPED_BY_THE_CASE)
    private List<Annotation> theAnnotations;

}
