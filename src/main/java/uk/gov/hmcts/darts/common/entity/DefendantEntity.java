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
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = DefendantEntity.TABLE_NAME)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DefendantEntity extends CreatedModifiedBaseEntity
    implements IsNamedEntity, HasIntegerId {

    public static final String TABLE_NAME = "defendant";
    public static final String ID = "dfd_id";
    public static final String CAS_ID = "cas_id";
    public static final String DEFENDANT_NAME = "defendant_name";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dfd_gen")
    @SequenceGenerator(name = "dfd_gen", sequenceName = "dfd_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = CAS_ID)
    private CourtCaseEntity courtCase;

    @Column(name = DEFENDANT_NAME)
    private String name;

    @Override
    public void setName(String name) {
        this.name = DataUtil.trim(name);
    }
}
