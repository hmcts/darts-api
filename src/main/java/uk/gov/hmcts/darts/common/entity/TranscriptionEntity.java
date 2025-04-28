package uk.gov.hmcts.darts.common.entity;

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
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasLongId;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static java.util.Comparator.comparing;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "transcription")
@Getter
@Setter
@Audited
@AuditTable("transcription_aud")
public class TranscriptionEntity extends CreatedModifiedBaseEntity implements HasLongId {

    @Id
    @Column(name = "tra_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tra_gen")
    @SequenceGenerator(name = "tra_gen", sequenceName = "tra_seq", allocationSize = 1)
    private Long id;

    @NotAudited
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "case_transcription_ae",
        joinColumns = {@JoinColumn(name = "tra_id")},
        inverseJoinColumns = {@JoinColumn(name = "cas_id")})
    private Set<CourtCaseEntity> courtCases = new HashSet<>();

    @Audited(targetAuditMode = NOT_AUDITED)
    @ManyToOne
    @JoinColumn(name = "trt_id", nullable = false)
    private TranscriptionTypeEntity transcriptionType;

    @Audited(targetAuditMode = NOT_AUDITED)
    @ManyToOne
    @JoinColumn(name = "ctr_id")
    private CourtroomEntity courtroom;

    @NotAudited
    @ManyToOne
    @JoinColumn(name = "tru_id")
    private TranscriptionUrgencyEntity transcriptionUrgency;

    @NotAudited
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "hearing_transcription_ae",
        joinColumns = {@JoinColumn(name = "tra_id")},
        inverseJoinColumns = {@JoinColumn(name = "hea_id")})
    private Set<HearingEntity> hearings = new HashSet<>();

    @NotAudited
    @ManyToOne
    @JoinColumn(name = "trs_id")
    private TranscriptionStatusEntity transcriptionStatus;

    @NotAudited
    @Column(name = "transcription_object_id", length = 16)
    private String legacyObjectId;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private UserAccountEntity requestedBy;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "start_ts")
    private OffsetDateTime startTime;

    @Column(name = "end_ts")
    private OffsetDateTime endTime;

    @NotAudited
    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @NotAudited
    @Column(name = "is_manual_transcription", nullable = false)
    private Boolean isManualTranscription;

    @NotAudited
    @Column(name = "is_current")
    private Boolean isCurrent;

    @NotAudited
    @Column(name = "hide_request_from_requestor", nullable = false)
    private Boolean hideRequestFromRequestor;

    @NotAudited
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private UserAccountEntity deletedBy;

    @NotAudited
    @Column(name = "deleted_ts")
    private OffsetDateTime deletedTimestamp;

    @NotAudited
    @Column(name = "chronicle_id")
    private String chronicleId;

    @NotAudited
    @Column(name = "antecedent_id")
    private String antecedentId;

    @NotAudited
    @OneToMany(mappedBy = TranscriptionCommentEntity_.TRANSCRIPTION)
    private List<TranscriptionCommentEntity> transcriptionCommentEntities = new ArrayList<>();

    @NotAudited
    @OneToMany(cascade = {PERSIST, MERGE}, mappedBy = TranscriptionWorkflowEntity_.TRANSCRIPTION)
    private List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = new ArrayList<>();

    @NotAudited
    @OneToMany(mappedBy = TranscriptionDocumentEntity_.TRANSCRIPTION)
    private List<TranscriptionDocumentEntity> transcriptionDocumentEntities = new ArrayList<>();

    @NotAudited
    @Column(name = "transcription_object_name")
    private String transcriptionObjectName;

    @SuppressWarnings("checkstyle:MemberName")
    @NotAudited
    @Column(name = "c_current_state")
    private String cCurrentState;

    @SuppressWarnings("checkstyle:MemberName")
    @NotAudited
    @Column(name = "r_current_state")
    private Integer rCurrentState;

    public void addCase(CourtCaseEntity courtCase) {
        if (courtCase != null) {
            courtCases.add(courtCase);
        }
    }

    public void addHearing(HearingEntity hearing) {
        if (hearing != null) {
            if (hearings.isEmpty()) {
                this.courtroom = hearing.getCourtroom();
            }
            hearings.add(hearing);
        }
    }

    @SuppressWarnings("PMD.NullAssignment")//Required to set courtroom to null if no hearings
    public void setHearings(Set<HearingEntity> hearings) {
        this.hearings = hearings;
        if (CollectionUtils.isEmpty(hearings)) {
            this.courtroom = null;
        } else {
            this.courtroom = DataUtil.orderByCreatedByAndId(hearings).getFirst().getCourtroom();
        }
    }

    /**
     * Get the court case associated with this transcription, using hearing as the preferred route.
     * If no hearings as associated, check for a directly related court case.
     * <p>
     * This is detailed further in <a href="https://tools.hmcts.net/jira/browse/DMP-2157">DMP-2157</a>
     * A potential enhancement for officially supporting this many-to-many is detailed in
     * <a href="https://tools.hmcts.net/jira/browse/DMP-2489">DMP-2489</a>
     * </p>
     *
     * @return CourtCaseEntity if found, otherwise null
     */
    public CourtCaseEntity getCourtCase() {
        HearingEntity hearing = getHearing();
        if (hearing != null) {
            return hearing.getCourtCase();
        }
        if (!CollectionUtils.isEmpty(courtCases)) {
            return DataUtil.orderByCreatedByAndId(courtCases).getFirst();
        }
        return null;
    }

    public CourtroomEntity getPrimaryOrSecondaryCourtroom() {
        HearingEntity hearing = getHearing();
        if (hearing != null) {
            return hearing.getCourtroom();
        }
        return courtroom;
    }

    public Optional<CourthouseEntity> getCourtHouse() {
        if (getHearing() != null) {
            return Optional.of(getHearing().getCourtroom().getCourthouse());
        } else if (getCourtCase() != null) {
            return Optional.of(getCourtCase().getCourthouse());
        }

        return Optional.empty();
    }

    /**
     * This method was added to simplify the switch from List to Set on HearingEntity in which existing code uses .getFirst()
     * This switch was needed to prevent data integirty issues when inserting/deleting values.
     * As when using a list spring will first delete all values on the mapping table. Then reinsert only the new ones.
     * Where as using a Set it will only add the new values and remove the old ones.
     * A tech debt ticket (DMP-4972) has be raised to refactor all the code that uses this method, to ensure it uses a many to many safe equivelent
     *
     * @return the first hearing entity found within the set
     * @deprecated because this is not many to many safe. Implementation should account for multiple hearings
     */
    @Deprecated
    public HearingEntity getHearing() {
        if (CollectionUtils.isEmpty(hearings)) {
            return null;
        }
        return DataUtil.orderByCreatedByAndId(hearings).getFirst();
    }

    /**
     * Get the court cases associated with this transcription, considering both hearings and directly related courts cases.
     * The case_transcription_ae table will be populated by migration team for those transcription where there is no hearing details available in Legacy system.
     * There are transcriptions in legacy where we don't have hearing dates available so those will be put into case_transcription_ae table
     */
    public List<CourtCaseEntity> getAssociatedCourtCases() {
        List<CourtCaseEntity> allCourtCases = new ArrayList<>();

        var casesFromHearings = hearings.stream().map(HearingEntity::getCourtCase).toList();
        allCourtCases.addAll(casesFromHearings);

        allCourtCases.addAll(this.courtCases);

        return io.vavr.collection.List.ofAll(allCourtCases).distinctBy(CourtCaseEntity::getId).toJavaList();
    }

    public Optional<TranscriptionWorkflowEntity> getLatestTranscriptionWorkflow() {
        return transcriptionWorkflowEntities.stream()
            .min(comparing(TranscriptionWorkflowEntity::getWorkflowTimestamp));
    }
}