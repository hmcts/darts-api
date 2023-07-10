package uk.gov.hmcts.darts.common.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.CascadeType;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "court_case")
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class CaseEntity {

    @Id
    @Column(name = "cas_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cas_gen")
    @SequenceGenerator(name = "cas_gen", sequenceName = "cas_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "rer_id")
    private ReportingRestrictionsEntity reportingRestrictions;

    @Column(name = "case_object_id", length = 16)
    private String legacyCaseObjectId;

    @Column(name = "case_number")
    private String caseNumber;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "cth_id")
    private CourthouseEntity courthouse;

    @Column(name = "case_closed")
    private Boolean closed;

    @Column(name = "interpreter_used")
    private Boolean interpreterUsed;

    @Column(name = "case_closed_ts")
    private OffsetDateTime caseClosedTimestamp;

    @Type(ListArrayType.class)
    @Column(name = "defendant_list")
    private List<String> defendants = new ArrayList<>();

    @Type(ListArrayType.class)
    @Column(name = "prosecutor_list")
    private List<String> prosecutors = new ArrayList<>();

    @Type(ListArrayType.class)
    @Column(name = "defence_list")
    private List<String> defenders = new ArrayList<>();

    @Column(name = "retain_until_ts")
    private OffsetDateTime retainUntilTimestamp;

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @OneToMany(mappedBy = "courtCase")
    private List<TranscriptionEntity> transcriptions;

    @OneToMany(mappedBy = "courtCase")
    private List<AnnotationEntity> annotations;

    @OneToMany(mappedBy = "courtCase")
    private List<HearingEntity> hearings = new ArrayList<>();

    public void addHearing(HearingEntity hearing) {
        this.hearings.add(hearing);
    }
}
