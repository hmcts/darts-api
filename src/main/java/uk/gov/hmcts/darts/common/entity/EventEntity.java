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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Entity
@Table(name = "event")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class EventEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "eve_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eve_gen")
    @SequenceGenerator(name = "eve_gen", sequenceName = "eve_seq", allocationSize = 1)
    private Long id;

    @Column(name = "event_object_id", unique = true, length = 16)
    private String legacyObjectId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "evh_id")
    private EventHandlerEntity eventType;

    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "event_text")
    private String eventText;

    @Column(name = "event_ts", nullable = false)
    private OffsetDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "ctr_id", nullable = false)
    private CourtroomEntity courtroom;

    @OneToMany(mappedBy = EventLinkedCaseEntity_.EVENT)
    private List<EventLinkedCaseEntity> eventLinkedCaseEntities = new ArrayList<>();

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "is_log_entry")
    private boolean isLogEntry;

    @Column(name = "chronicle_id")
    private String chronicleId;

    @Column(name = "antecedent_id")
    private String antecedentId;

    @ManyToMany
    @JoinTable(name = "hearing_event_ae",
        joinColumns = {@JoinColumn(name = "eve_id")},
        inverseJoinColumns = {@JoinColumn(name = "hea_id")})
    private Set<HearingEntity> hearingEntities = new HashSet<>();

    @Column(name = "event_status")
    private Integer eventStatus;

    @Column(name = "is_current")
    private Boolean isCurrent;

    @Column(name = "is_data_anonymised")
    private boolean isDataAnonymised;

    public void addHearing(HearingEntity hearingEntity) {
        hearingEntities.add(hearingEntity);
    }

    public List<CourtCaseEntity> getLinkedCases() {
        if (CollectionUtils.isEmpty(eventLinkedCaseEntities)) {
            return new ArrayList<>();
        }
        return eventLinkedCaseEntities.stream().map(EventLinkedCaseEntity::getCourtCase).toList();
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
    public HearingEntity getHearingEntity() {
        return this.getHearingEntities().stream()
            .sorted(Comparator.comparing(HearingEntity::getCreatedDateTime)
                        .thenComparing(HearingEntity::getId))
            .findFirst()
            .orElseThrow(NoSuchElementException::new);
    }
}
