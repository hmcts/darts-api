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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;

@Entity
@Table(name = JudgeEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JudgeEntity extends CreatedModifiedBaseEntity implements IsNamedEntity {

    public static final String TABLE_NAME = "judge";
    public static final String ID = "jud_id";
    public static final String JUDGE_NAME = "judge_name";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jud_gen")
    @SequenceGenerator(name = "jud_gen", sequenceName = "jud_seq", allocationSize = 1)
    private Integer id;

    @Column(name = JUDGE_NAME)
    private String name;

}
