package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Data;

import java.util.Date;

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

    @Column(name = "c_text", length = 2000)
    private String text;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "c_time_stamp")
    private Date timestamp;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_start")
    private Date start;

    @Temporal(TemporalType.DATE)
    @Column(name = "c_end")
    private Date end;

    @Column(name = "c_courthouse", length = 64)
    private String courthouse;

    @Column(name = "c_courtroom", length = 64)
    private String courtroom;

    @Column(name = "c_reporting_restrictions")
    private Integer reportingRestrictions;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
