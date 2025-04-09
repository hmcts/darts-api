package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = "arm_rpo_state")
@Getter
@Setter
@EqualsAndHashCode
public class ArmRpoStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "are_seq")
    @SequenceGenerator(name = "are_seq", sequenceName = "are_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    @Column(name = "are_id", nullable = false)
    private Integer id;

    @Column(name = "are_description", nullable = false)
    private String description;

}