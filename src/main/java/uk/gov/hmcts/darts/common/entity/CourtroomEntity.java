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
import lombok.Data;

@Entity
@Table(name = "moj_courtroom")
@Data
public class CourtroomEntity {

    @Id
    @Column(name = "moj_ctr_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_ctr_gen")
    @SequenceGenerator(name = "moj_ctr_gen", sequenceName = "moj_ctr_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_cth_id")
    private Courthouse courthouse;

    @Column(name = "courtroom_name")
    private String name;

}
