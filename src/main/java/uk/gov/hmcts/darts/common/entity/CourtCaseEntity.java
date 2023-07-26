package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.apache.commons.collections4.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = CourtCaseEntity.TABLE_NAME)
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class CourtCaseEntity {

    public static final String COURT_CASE = "courtCase";
    public static final String VERSION_LABEL = "version_label";
    public static final String RETAIN_UNTIL_TS = "retain_until_ts";
    public static final String CASE_CLOSED_TS = "case_closed_ts";
    public static final String INTERPRETER_USED = "interpreter_used";
    public static final String CASE_CLOSED = "case_closed";
    public static final String CTH_ID = "cth_id";
    public static final String CASE_NUMBER = "case_number";
    public static final String CASE_OBJECT_ID = "case_object_id";
    public static final String EVH_ID = "evh_id";
    public static final String CAS_ID = "cas_id";
    public static final String TABLE_NAME = "court_case";

    @Id
    @Column(name = CAS_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cas_gen")
    @SequenceGenerator(name = "cas_gen", sequenceName = "cas_seq", allocationSize = 1)
    private Integer id;

    /**
     * The reporting restriction maps to the EventHandlerEntity.event_name
     */
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = EVH_ID)
    private EventHandlerEntity reportingRestrictions;

    @Column(name = CASE_OBJECT_ID, length = 16)
    private String legacyCaseObjectId;

    @Column(name = CASE_NUMBER)
    private String caseNumber;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = CTH_ID)
    private CourthouseEntity courthouse;

    @Column(name = CASE_CLOSED)
    private Boolean closed;

    @Column(name = INTERPRETER_USED)
    private Boolean interpreterUsed;

    @Column(name = CASE_CLOSED_TS)
    private OffsetDateTime caseClosedTimestamp;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = COURT_CASE, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DefendantEntity> defendantList = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = COURT_CASE, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ProsecutorEntity> prosecutorList = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = COURT_CASE, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DefenceEntity> defenceList = new ArrayList<>();

    @Column(name = RETAIN_UNTIL_TS)
    private OffsetDateTime retainUntilTimestamp;

    @Column(name = VERSION_LABEL, length = 32)
    private String legacyVersionLabel;

    @OneToMany(mappedBy = COURT_CASE)
    private List<TranscriptionEntity> transcriptions;

    @OneToMany(mappedBy = COURT_CASE)
    private List<AnnotationEntity> annotations;

    @OneToMany(mappedBy = COURT_CASE)
    private List<HearingEntity> hearings = new ArrayList<>();

    public void addHearing(HearingEntity hearing) {
        this.hearings.add(hearing);
    }

    public void addDefence(DefenceEntity defence) {
        this.defenceList.add(defence);
    }

    public void addDefendant(DefendantEntity defendant) {
        this.defendantList.add(defendant);
    }

    public void addProsecutor(ProsecutorEntity prosecutor) {
        this.prosecutorList.add(prosecutor);
    }

    public List<String> getDefendantStringList() {
        return CollectionUtils.emptyIfNull(defendantList).stream().map(DefendantEntity::getName).sorted().toList();
    }

    public List<String> getDefenceStringList() {
        return CollectionUtils.emptyIfNull(defenceList).stream().map(DefenceEntity::getName).sorted().toList();
    }

    public List<String> getProsecutorsStringList() {
        return CollectionUtils.emptyIfNull(prosecutorList).stream().map(ProsecutorEntity::getName).sorted().toList();
    }
}
