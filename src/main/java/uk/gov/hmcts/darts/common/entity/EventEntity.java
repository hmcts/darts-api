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
import java.util.List;

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
    private Integer id;

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
    private List<HearingEntity> hearingEntities = new ArrayList<>();

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
}
