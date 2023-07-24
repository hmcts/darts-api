package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "event")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class EventEntity extends VersionedEntity {

    @Id
    @Column(name = "eve_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eve_gen")
    @SequenceGenerator(name = "eve_gen", sequenceName = "eve_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "event_object_id", unique = true, length = 16)
    private String legacyObjectId;

    @ManyToOne
    @JoinColumn(name = "evh_id")
    private EventHandlerEntity eventType;

    @Column(name = "event_id")
    private Integer legacyEventId;

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "event_text")
    private String eventText;

    @Column(name = "event_ts")
    private OffsetDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "ctr_id")
    private CourtroomEntity courtroom;

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "superseded")
    private Boolean superseded;

    @ManyToMany
    @JoinTable(name = "hearing_event_ae",
        joinColumns = {@JoinColumn(name = "eve_id")},
        inverseJoinColumns = {@JoinColumn(name = "hea_id")})
    private List<HearingEntity> hearingEntities;

}
