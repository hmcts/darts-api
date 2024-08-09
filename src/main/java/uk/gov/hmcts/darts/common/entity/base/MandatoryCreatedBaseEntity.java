package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;

@MappedSuperclass
@Getter
@Setter
public class MandatoryCreatedBaseEntity {

    @NotNull
    @CreationTimestamp
    @Column(name = "created_ts", nullable = false)
    private OffsetDateTime createdDateTime;

    @NotNull
    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccountEntity createdBy;

}