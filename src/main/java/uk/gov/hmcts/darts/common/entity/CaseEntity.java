package uk.gov.hmcts.darts.common.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "moj_case")
@Data
@SuppressWarnings({"PMD.ShortClassName"})
public class CaseEntity {

    @Id
    @Column(name = "moj_cas_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_cas_gen")
    @SequenceGenerator(name = "moj_cas_gen", sequenceName = "moj_cas_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "moj_rer_id")
    @Cascade(CascadeType.SAVE_UPDATE)
    private ReportingRestrictionsEntity reportingRestrictions;

    @Column(name = "r_case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "c_case_id")
    private String caseNumber;

    @Column(name = "c_closed")
    private Boolean closed;

    @Column(name = "c_interpreter_used")
    private Boolean interpreterUsed;

    @Column(name = "c_case_closed_ts")
    private OffsetDateTime caseClosedTimestamp;

    @Type(ListArrayType.class)
    @Column(name = "c_defendant")
    private List<String> defendants = new ArrayList<>();

    @Type(ListArrayType.class)
    @Column(name = "c_prosecutor")
    private List<String> prosecutors = new ArrayList<>();

    @Type(ListArrayType.class)
    @Column(name = "c_defence")
    private List<String> defenders = new ArrayList<>();

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTimestamp;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @OneToMany(mappedBy = "courtCase")
    private List<TranscriptionEntity> transcriptions;

    @OneToMany(mappedBy = "courtCase")
    private List<AnnotationEntity> annotations;
}
