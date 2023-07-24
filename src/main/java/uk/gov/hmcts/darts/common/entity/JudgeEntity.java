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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = JudgeEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class JudgeEntity {

    public static final String TABLE_NAME = "judges";
    public static final String ID = "jud_id";
    public static final String HEA_ID = "hea_id";
    public static final String JUDGE_NAME = "judge_name";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jud_gen")
    @SequenceGenerator(name = "jud_gen", sequenceName = "jud_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = HEA_ID)
    private HearingEntity hearing;

    @Column(name = JUDGE_NAME)
    private String name;

}
