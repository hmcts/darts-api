package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "object_hidden_reason")
@Getter
@Immutable
public class HiddenReasonEntity {

    @Id
    @Column(name = "ohr_id")
    private Integer id;

    @Column(name = "ohr_reason")
    private String reason;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "display_state")
    private Boolean displayState;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "marked_for_deletion")
    private Boolean markedForDeletion;

}
