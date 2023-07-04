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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = CourtroomEntity.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {CourtroomEntity.CTH_ID, CourtroomEntity.COURTROOM_NAME})})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourtroomEntity {

    public static final String CTR_ID = "ctr_id";
    public static final String COURTROOM_NAME = "courtroom_name";
    public static final String CTH_ID = "cth_id";
    public static final String TABLE_NAME = "courtroom";

    @Id
    @Column(name = CTR_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ctr_gen")
    @SequenceGenerator(name = "ctr_gen", sequenceName = "ctr_seq", allocationSize = 1)
    private Integer id;

    @Column(name = COURTROOM_NAME)
    private String name;

    @ManyToOne
    @JoinColumn(name = CTH_ID)
    private CourthouseEntity courthouse;

}
