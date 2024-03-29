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
@Table(name = "object_hidden_reason")
@Data
public class ObjectHiddenReasonEntity {

    @Id
    @Column(name = "ohr_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ohr_gen")
    @SequenceGenerator(name = "ohr_gen", sequenceName = "ohr_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "ohr_reason")
    private String description;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "display_state")
    private boolean displayState = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "marked_for_deletion")
    private boolean markedForDeletion;

}
