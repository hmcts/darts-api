package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moj_event")
@Data
public class Event {

    @Id
    @Column(name = "moj_eve_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "r_event_object_id", unique = true, length = 16)
    private String legacyObjectId;

    @Column(name = "c_event_id")
    private Integer eventId;

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "event_text")
    private String eventText;

    @Column(name = "c_time_stamp")
    private OffsetDateTime timestamp;

    @Column(name = "c_courtroom")
    private String courtroom;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
