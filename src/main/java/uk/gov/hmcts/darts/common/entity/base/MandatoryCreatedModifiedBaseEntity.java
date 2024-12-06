package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.LastModifiedBy;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
public class MandatoryCreatedModifiedBaseEntity extends MandatoryCreatedBaseEntity {

    @UpdateTimestamp
    @Column(name = "last_modified_ts", nullable = false)
    private OffsetDateTime lastModifiedDateTime;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "last_modified_by", nullable = false)
    @LastModifiedBy
    private UserAccountEntity lastModifiedBy;
}
