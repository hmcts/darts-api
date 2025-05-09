package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = DefenceEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DefenceEntity extends CreatedModifiedBaseEntity
    implements IsNamedEntity, HasIntegerId {

    public static final String TABLE_NAME = "defence";
    public static final String ID = "dfc_id";
    public static final String CAS_ID = "cas_id";
    public static final String DEFENCE_NAME = "defence_name";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dfc_gen")
    @SequenceGenerator(name = "dfc_gen", sequenceName = "dfc_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = CAS_ID)
    private CourtCaseEntity courtCase;

    @Column(name = DEFENCE_NAME)
    private String name;

    @Override
    public void setName(String name) {
        this.name = DataUtil.trim(name);
    }
}
