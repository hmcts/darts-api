package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = CourtCaseEntity.TABLE_NAME)
@SuppressWarnings({"PMD.ShortClassName"})
@Getter
@Setter
public class CourtCaseEntity extends CreatedModifiedBaseEntity {

    public static final String COURT_CASE = "courtCase";
    public static final String CASE_CLOSED_TS = "case_closed_ts";
    public static final String INTERPRETER_USED = "interpreter_used";
    public static final String CASE_CLOSED = "case_closed";
    public static final String CTH_ID = "cth_id";
    public static final String CASE_NUMBER = "case_number";
    public static final String CASE_OBJECT_ID = "case_object_id";
    public static final String EVH_ID = "evh_id";
    public static final String CAS_ID = "cas_id";
    public static final String TABLE_NAME = "court_case";
    public static final String IS_RETENTION_UPDATED = "is_retention_updated";
    public static final String RETENTION_RETRIES = "retention_retries";
    public static final String IS_DELETED = "is_deleted";

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

    @Column(name = CASE_NUMBER, nullable = false)
    private String caseNumber;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = CTH_ID)
    private CourthouseEntity courthouse;

    @Column(name = CASE_CLOSED, nullable = false)
    private Boolean closed;

    @Column(name = INTERPRETER_USED, nullable = false)
    private Boolean interpreterUsed;

    @Column(name = CASE_CLOSED_TS)
    private OffsetDateTime caseClosedTimestamp;

    @Column(name = IS_RETENTION_UPDATED)
    private boolean isRetentionUpdated;

    @Column(name = RETENTION_RETRIES)
    private Integer retentionRetries;

    @OneToMany(orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = COURT_CASE, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DefendantEntity> defendantList = new ArrayList<>();

    @OneToMany(orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = COURT_CASE, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ProsecutorEntity> prosecutorList = new ArrayList<>();

    @OneToMany(orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = COURT_CASE, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DefenceEntity> defenceList = new ArrayList<>();

    @Column(name = IS_DELETED)
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private UserAccountEntity deletedBy;

    @Column(name = "deleted_ts")
    private OffsetDateTime deletedTimestamp;

    @OneToMany(mappedBy = HearingEntity_.COURT_CASE, cascade = CascadeType.PERSIST)
    private List<HearingEntity> hearings = new ArrayList<>();

    @OneToMany(mappedBy = CaseRetentionEntity_.COURT_CASE)
    private List<CaseRetentionEntity> caseRetentionEntities = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "case_judge_ae",
        joinColumns = {@JoinColumn(name = "cas_id")},
        inverseJoinColumns = {@JoinColumn(name = "jud_id")})
    private List<JudgeEntity> judges = new ArrayList<>();

    @OneToMany(mappedBy = MediaLinkedCaseEntity_.COURT_CASE)
    private List<MediaLinkedCaseEntity> mediaLinkedCaseList = new ArrayList<>();

    @Column(name = "is_data_anonymised")
    private boolean isDataAnonymised;

    @Column(name = "data_anonymised_by")
    private Integer dataAnonymisedBy;

    @Column(name = "data_anonymised_ts")
    private OffsetDateTime dataAnonymisedTs;

    @Column(name = "case_type")
    private String caseType;

    @Column(name = "upload_priority")
    private Integer uploadPriority;

    @Column(name = "ret_conf_score")
    private Integer retConfScore;

    @Column(name = "ret_conf_reason")
    private String retConfReason;

    @Column(name = "ret_conf_updated_ts")
    private OffsetDateTime retConfUpdatedTs;

    public void addDefence(DefenceEntity defence) {
        if (defenceList.stream().noneMatch(defenceEntity -> defenceEntity.getName().equalsIgnoreCase(defence.getName()))) {
            defenceList.add(defence);
        }
    }

    public void addDefence(String name) {
        if (defenceList.stream().noneMatch(defenceEntity -> defenceEntity.getName().equalsIgnoreCase(
            name))) {
            DefenceEntity defenceEntity = new DefenceEntity();
            defenceEntity.setName(name);
            defenceEntity.setCourtCase(this);
            defenceList.add(defenceEntity);
        }
    }

    public void addDefendant(DefendantEntity defendant) {
        if (defendantList.stream().noneMatch(defendantEntity -> defendantEntity.getName().equalsIgnoreCase(defendant.getName()))) {
            defendantList.add(defendant);
        }
    }

    public void addDefendant(String name) {
        if (defendantList.stream().noneMatch(defendantEntity -> defendantEntity.getName().equalsIgnoreCase(
            name))) {
            DefendantEntity defendantEntity = new DefendantEntity();
            defendantEntity.setName(name);
            defendantEntity.setCourtCase(this);
            defendantList.add(defendantEntity);
        }
    }

    public void addJudge(JudgeEntity judge) {
        if (judges.stream().noneMatch(judgeEntity -> judgeEntity.getName().equalsIgnoreCase(judge.getName()))) {
            judges.add(judge);
        }
    }

    public void addProsecutor(ProsecutorEntity prosecutor) {
        if (prosecutorList.stream().noneMatch(prosecutorEntity -> prosecutorEntity.getName().equalsIgnoreCase(
            prosecutor.getName()))) {
            prosecutorList.add(prosecutor);
        }
    }

    public void addProsecutor(String name) {
        if (prosecutorList.stream().noneMatch(prosecutorEntity -> prosecutorEntity.getName().equalsIgnoreCase(
            name))) {
            ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
            prosecutorEntity.setName(name);
            prosecutorEntity.setCourtCase(this);
            prosecutorList.add(prosecutorEntity);
        }
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

    public List<String> getJudgeStringList() {
        return CollectionUtils.emptyIfNull(judges).stream().map(JudgeEntity::getName).sorted().distinct().toList();
    }

}

