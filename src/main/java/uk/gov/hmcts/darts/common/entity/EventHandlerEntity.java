package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedBaseEntity;

@Entity
@Table(name = "event_handler")
@Getter
@Setter
public class EventHandlerEntity extends CreatedBaseEntity {

    @Id
    @Column(name = "evh_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evh_gen")
    @SequenceGenerator(name = "evh_gen", sequenceName = "evh_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "event_type", nullable = false)
    private String type;

    @Column(name = "event_sub_type")
    private String subType;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "handler")
    private String handler;

    @Column(name = "active", nullable = false)
    private Boolean active;

}
