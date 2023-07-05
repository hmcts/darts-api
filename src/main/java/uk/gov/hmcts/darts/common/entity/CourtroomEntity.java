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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = CourtroomEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {CourtroomEntity.MOJ_CTH_ID, CourtroomEntity.COURTROOM_NAME})})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CourtroomEntity {

    public static final String MOJ_CTR_ID = "moj_ctr_id";
    public static final String COURTROOM_NAME = "courtroom_name";
    public static final String MOJ_CTH_ID = "moj_cth_id";
    public static final String TABLE_NAME = "moj_courtroom";

    @Id
    @Column(name = MOJ_CTR_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_ctr_gen")
    @SequenceGenerator(name = "moj_ctr_gen", sequenceName = "moj_ctr_seq", allocationSize = 1)
    private Integer id;

    @Column(name = COURTROOM_NAME)
    private String name;

    @ManyToOne(cascade = PERSIST)
    @JoinColumn(name = MOJ_CTH_ID)
    private CourthouseEntity courthouse;

}
