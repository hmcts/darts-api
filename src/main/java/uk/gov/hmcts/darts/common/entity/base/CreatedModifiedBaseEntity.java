package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
public class CreatedModifiedBaseEntity {

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccountEntity createdBy;

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by")
    private UserAccountEntity lastModifiedBy;
}
