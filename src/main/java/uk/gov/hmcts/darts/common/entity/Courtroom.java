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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@Table(name = "moj_courtroom")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Courtroom {

    @Id
    @Column(name = "moj_ctr_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_ctr_gen")
    @SequenceGenerator(name = "moj_ctr_gen", sequenceName = "moj_ctr_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "courtroom_name")
    private String name;

    @ManyToOne
    @Cascade(CascadeType.SAVE_UPDATE)
    @JoinColumn(name = "moj_cth_id")
    private Courthouse courthouse;


}
