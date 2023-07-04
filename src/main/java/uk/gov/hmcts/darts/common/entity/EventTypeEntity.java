package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "event_type")
@Data
public class EventTypeEntity {

    @Id
    @Column(name = "evt_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evt_gen")
    @SequenceGenerator(name = "evt_gen", sequenceName = "evt_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "evt_type", nullable = false)
    private String type;

    @Column(name = "sub_type")
    private String subType;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "handler")
    private String handler;

}
