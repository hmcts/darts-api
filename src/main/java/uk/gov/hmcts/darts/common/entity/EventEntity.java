package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moj_event")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class EventEntity extends VersionedEntity {

    @Id
    @Column(name = "moj_eve_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_eve_gen")
    @SequenceGenerator(name = "moj_eve_gen", sequenceName = "moj_eve_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "r_event_object_id", unique = true, length = 16)
    private String legacyObjectId;

    @ManyToOne
    @JoinColumn(name = "moj_evt_id")
    private EventTypeEntity eventType;

    @Column(name = "c_event_id")
    private Integer legacyEventId;

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "event_text")
    private String eventText;

    @Column(name = "c_time_stamp")
    private OffsetDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "moj_ctr_id")
    private CourtroomEntity courtroom;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "i_superseded")
    private Boolean superseded;

}
