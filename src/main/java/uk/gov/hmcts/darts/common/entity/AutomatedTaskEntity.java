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

@Entity
@Table(name = AutomatedTaskEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AutomatedTaskEntity {

    public static final String TABLE_NAME = "automated_task";
    public static final String AUTOMATED_TASK_ID = "aut_id";
    public static final String TASK_NAME = "task_name";
    public static final String TASK_DESCRIPTION = "task_description";
    public static final String CRON_EXPRESSION = "cron_expression";
    public static final String CRON_EDITABLE = "cron_editable";
    public static final String TASK_GEN = "task_gen";
    public static final String AUTOMATED_TASK_SEQ = "automated_task_seq";


    @Id
    @Column(name = AUTOMATED_TASK_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = TASK_GEN)
    @SequenceGenerator(name = TASK_GEN, sequenceName = AUTOMATED_TASK_SEQ, allocationSize = 1)
    private Integer id;

    @Column(name = TASK_NAME)
    private String taskName;

    @Column(name = TASK_DESCRIPTION)
    private String taskDescription;

    @Column(name = CRON_EXPRESSION)
    private String cronExpression;

    @Column(name = CRON_EDITABLE)
    private Boolean cronEditable;

}
